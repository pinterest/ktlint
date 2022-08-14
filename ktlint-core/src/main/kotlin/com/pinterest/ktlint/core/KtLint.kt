package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.codeStyleSetProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.emptyEditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.internal.EditorConfigGenerator
import com.pinterest.ktlint.core.internal.EditorConfigLoader
import com.pinterest.ktlint.core.internal.PreparedCode
import com.pinterest.ktlint.core.internal.SuppressionLocatorBuilder
import com.pinterest.ktlint.core.internal.VisitorProvider
import com.pinterest.ktlint.core.internal.prepareCodeForLinting
import com.pinterest.ktlint.core.internal.toQualifiedRuleId
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

public object KtLint {
    public val FILE_PATH_USER_DATA_KEY: Key<String> = Key<String>("FILE_PATH")

    @Deprecated("Marked for removal in Ktlint 0.48.0")
    public val EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY: Key<EditorConfigProperties> =
        Key<EditorConfigProperties>("EDITOR_CONFIG_PROPERTIES")
    internal const val UTF8_BOM = "\uFEFF"
    public const val STDIN_FILE: String = "<stdin>"

    internal val editorConfigLoader = EditorConfigLoader(FileSystems.getDefault())

    /**
     * Parameters to invoke [KtLint.lint] and [KtLint.format] API's.
     *
     * [fileName] path of file to lint/format
     * [text] Contents of file to lint/format
     * [ruleSets] a collection of "RuleSet"s used to validate source. This field is deprecated and will be removed in
     * KtLint 0.48.
     * [ruleProviders] a collection of [RuleProvider]s used to create new instances of [Rule]s so that it can keep
     * internal state and be called thread-safe
     * [userData] Map of user options. This field is deprecated and will be removed in a future version.
     * [cb] callback invoked for each lint error
     * [script] true if this is a Kotlin script file
     * [editorConfigPath] optional path of the .editorconfig file (otherwise will use working directory)
     * [debug] True if invoked with the --debug flag
     * [editorConfigOverride] should contain entries to add/replace from loaded `.editorconfig` files.
     *
     * For possible keys check related [Rule]s that implements [UsesEditorConfigProperties] interface.
     *
     * For values use `PropertyType.PropertyValue.valid("override", <expected type>)` approach.
     * It is also possible to set value into "unset" state by using [PropertyType.PropertyValue.UNSET].
     *
     * [isInvokedFromCli] **For internal use only**: indicates that linting was invoked from KtLint CLI tool.
     * Enables some internals workarounds for Kotlin Compiler initialization.
     * Usually you don't need to use it and most probably it will be removed in one of next versions.
     */
    public data class ExperimentalParams(
        val fileName: String? = null,
        val text: String,
        @Deprecated(
            message = "Marked for removal in KtLint 0.48",
            replaceWith = ReplaceWith("ruleProviders"),
        )
        val ruleSets: Iterable<RuleSet> = Iterable { emptySet<RuleSet>().iterator() },
        val ruleProviders: Set<RuleProvider> = emptySet(),
        val userData: Map<String, String> = emptyMap(), // TODO: remove in a future version
        val cb: (e: LintError, corrected: Boolean) -> Unit,
        val script: Boolean = false,
        val editorConfigPath: String? = null,
        val debug: Boolean = false,
        val editorConfigOverride: EditorConfigOverride = emptyEditorConfigOverride,
        val isInvokedFromCli: Boolean = false,
    ) {
        internal val ruleRunners: Set<RuleRunner> =
            ruleProviders
                .map { RuleRunner(it) }
                .plus(
                    /** Support backward compatibility for API consumers in KtLint 0.47 by changing rule sets to rule
                     * providers with limitation that for those rules *no* new instances can be created during
                     * [KtLint.format].
                     * KtLint CLI already transforms rules provided by external rule providers to real RuleProviders
                     * for which new instances can be created. The same workaround is not possible for as
                     * [KtLint.ExperimentalParams.ruleSets] already contain the created [Rule] instance.
                     */
                    // TODO: remove when removing the deprecated ruleSets.
                    ruleSets
                        .flatMap { it.rules.toList() }
                        .map { RuleRunner(createStaticRuleProvider(it)) },
                ).distinctBy { it.ruleId }
                .toSet()

        internal fun getRules(): Set<Rule> =
            ruleRunners
                .map { it.getRule() }
                .toSet()

        init {
            require(ruleSets.any() xor ruleProviders.any()) {
                "Provide exactly one of parameters 'ruleSets' or 'ruleProviders'"
            }
            // Extract all default and custom ".editorconfig" properties which are registered
            val editorConfigProperties =
                getRules()
                    .asSequence()
                    .filterIsInstance<UsesEditorConfigProperties>()
                    .map { it.editorConfigProperties }
                    .flatten()
                    .plus(DefaultEditorConfigProperties.editorConfigProperties)
                    .map { it.type.name }
                    .distinct()
                    .toSet()

            userData
                .keys
                .intersect(editorConfigProperties)
                .let {
                    check(it.isEmpty()) {
                        "UserData should not contain '.editorconfig' properties ${it.sorted()}. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that this is " +
                            "only required for properties that (potentially) contain a value that differs from the " +
                            "actual value in the '.editorconfig' file."
                    }
                }

            userData
                .keys
                .minus(editorConfigProperties)
                .let {
                    check(it.isEmpty()) {
                        "UserData contains properties ${it.sorted()}. However, userData is deprecated and will be " +
                            "removed in a future version. Please create an issue that shows how this field is " +
                            "actively used."
                    }
                }
        }

        internal val normalizedFilePath: Path?
            get() = if (fileName == STDIN_FILE || fileName == null) {
                null
            } else {
                Paths.get(fileName)
            }

        internal val isStdIn: Boolean get() = fileName == STDIN_FILE
    }

    /**
     * Check source for lint errors.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    public fun lint(params: ExperimentalParams) {
        val preparedCode = prepareCodeForLinting(params)
        val errors = mutableListOf<LintError>()

        VisitorProvider(params)
            .visitor(preparedCode.editorConfigProperties)
            .invoke { rule, fqRuleId ->
                preparedCode.executeRule(rule, fqRuleId, false) { offset, errorMessage, canBeAutoCorrected ->
                    val (line, col) = preparedCode.positionInTextLocator(offset)
                    errors.add(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected))
                }
            }

        errors
            .sortedWith { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { e -> params.cb(e, false) }
    }

    private fun PreparedCode.executeRule(
        rule: Rule,
        fqRuleId: String,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        rule.startTraversalOfAST()
        rule.beforeFirstNode(editorConfigProperties)
        this.executeRuleOnNodeRecursively(rootNode, rule, fqRuleId, autoCorrect, emit)
        rule.afterLastNode()
    }

    private fun PreparedCode.executeRuleOnNodeRecursively(
        node: ASTNode,
        rule: Rule,
        fqRuleId: String,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (rule.shouldContinueTraversalOfAST()) {
            try {
                rule.beforeVisitChildNodes(node, autoCorrect, emit)
                if (!rule.runsOnRootNodeOnly() && rule.shouldContinueTraversalOfAST()) {
                    node
                        .getChildren(null)
                        .forEach { childNode ->
                            // https://github.com/shyiko/ktlint/issues/158#issuecomment-462728189
                            // fixme: enforcing suppression based on node.startOffset is wrong (not just because not all nodes are leaves
                            //  but because rules are free to emit (and fix!) errors at any position)
                            if (!suppressedRegionLocator(childNode.startOffset, fqRuleId, childNode === rootNode)) {
                                this.executeRuleOnNodeRecursively(childNode, rule, fqRuleId, autoCorrect, emit)
                            }
                        }
                }
                rule.afterVisitChildNodes(node, autoCorrect, emit)
            } catch (e: Exception) {
                if (autoCorrect) {
                    // line/col cannot be reliably mapped as exception might originate from a node not present in the
                    // original AST
                    throw RuleExecutionException(0, 0, fqRuleId, e)
                } else {
                    val (line, col) = positionInTextLocator(node.startOffset)
                    throw RuleExecutionException(line, col, fqRuleId, e)
                }
            }
        }
    }

    /**
     * Fix style violations.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(params: ExperimentalParams): String {
        val hasUTF8BOM = params.text.startsWith(UTF8_BOM)
        val preparedCode = prepareCodeForLinting(params)

        var tripped = false
        var mutated = false
        val errors = mutableSetOf<Pair<LintError, Boolean>>()
        val visitorProvider = VisitorProvider(params = params)
        visitorProvider
            .visitor(preparedCode.editorConfigProperties)
            .invoke { rule, fqRuleId ->
                preparedCode.executeRule(rule, fqRuleId, true) { offset, errorMessage, canBeAutoCorrected ->
                    tripped = true
                    if (canBeAutoCorrected) {
                        mutated = true
                        if (preparedCode.suppressedRegionLocator !== SuppressionLocatorBuilder.noSuppression) {
                            // Offsets of start and end positions of suppressed regions might have changed due to
                            // updating the code
                            preparedCode.suppressedRegionLocator =
                                SuppressionLocatorBuilder.buildSuppressedRegionsLocator(
                                    preparedCode.rootNode,
                                )
                        }
                    }
                    val (line, col) = preparedCode.positionInTextLocator(offset)
                    errors.add(
                        Pair(
                            LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected),
                            // It is assumed that a rule that emits that an error can be autocorrected, also
                            // does correct the error.
                            canBeAutoCorrected,
                        ),
                    )
                }
            }
        if (tripped) {
            visitorProvider
                .visitor(preparedCode.editorConfigProperties)
                .invoke { rule, fqRuleId ->
                    preparedCode.executeRule(rule, fqRuleId, false) { offset, errorMessage, canBeAutoCorrected ->
                        val (line, col) = preparedCode.positionInTextLocator(offset)
                        errors.add(
                            Pair(
                                LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected),
                                // It is assumed that a rule only corrects an error after it has emitted an
                                // error and indicating that it actually can be autocorrected.
                                false,
                            ),
                        )
                    }
                }
        }

        errors
            .sortedWith { (l), (r) -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { (e, corrected) -> params.cb(e, corrected) }

        if (!mutated) {
            return params.text
        }

        val code = preparedCode
            .rootNode
            .text
            .replace("\n", determineLineSeparator(params.text, params.userData))
        return if (hasUTF8BOM) {
            UTF8_BOM + code
        } else {
            code
        }
    }

    private fun Rule.runsOnRootNodeOnly() =
        visitorModifiers.contains(Rule.VisitorModifier.RunOnRootNodeOnly)

    /**
     * Reduce memory usage of all internal caches.
     */
    public fun trimMemory() {
        editorConfigLoader.trimMemory()
    }

    /**
     * Generates Kotlin `.editorconfig` file section content based on [ExperimentalParams].
     *
     * Method loads merged `.editorconfig` content from [ExperimentalParams] path,
     * and then, by querying rules from [ExperimentalParams] for missing properties default values,
     * generates Kotlin section (default is `[*.{kt,kts}]`) new content.
     *
     * Rule should implement [UsesEditorConfigProperties] interface to support this.
     *
     * @return Kotlin section editorconfig content. For example:
     * ```properties
     * final-newline=true
     * indent-size=4
     * ```
     */
    public fun generateKotlinEditorConfigSection(
        params: ExperimentalParams,
    ): String {
        val filePath = params.normalizedFilePath
        requireNotNull(filePath) {
            "Please pass path to existing Kotlin file"
        }
        val codeStyle =
            params
                .editorConfigOverride
                .properties[codeStyleSetProperty]
                ?.parsed
                ?.safeAs<DefaultEditorConfigProperties.CodeStyleValue>()
                ?: codeStyleSetProperty.defaultValue
        return EditorConfigGenerator(editorConfigLoader).generateEditorconfig(
            filePath,
            params.getRules(),
            params.debug,
            codeStyle,
        )
    }

    private fun determineLineSeparator(fileContent: String, userData: Map<String, String>): String {
        val eol = userData["end_of_line"]?.trim()?.lowercase(Locale.getDefault())
        return when {
            eol == "native" -> System.lineSeparator()
            eol == "crlf" || eol != "lf" && fileContent.lastIndexOf('\r') != -1 -> "\r\n"
            else -> "\n"
        }
    }
}

internal class RuleRunner(private val provider: RuleProvider) {
    private var rule = provider.createNewRuleInstance()

    internal val qualifiedRuleId = rule.toQualifiedRuleId()
    internal val shortenedQualifiedRuleId = qualifiedRuleId.removePrefix("standard:")

    internal val ruleId = rule.id
    internal val ruleSetId = qualifiedRuleId.substringBefore(':')

    val runOnRootNodeOnly =
        rule.visitorModifiers.contains(Rule.VisitorModifier.RunOnRootNodeOnly)
    val runAsLateAsPossible = rule.visitorModifiers.contains(Rule.VisitorModifier.RunAsLateAsPossible)
    var runAfterRule = setRunAfterRule()

    /**
     * Gets the [Rule]. If the [Rule] has already been used for traversal of the AST, a new instance of the [Rule] is
     * provided. This prevents leakage of the state of the Rule between executions.
     */
    internal fun getRule(): Rule {
        if (rule.isUsedForTraversalOfAST()) {
            rule = provider.createNewRuleInstance()
        }
        return rule
    }

    private fun setRunAfterRule(): Rule.VisitorModifier.RunAfterRule? =
        rule
            .visitorModifiers
            .find { it is Rule.VisitorModifier.RunAfterRule }
            ?.let {
                val runAfterRuleVisitorModifier = it as Rule.VisitorModifier.RunAfterRule
                val qualifiedAfterRuleId = runAfterRuleVisitorModifier.ruleId.toQualifiedRuleId()
                check(qualifiedRuleId != qualifiedAfterRuleId) {
                    // Do not print the fully qualified rule id in the error message as it might not appear in the code
                    // in case it is a rule from the 'standard' rule set.
                    "Rule with id '${rule.id}' has a visitor modifier of type " +
                        "'${Rule.VisitorModifier.RunAfterRule::class.simpleName}' but it is not referring to another " +
                        "rule but to the rule itself. A rule can not run after itself. This should be fixed by the " +
                        "maintainer of the rule."
                }
                runAfterRuleVisitorModifier.copy(
                    ruleId = qualifiedAfterRuleId,
                )
            }

    internal fun clearRunAfterRule() {
        require(!rule.isUsedForTraversalOfAST()) {
            "RunAfterRule can not be cleared when rule has already been used for traversal of the AST"
        }
        runAfterRule = null
    }
}

/**
 * This provider is added for backward compatibility of KtLint 0.47 so that API consumers can either use
 * [KtLint.ExperimentalParams.ruleSets] or [KtLint.ExperimentalParams.ruleProviders] per [RuleSetProvider]. This method
 * created a [RuleProvider] which returns a *static* instance of a rule and should only be used for rules provided via
 * [KtLint.ExperimentalParams.ruleSets].
 * * Rules provided by this [RuleProvider] might leak state between the first and second invocation of the rule when
 * running [KtLint.format]. It is assumed that [Rule] implementations offered by 'KtLint 0.46.x' and custom rule set
 * providers are not suffering any problems at this moment as this architectural bug exists in KtLint for quite a long
 * * Note that [KtLint.ExperimentalParams.ruleSets] and this helper method will be removed in KtLint 0.48.
 */
@Deprecated(message = "Remove when 'ruleSets' are removed")
private fun createStaticRuleProvider(rule: Rule) =
    RuleProvider { rule }
