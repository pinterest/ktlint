package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.codeStyleSetProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.emptyEditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.internal.EditorConfigGenerator
import com.pinterest.ktlint.core.internal.EditorConfigLoader
import com.pinterest.ktlint.core.internal.KotlinPsiFileFactoryProvider
import com.pinterest.ktlint.core.internal.LineAndColumn
import com.pinterest.ktlint.core.internal.SuppressionLocator
import com.pinterest.ktlint.core.internal.SuppressionLocatorBuilder
import com.pinterest.ktlint.core.internal.VisitorProvider
import com.pinterest.ktlint.core.internal.buildPositionInTextLocator
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

public object KtLint {
    public val FILE_PATH_USER_DATA_KEY: Key<String> = Key<String>("FILE_PATH")
    public val EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY: Key<EditorConfigProperties> =
        Key<EditorConfigProperties>("EDITOR_CONFIG_PROPERTIES")
    private const val UTF8_BOM = "\uFEFF"
    public const val STDIN_FILE: String = "<stdin>"

    private val kotlinPsiFileFactoryProvider = KotlinPsiFileFactoryProvider()
    private val editorConfigLoader = EditorConfigLoader(FileSystems.getDefault())

    /**
     * @param fileName path of file to lint/format
     * @param text Contents of file to lint/format
     * @param ruleSets a collection of "RuleSet"s used to validate source
     * @param userData Map of user options. This field is deprecated and will be removed in a future version.
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
    public data class ExperimentalParams(
        val fileName: String? = null,
        val text: String,
        val ruleSets: Iterable<RuleSet>,
        val userData: Map<String, String> = emptyMap(), // TODO: remove in a future version
        val cb: (e: LintError, corrected: Boolean) -> Unit,
        val script: Boolean = false,
        val editorConfigPath: String? = null,
        val debug: Boolean = false,
        val editorConfigOverride: EditorConfigOverride = emptyEditorConfigOverride,
        val isInvokedFromCli: Boolean = false
    ) {
        init {
            // Extract all default and custom ".editorconfig" properties which are registered
            val editorConfigProperties =
                ruleSets
                    .asSequence()
                    .flatten()
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

        internal val rules: Set<Rule>
            get() = ruleSets
                .flatMap {
                    it.rules.toList()
                }
                .toSet()
    }

    /**
     * Check source for lint errors.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    public fun lint(params: ExperimentalParams) {
        val psiFileFactory = kotlinPsiFileFactoryProvider.getKotlinPsiFileFactory(params.isInvokedFromCli)
        val preparedCode = prepareCodeForLinting(psiFileFactory, params)
        val errors = mutableListOf<LintError>()

        VisitorProvider(params)
            .visitor(preparedCode.rootNode)
            .invoke { node, rule, fqRuleId ->
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

                        throw RuleExecutionException(line, col, fqRuleId, e)
                    }
                }
            }

        errors
            .sortedWith { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { e -> params.cb(e, false) }
    }

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

        if (!params.isStdIn) {
            rootNode.putUserData(FILE_PATH_USER_DATA_KEY, params.normalizedFilePath.toString())
        }
        rootNode.putUserData(EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY, editorConfigProperties)

        val suppressedRegionLocator = SuppressionLocatorBuilder.buildSuppressedRegionsLocator(rootNode)

        return PreparedCode(
            rootNode,
            positionInTextLocator,
            suppressedRegionLocator
        )
    }

    @Deprecated(
        message = "Should not be a part of public api. Will be removed in future release."
    )
    public fun normalizeText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceFirst(UTF8_BOM, "")
    }

    /**
     * Fix style violations.
     *
     * @throws ParseException if text is not a valid Kotlin code
     * @throws RuleExecutionException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(params: ExperimentalParams): String {
        val hasUTF8BOM = params.text.startsWith(UTF8_BOM)
        val psiFileFactory = kotlinPsiFileFactoryProvider.getKotlinPsiFileFactory(params.isInvokedFromCli)
        val preparedCode = prepareCodeForLinting(psiFileFactory, params)

        var tripped = false
        var mutated = false
        val errors = mutableSetOf<Pair<LintError, Boolean>>()
        val visitorProvider = VisitorProvider(params = params)
        visitorProvider
            .visitor(
                preparedCode.rootNode,
                concurrent = false
            ).invoke { node, rule, fqRuleId ->
                // fixme: enforcing suppression based on node.startOffset is wrong
                // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
                if (
                    !preparedCode.suppressedRegionLocator(node.startOffset, fqRuleId, node === preparedCode.rootNode)
                ) {
                    try {
                        rule.visit(node, true) { offset, errorMessage, canBeAutoCorrected ->
                            tripped = true
                            if (canBeAutoCorrected) {
                                mutated = true
                                if (preparedCode.suppressedRegionLocator !== SuppressionLocatorBuilder.noSuppression) {
                                    preparedCode.suppressedRegionLocator =
                                        SuppressionLocatorBuilder.buildSuppressedRegionsLocator(
                                            preparedCode.rootNode
                                        )
                                }
                            }
                            val (line, col) = preparedCode.positionInTextLocator(offset)
                            errors.add(
                                Pair(
                                    LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected),
                                    // It is assumed that a rule that emits that an error can be autocorrected, also
                                    // does correct the error.
                                    canBeAutoCorrected
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // line/col cannot be reliably mapped as exception might originate from a node not present
                        // in the original AST
                        throw RuleExecutionException(0, 0, fqRuleId, e)
                    }
                }
            }
        if (tripped) {
            visitorProvider
                .visitor(preparedCode.rootNode)
                .invoke { node, rule, fqRuleId ->
                    // fixme: enforcing suppression based on node.startOffset is wrong
                    // (not just because not all nodes are leaves but because rules are free to emit (and fix!) errors at any position)
                    if (
                        !preparedCode.suppressedRegionLocator(
                            node.startOffset,
                            fqRuleId,
                            node === preparedCode.rootNode
                        )
                    ) {
                        try {
                            rule.visit(node, false) { offset, errorMessage, canBeAutoCorrected ->
                                // https://github.com/shyiko/ktlint/issues/158#issuecomment-462728189
                                if (
                                    node.startOffset != offset &&
                                    preparedCode.suppressedRegionLocator(
                                        offset,
                                        fqRuleId,
                                        node === preparedCode.rootNode
                                    )
                                ) {
                                    return@visit
                                }
                                val (line, col) = preparedCode.positionInTextLocator(offset)
                                errors.add(
                                    Pair(
                                        LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected),
                                        // It is assumed that a rule only corrects an error after it has emitted an
                                        // error and indicating that it actually can be autocorrected.
                                        false
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            val (line, col) = preparedCode.positionInTextLocator(node.startOffset)

                            throw RuleExecutionException(line, col, fqRuleId, e)
                        }
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
        params: ExperimentalParams
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
            params.rules,
            params.debug,
            codeStyle
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
