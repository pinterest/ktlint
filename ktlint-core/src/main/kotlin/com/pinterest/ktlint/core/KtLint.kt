package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.core.api.KtLintRuleExecutionException
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.internal.EditorConfigFinder
import com.pinterest.ktlint.core.internal.EditorConfigGenerator
import com.pinterest.ktlint.core.internal.EditorConfigLoader
import com.pinterest.ktlint.core.internal.RuleExecutionContext
import com.pinterest.ktlint.core.internal.SuppressHandler
import com.pinterest.ktlint.core.internal.ThreadSafeEditorConfigCache.Companion.THREAD_SAFER_EDITOR_CONFIG_CACHE
import com.pinterest.ktlint.core.internal.VisitorProvider
import com.pinterest.ktlint.core.internal.createRuleExecutionContext
import com.pinterest.ktlint.core.internal.toQualifiedRuleId
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import org.ec4j.core.Resource
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@Suppress("MemberVisibilityCanBePrivate")
public object KtLint {
    @Deprecated(
        """
            Marked for removal in KtLint 0.49.0. Use:
                if (node.isRoot()) {
                    val fileName = (node.psi as? KtFile)?.name
                    ...
                }
            """,
    )
    public val FILE_PATH_USER_DATA_KEY: Key<String> = Key<String>("FILE_PATH")

    internal const val UTF8_BOM = "\uFEFF"
    public const val STDIN_FILE: String = "<stdin>"

    internal val EDITOR_CONFIG_LOADER = EditorConfigLoader(FileSystems.getDefault())

    /**
     * Parameters to invoke [KtLint.lint] and [KtLint.format] API's.
     *
     * [fileName] path of file to lint/format
     * [text] Contents of file to lint/format
     * [ruleProviders] a collection of [RuleProvider]s used to create new instances of [Rule]s so that it can keep
     * internal state and be called thread-safe
     * [userData] Map of user options. This field is deprecated and will be removed in a future version.
     * [cb] callback invoked for each lint error
     * [script] true if this is a Kotlin script file
     * [debug] True if invoked with the --debug flag
     * [editorConfigDefaults] contains default values for `.editorconfig` properties which are not set explicitly in
     * any '.editorconfig' file located on the path of the [fileName]. If a property is set in [editorConfigDefaults]
     * this takes precedence above the default values defined in the KtLint project.
     * [editorConfigOverride] should contain entries to add/replace from loaded `.editorconfig` files. If a property is
     * set in [editorConfigOverride] it takes precedence above the same property being set in any other way.
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
        val ruleProviders: Set<RuleProvider> = emptySet(),
        val userData: Map<String, String> = emptyMap(), // TODO: remove in a future version
        val cb: (e: LintError, corrected: Boolean) -> Unit,
        val script: Boolean = false,
        val debug: Boolean = false,
        val editorConfigDefaults: EditorConfigDefaults = EMPTY_EDITOR_CONFIG_DEFAULTS,
        val editorConfigOverride: EditorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
        val isInvokedFromCli: Boolean = false,
    ) {
        internal val ruleRunners: Set<RuleRunner> =
            ruleProviders
                .map { RuleRunner(it) }
                .distinctBy { it.ruleId }
                .toSet()

        internal fun getRules(): Set<Rule> =
            ruleRunners
                .map { it.getRule() }
                .toSet()

        init {
            require(ruleProviders.any()) {
                "A non-empty set of 'ruleProviders' need to be provided"
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
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    public fun lint(params: ExperimentalParams) {
        val ruleExecutionContext = createRuleExecutionContext(params)
        val errors = mutableListOf<LintError>()

        VisitorProvider(params)
            .visitor(ruleExecutionContext.editorConfigProperties)
            .invoke { rule, fqRuleId ->
                ruleExecutionContext.executeRule(rule, fqRuleId, false) { offset, errorMessage, canBeAutoCorrected ->
                    val (line, col) = ruleExecutionContext.positionInTextLocator(offset)
                    errors.add(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected))
                }
            }

        errors
            .sortedWith { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { e -> params.cb(e, false) }
    }

    private fun RuleExecutionContext.executeRule(
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

    private fun RuleExecutionContext.executeRuleOnNodeRecursively(
        node: ASTNode,
        rule: Rule,
        fqRuleId: String,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        /**
         * The [RuleExecutionContext.suppressionLocator] can be changed during each visit of node when running
         * [KtLint.format]. So a new handler is to be built before visiting the nodes.
         */
        val suppressHandler = SuppressHandler(suppressionLocator, rootNode, autoCorrect, emit)
        if (rule.shouldContinueTraversalOfAST()) {
            try {
                suppressHandler.handle(node, fqRuleId) { autoCorrect, emit ->
                    rule.beforeVisitChildNodes(node, autoCorrect, emit)
                }
                if (rule.shouldContinueTraversalOfAST()) {
                    node
                        .getChildren(null)
                        .forEach { childNode ->
                            suppressHandler.handle(childNode, fqRuleId) { autoCorrect, emit ->
                                this.executeRuleOnNodeRecursively(
                                    childNode,
                                    rule,
                                    fqRuleId,
                                    autoCorrect,
                                    emit,
                                )
                            }
                        }
                }
                suppressHandler.handle(node, fqRuleId) { autoCorrect, emit ->
                    rule.afterVisitChildNodes(node, autoCorrect, emit)
                }
            } catch (e: Exception) {
                if (autoCorrect) {
                    // line/col cannot be reliably mapped as exception might originate from a node not present in the
                    // original AST
                    throw KtLintRuleExecutionException(0, 0, fqRuleId, e)
                } else {
                    val (line, col) = positionInTextLocator(node.startOffset)
                    throw KtLintRuleExecutionException(line, col, fqRuleId, e)
                }
            }
        }
    }

    /**
     * Fix style violations.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(params: ExperimentalParams): String {
        val hasUTF8BOM = params.text.startsWith(UTF8_BOM)
        val ruleExecutionContext = createRuleExecutionContext(params)

        var tripped = false
        var mutated = false
        val errors = mutableSetOf<Pair<LintError, Boolean>>()
        val visitorProvider = VisitorProvider(params = params)
        visitorProvider
            .visitor(ruleExecutionContext.editorConfigProperties)
            .invoke { rule, fqRuleId ->
                ruleExecutionContext.executeRule(rule, fqRuleId, true) { offset, errorMessage, canBeAutoCorrected ->
                    tripped = true
                    if (canBeAutoCorrected) {
                        mutated = true
                        /**
                         * Rebuild the suppression locator after each change in the AST as the offsets of the
                         * suppression hints might have changed.
                         */
                        ruleExecutionContext.rebuildSuppressionLocator()
                    }
                    val (line, col) = ruleExecutionContext.positionInTextLocator(offset)
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
                .visitor(ruleExecutionContext.editorConfigProperties)
                .invoke { rule, fqRuleId ->
                    ruleExecutionContext.executeRule(rule, fqRuleId, false) { offset, errorMessage, canBeAutoCorrected ->
                        val (line, col) = ruleExecutionContext.positionInTextLocator(offset)
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

        val code = ruleExecutionContext
            .rootNode
            .text
            .replace("\n", determineLineSeparator(params.text, params.userData))
        return if (hasUTF8BOM) {
            UTF8_BOM + code
        } else {
            code
        }
    }

    /**
     * Reduce memory usage by cleaning internal caches.
     */
    public fun trimMemory() {
        THREAD_SAFER_EDITOR_CONFIG_CACHE.clear()
    }

    /**
     * Get the list of files which will be accessed by KtLint when linting or formatting the given file or directory.
     * The API consumer can use this list to observe changes in '.editorconfig` files. Whenever such a change is
     * observed, the API consumer should call [reloadEditorConfigFile].
     * To avoid unnecessary access to the file system, it is best to call this method only once for the root of the
     * project which is to be [lint] or [format].
     */
    public fun editorConfigFilePaths(path: Path): List<Path> =
        EditorConfigFinder().findEditorConfigs(path)

    /**
     * Reloads an '.editorconfig' file given that it is currently loaded into the KtLint cache. This method is intended
     * to be called by the API consumer when it is aware of changes in the '.editorconfig' file that should be taken
     * into account with next calls to [lint] and/or [format]. See [editorConfigFilePaths] to get the list of
     * '.editorconfig' files which need to be observed.
     */
    public fun reloadEditorConfigFile(path: Path) {
        THREAD_SAFER_EDITOR_CONFIG_CACHE.reloadIfExists(
            Resource.Resources.ofPath(path, StandardCharsets.UTF_8),
        )
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
                .properties[CODE_STYLE_PROPERTY]
                ?.parsed
                ?.safeAs<DefaultEditorConfigProperties.CodeStyleValue>()
                ?: CODE_STYLE_PROPERTY.defaultValue
        return EditorConfigGenerator(EDITOR_CONFIG_LOADER).generateEditorconfig(
            filePath,
            params.getRules(),
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
