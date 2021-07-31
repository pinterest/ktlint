package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.visit
import com.pinterest.ktlint.core.internal.EditorConfigGenerator
import com.pinterest.ktlint.core.internal.EditorConfigLoader
import com.pinterest.ktlint.core.internal.EditorConfigLoader.Companion.convertToRawValues
import com.pinterest.ktlint.core.internal.EditorConfigOverridesMap
import com.pinterest.ktlint.core.internal.KotlinPsiFileFactory
import com.pinterest.ktlint.core.internal.LineAndColumn
import com.pinterest.ktlint.core.internal.SuppressionLocator
import com.pinterest.ktlint.core.internal.buildPositionInTextLocator
import com.pinterest.ktlint.core.internal.buildSuppressedRegionsLocator
import com.pinterest.ktlint.core.internal.noSuppression
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile

public object KtLint {

    public val EDITOR_CONFIG_USER_DATA_KEY: Key<EditorConfig> = Key<EditorConfig>("EDITOR_CONFIG")
    public val ANDROID_USER_DATA_KEY: Key<Boolean> = Key<Boolean>("ANDROID")
    public val FILE_PATH_USER_DATA_KEY: Key<String> = Key<String>("FILE_PATH")
    private const val FILE_PATH_PROPERTY = "file_path"
    public val EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY: Key<EditorConfigProperties> =
        Key<EditorConfigProperties>("EDITOR_CONFIG_PROPERTIES")
    public val DISABLED_RULES: Key<Set<String>> = Key<Set<String>>("DISABLED_RULES")
    private const val UTF8_BOM = "\uFEFF"
    public const val STDIN_FILE: String = "<stdin>"

    private val kotlinPsiFileFactory = KotlinPsiFileFactory()
    private val editorConfigLoader = EditorConfigLoader(FileSystems.getDefault())

    @OptIn(FeatureInAlphaState::class)
    private val editorConfigGenerator = EditorConfigGenerator(editorConfigLoader)

    /**
     * @param fileName path of file to lint/format
     * @param text Contents of file to lint/format
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param userData Map of user options
     * @param cb callback invoked for each lint error
     * @param script true if this is a Kotlin script file
     * @param editorConfigPath optional path of the .editorconfig file (otherwise will use working directory)
     * @param debug True if invoked with the --debug flag
     */
    public data class Params(
        val fileName: String? = null,
        val text: String,
        val ruleSets: Iterable<RuleSet>,
        val userData: Map<String, String> = emptyMap(),
        val cb: (e: LintError, corrected: Boolean) -> Unit,
        val script: Boolean = false,
        val editorConfigPath: String? = null,
        val debug: Boolean = false,
    )

    /**
     * @param fileName path of file to lint/format
     * @param text Contents of file to lint/format
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param userData Map of user options
     * @param cb callback invoked for each lint error
     * @param script true if this is a Kotlin script file
     * @param editorConfigPath optional path of the .editorconfig file (otherwise will use working directory)
     * @param debug True if invoked with the --debug flag
     * @param editorConfigOverride should contain entries to add/replace from loaded `.editorconfig` files.
     *
     * For possible keys check related [Rule]s that implements [UsesEditorConfigProperties] interface.
     *
     * For values use `PropertyType.PropertyValue.valid("override", <expected type>)` approach.
     * It is also possible to set value into "unset" state by using [PropertyType.PropertyValue.UNSET].
     *
     * @param isInvokedFromCli **For internal use only**: indicates that linting was invoked from KtLint CLI tool.
     * Enables some internals workarounds for Kotlin Compiler initialization.
     * Usually you don't need to use it and most probably it will be removed in one of next versions.
     */
    @FeatureInAlphaState
    public data class ExperimentalParams(
        val fileName: String? = null,
        val text: String,
        val ruleSets: Iterable<RuleSet>,
        val userData: Map<String, String> = emptyMap(),
        val cb: (e: LintError, corrected: Boolean) -> Unit,
        val script: Boolean = false,
        val editorConfigPath: String? = null,
        val debug: Boolean = false,
        val editorConfigOverride: EditorConfigOverridesMap = emptyMap(),
        val isInvokedFromCli: Boolean = false,
    ) {
        internal val normalizedFilePath: Path? get() = if (fileName == STDIN_FILE || fileName == null) {
            null
        } else {
            Paths.get(fileName)
        }

        internal val isStdIn: Boolean get() = fileName == STDIN_FILE

        internal val rules: Set<Rule> get() = ruleSets
            .flatMap {
                it.rules.toList()
            }
            .toSet()
    }

    @OptIn(FeatureInAlphaState::class)
    private fun toExperimentalParams(params: Params): ExperimentalParams =
        ExperimentalParams(
            fileName = params.fileName,
            text = params.text,
            ruleSets = params.ruleSets,
            userData = params.userData,
            cb = params.cb,
            script = params.script,
            editorConfigPath = params.editorConfigPath,
            debug = params.debug
        )

    /**
     * Check source for lint errors.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    @OptIn(FeatureInAlphaState::class)
    public fun lint(params: Params) {
        lint(toExperimentalParams(params))
    }

    /**
     * Check source for lint errors.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    @FeatureInAlphaState
    public fun lint(params: ExperimentalParams) {
        val psiFileFactory = kotlinPsiFileFactory.acquirePsiFileFactory(params.isInvokedFromCli)
        val preparedCode = prepareCodeForLinting(psiFileFactory, params)
        val errors = mutableListOf<LintError>()

        visitor(preparedCode.rootNode, params.ruleSets).invoke { node, rule, fqRuleId ->
            // fixme: enforcing suppression based on node.startOffset is wrong
            // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
            if (
                !preparedCode.suppressedRegionLocator(node.startOffset, fqRuleId, node === preparedCode.rootNode)
            ) {
                try {
                    rule.visit(node, false) { offset, errorMessage, canBeAutoCorrected ->
                        // https://github.com/shyiko/ktlint/issues/158#issuecomment-462728189
                        if (node.startOffset != offset &&
                            preparedCode.suppressedRegionLocator(offset, fqRuleId, node === preparedCode.rootNode)
                        ) {
                            return@visit
                        }
                        val (line, col) = preparedCode.positionInTextLocator(offset)
                        errors.add(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected))
                    }
                } catch (e: Exception) {
                    val (line, col) = preparedCode.positionInTextLocator(node.startOffset)
                    kotlinPsiFileFactory.releasePsiFileFactory()
                    throw RuleExecutionException(line, col, fqRuleId, e)
                }
            }
        }

        kotlinPsiFileFactory.releasePsiFileFactory()
        errors
            .sortedWith { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { e -> params.cb(e, false) }
    }

    @OptIn(FeatureInAlphaState::class)
    private fun prepareCodeForLinting(
        psiFileFactory: PsiFileFactory,
        params: ExperimentalParams
    ): PreparedCode {
        val normalizedText = normalizeText(params.text)
        val positionInTextLocator = buildPositionInTextLocator(normalizedText)

        val psiFileName = if (params.script) "file.kts" else "file.kt"
        val psiFile = psiFileFactory.createFileFromText(
            psiFileName,
            KotlinLanguage.INSTANCE,
            normalizedText
        ) as KtFile

        val errorElement = psiFile.findErrorElement()
        if (errorElement != null) {
            val (line, col) = positionInTextLocator(errorElement.textOffset)
            throw ParseException(line, col, errorElement.errorDescription)
        }

        val rootNode = psiFile.node

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(
            params.normalizedFilePath,
            params.isStdIn,
            params.editorConfigPath?.let { Paths.get(it) },
            params.rules,
            params.editorConfigOverride,
            params.debug
        )

        // Passed-in userData overrides .editorconfig
        val mergedUserData = editorConfigProperties
            .convertToRawValues() + params.userData
            .run {
                if (!params.isStdIn) {
                    plus(FILE_PATH_PROPERTY to params.normalizedFilePath.toString())
                } else {
                    this
                }
            }

        injectUserData(rootNode, editorConfigProperties, mergedUserData)

        val suppressedRegionLocator = buildSuppressedRegionsLocator(rootNode)

        return PreparedCode(
            rootNode,
            positionInTextLocator,
            suppressedRegionLocator
        )
    }

    @Deprecated(
        message = "Should not be a part of public api. Will be removed in future release.",
        level = DeprecationLevel.WARNING
    )
    public fun normalizeText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceFirst(UTF8_BOM, "")
    }

    private val Map<String, String>.isAndroidCodeStyle get() = get("android")?.toBoolean() ?: false

    private fun injectUserData(
        node: ASTNode,
        editorConfigProperties: EditorConfigProperties,
        userData: Map<String, String>
    ) {
        val android = userData.isAndroidCodeStyle
        val editorConfigMap =
            if (android &&
                userData["max_line_length"].let { it?.toLowerCase() != "off" && it?.toIntOrNull() == null }
            ) {
                userData + mapOf("max_line_length" to "100")
            } else {
                userData
            }
        node.putUserData(FILE_PATH_USER_DATA_KEY, userData[FILE_PATH_PROPERTY])
        node.putUserData(EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(editorConfigMap - "android" - "file_path"))
        node.putUserData(EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY, editorConfigProperties)
        node.putUserData(ANDROID_USER_DATA_KEY, android)
        node.putUserData(
            DISABLED_RULES,
            userData["disabled_rules"]?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
        )
    }

    private fun visitor(
        rootNode: ASTNode,
        ruleSets: Iterable<RuleSet>,
        concurrent: Boolean = true,
        filter: (rootNode: ASTNode, fqRuleId: String) -> Boolean = this::filterDisabledRules

    ): ((node: ASTNode, rule: Rule, fqRuleId: String) -> Unit) -> Unit {
        val fqrsRestrictedToRoot = mutableListOf<Pair<String, Rule>>()
        val fqrs = mutableListOf<Pair<String, Rule>>()
        val fqrsExpectedToBeExecutedLastOnRoot = mutableListOf<Pair<String, Rule>>()
        val fqrsExpectedToBeExecutedLast = mutableListOf<Pair<String, Rule>>()
        for (ruleSet in ruleSets) {
            val prefix = if (ruleSet.id === "standard") "" else "${ruleSet.id}:"
            for (rule in ruleSet) {
                val fqRuleId = "$prefix${rule.id}"
                if (!filter(rootNode, fqRuleId)) {
                    continue
                }
                val fqr = fqRuleId to rule
                when {
                    rule is Rule.Modifier.Last -> fqrsExpectedToBeExecutedLast.add(fqr)
                    rule is Rule.Modifier.RestrictToRootLast -> fqrsExpectedToBeExecutedLastOnRoot.add(fqr)
                    rule is Rule.Modifier.RestrictToRoot -> fqrsRestrictedToRoot.add(fqr)
                    else -> fqrs.add(fqr)
                }
            }
        }
        return { visit ->
            for ((fqRuleId, rule) in fqrsRestrictedToRoot) {
                visit(rootNode, rule, fqRuleId)
            }
            if (concurrent) {
                rootNode.visit { node ->
                    for ((fqRuleId, rule) in fqrs) {
                        visit(node, rule, fqRuleId)
                    }
                }
            } else {
                for ((fqRuleId, rule) in fqrs) {
                    rootNode.visit { node ->
                        visit(node, rule, fqRuleId)
                    }
                }
            }
            for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLastOnRoot) {
                visit(rootNode, rule, fqRuleId)
            }
            if (!fqrsExpectedToBeExecutedLast.isEmpty()) {
                if (concurrent) {
                    rootNode.visit { node ->
                        for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLast) {
                            visit(node, rule, fqRuleId)
                        }
                    }
                } else {
                    for ((fqRuleId, rule) in fqrsExpectedToBeExecutedLast) {
                        rootNode.visit { node ->
                            visit(node, rule, fqRuleId)
                        }
                    }
                }
            }
        }
    }

    private fun filterDisabledRules(rootNode: ASTNode, fqRuleId: String): Boolean {
        return rootNode.getUserData(DISABLED_RULES)?.contains(fqRuleId) == false
    }

    @Deprecated(
        message = "Should not be a part of public api. Will be removed in future release.",
        level = DeprecationLevel.WARNING
    )
    public fun calculateLineColByOffset(
        text: String
    ): (offset: Int) -> Pair<Int, Int> {
        return buildPositionInTextLocator(text)
    }

    /**
     * Fix style violations.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    @OptIn(FeatureInAlphaState::class)
    public fun format(params: Params): String = format(toExperimentalParams(params))

    /**
     * Fix style violations.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    /**
     * Fix style violations.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    @FeatureInAlphaState
    public fun format(params: ExperimentalParams): String {
        val hasUTF8BOM = params.text.startsWith(UTF8_BOM)
        val psiFileFactory = kotlinPsiFileFactory.acquirePsiFileFactory(params.isInvokedFromCli)
        val preparedCode = prepareCodeForLinting(psiFileFactory, params)

        var tripped = false
        var mutated = false
        visitor(preparedCode.rootNode, params.ruleSets, concurrent = false)
            .invoke { node, rule, fqRuleId ->
                // fixme: enforcing suppression based on node.startOffset is wrong
                // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
                if (
                    !preparedCode.suppressedRegionLocator(node.startOffset, fqRuleId, node === preparedCode.rootNode)
                ) {
                    try {
                        rule.visit(node, true) { _, _, canBeAutoCorrected ->
                            tripped = true
                            if (canBeAutoCorrected) {
                                mutated = true
                                if (preparedCode.suppressedRegionLocator !== noSuppression) {
                                    preparedCode.suppressedRegionLocator = buildSuppressedRegionsLocator(
                                        preparedCode.rootNode
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        kotlinPsiFileFactory.releasePsiFileFactory()
                        // line/col cannot be reliably mapped as exception might originate from a node not present
                        // in the original AST
                        throw RuleExecutionException(0, 0, fqRuleId, e)
                    }
                }
            }
        if (tripped) {
            val errors = mutableListOf<Pair<LintError, Boolean>>()
            visitor(preparedCode.rootNode, params.ruleSets).invoke { node, rule, fqRuleId ->
                // fixme: enforcing suppression based on node.startOffset is wrong
                // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
                if (
                    !preparedCode.suppressedRegionLocator(node.startOffset, fqRuleId, node === preparedCode.rootNode)
                ) {
                    try {
                        rule.visit(node, false) { offset, errorMessage, canBeAutoCorrected ->
                            // https://github.com/shyiko/ktlint/issues/158#issuecomment-462728189
                            if (
                                node.startOffset != offset &&
                                preparedCode.suppressedRegionLocator(offset, fqRuleId, node === preparedCode.rootNode)
                            ) {
                                return@visit
                            }
                            val (line, col) = preparedCode.positionInTextLocator(offset)
                            errors.add(Pair(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected), false))
                        }
                    } catch (e: Exception) {
                        val (line, col) = preparedCode.positionInTextLocator(node.startOffset)
                        kotlinPsiFileFactory.releasePsiFileFactory()
                        throw RuleExecutionException(line, col, fqRuleId, e)
                    }
                }
            }

            errors
                .sortedWith { (l), (r) -> if (l.line != r.line) l.line - r.line else l.col - r.col }
                .forEach { (e, corrected) -> params.cb(e, corrected) }
        }

        if (!mutated) {
            kotlinPsiFileFactory.releasePsiFileFactory()
            return params.text
        }

        val code = preparedCode
            .rootNode
            .text
            .replace("\n", determineLineSeparator(params.text, params.userData))

        kotlinPsiFileFactory.releasePsiFileFactory()
        return if (hasUTF8BOM) {
            UTF8_BOM + code
        } else {
            code
        }
    }

    /**
     * Reduce memory usage of all internal caches.
     */
    public fun trimMemory() {
        editorConfigLoader.trimMemory()
    }

    /**
     * Generates Kotlin `.editorconfig` file section content based on [Params.ruleSets].
     *
     * Method loads merged `.editorconfig` content from [Params.fileName] path,
     * and then, by querying rules from [Params.ruleSets] for missing properties default values,
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
    @FeatureInAlphaState
    public fun generateKotlinEditorConfigSection(
        params: ExperimentalParams
    ): String {
        val filePath = params.normalizedFilePath
        requireNotNull(filePath) {
            "Please pass path to existing Kotlin file"
        }
        return editorConfigGenerator.generateEditorconfig(
            filePath,
            params.rules,
            params.userData.isAndroidCodeStyle,
            params.debug
        )
    }

    private fun determineLineSeparator(fileContent: String, userData: Map<String, String>): String {
        val eol = userData["end_of_line"]?.trim()?.toLowerCase()
        return when {
            eol == "native" -> System.lineSeparator()
            eol == "crlf" || eol != "lf" && fileContent.lastIndexOf('\r') != -1 -> "\r\n"
            else -> "\n"
        }
    }

    private fun PsiElement.findErrorElement(): PsiErrorElement? {
        if (this is PsiErrorElement) {
            return this
        }
        this.children.forEach { child ->
            val errorElement = child.findErrorElement()
            if (errorElement != null) {
                return errorElement
            }
        }
        return null
    }

    private class PreparedCode(
        val rootNode: FileASTNode,
        val positionInTextLocator: (offset: Int) -> LineAndColumn,
        var suppressedRegionLocator: SuppressionLocator
    )
}
