@file:Suppress("MemberVisibilityCanBePrivate")

package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.END_OF_LINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.rule.engine.internal.EditorConfigFinder
import com.pinterest.ktlint.rule.engine.internal.EditorConfigGenerator
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoader
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoaderEc4j
import com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext
import com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.Companion.createRuleExecutionContext
import com.pinterest.ktlint.rule.engine.internal.ThreadSafeEditorConfigCache.Companion.THREAD_SAFE_EDITOR_CONFIG_CACHE
import com.pinterest.ktlint.rule.engine.internal.VisitorProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.ec4j.core.Resource
import org.ec4j.core.model.PropertyType.EndOfLineValue.crlf
import org.ec4j.core.model.PropertyType.EndOfLineValue.lf
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

public class KtLintRuleEngine(
    /**
     * The set of [RuleProvider]s to be invoked by the [KtLintRuleEngine]. A [RuleProvider] is able to create a new instance of a [Rule] so
     * that it can keep internal state and be called thread-safe manner
     */
    public val ruleProviders: Set<RuleProvider> = emptySet(),
    /**
     * The default values for `.editorconfig` properties which are not set explicitly in any '.editorconfig' file located on the path of the
     * file which is processed with the [KtLintRuleEngine]. If a property is set in [editorConfigDefaults] this takes precedence above the
     * default values defined in the KtLint project.
     */
    public val editorConfigDefaults: EditorConfigDefaults = EMPTY_EDITOR_CONFIG_DEFAULTS,
    /**
     * Override values for `.editorconfig` properties. If a property is set in [editorConfigOverride] it takes precedence above the same
     * property being set in any other way.
     */
    public val editorConfigOverride: EditorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
    /**
     * **For internal use only**: indicates that linting was invoked from KtLint CLI tool. It enables some internals workarounds for Kotlin
     * Compiler initialization. This property is likely to be removed in any of next versions without further notice.
     */
    public val isInvokedFromCli: Boolean = false,
    /**
     * The [FileSystem] to be used. This property is primarily intended to be used in unit tests. By specifying an alternative [FileSystem]
     * the unit test gains control on whether the [EditorConfigLoader] should or should not read specific ".editorconfig" files. For
     * example, it is considered unwanted that a unit test is influenced by the ".editorconfig" file of the project in which the unit test
     * is included.
     */
    public val fileSystem: FileSystem = FileSystems.getDefault(),
) {
    init {
        require(ruleProviders.any()) {
            "A non-empty set of 'ruleProviders' need to be provided"
        }
    }

    internal val editorConfigLoaderEc4j = EditorConfigLoaderEc4j(ruleProviders.propertyTypes())

    internal val editorConfigLoader =
        EditorConfigLoader(
            fileSystem,
            editorConfigLoaderEc4j,
            editorConfigDefaults,
            editorConfigOverride,
        )

    /**
     * Check the [code] for lint errors. If [code] is path as file reference then the '.editorconfig' files on the path to file are taken
     * into account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun lint(
        code: Code,
        callback: (LintError) -> Unit = { },
    ) {
        LOGGER.debug { "Starting with linting file '${code.fileNameOrStdin()}'" }

        val ruleExecutionContext = createRuleExecutionContext(this, code)
        val errors = mutableListOf<LintError>()

        VisitorProvider(ruleExecutionContext.ruleProviders)
            .visitor()
            .invoke { rule ->
                ruleExecutionContext.executeRule(rule, false) { offset, errorMessage, canBeAutoCorrected ->
                    val (line, col) = ruleExecutionContext.positionInTextLocator(offset)
                    LintError(line, col, rule.ruleId, errorMessage, canBeAutoCorrected)
                        .let { lintError ->
                            errors.add(lintError)
                            // In trace mode report the violation immediately. The order in which violations are actually found might be
                            // different from the order in which they are reported. For debugging purposes it can be helpful to know the
                            // exact order in which violations are being solved.
                            LOGGER.trace { "Lint violation: ${lintError.logMessage(code)}" }
                        }
                }
            }

        errors
            .sortedWith { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { e -> callback(e) }

        LOGGER.debug { "Finished with linting file '${code.fileNameOrStdin()}'" }
    }

    /**
     * Fix style violations in [code] for lint errors when possible. If [code] is passed as file reference then the '.editorconfig' files on
     * the path are taken into account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(
        code: Code,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
    ): String {
        LOGGER.debug { "Starting with formatting file '${code.fileNameOrStdin()}'" }

        val hasUTF8BOM = code.content.startsWith(UTF8_BOM)
        val ruleExecutionContext = createRuleExecutionContext(this, code)

        val visitorProvider = VisitorProvider(ruleExecutionContext.ruleProviders)
        var formatRunCount = 0
        var mutated: Boolean
        val errors = mutableSetOf<Pair<LintError, Boolean>>()
        do {
            mutated = false
            formatRunCount++
            visitorProvider
                .visitor()
                .invoke { rule ->
                    ruleExecutionContext.executeRule(rule, true) { offset, errorMessage, canBeAutoCorrected ->
                        if (canBeAutoCorrected) {
                            mutated = true
                            /*
                             * Rebuild the suppression locator after each change in the AST as the offsets of the suppression hints might
                             * have changed.
                             */
                            ruleExecutionContext.rebuildSuppressionLocator()
                        }
                        val (line, col) = ruleExecutionContext.positionInTextLocator(offset)
                        LintError(line, col, rule.ruleId, errorMessage, canBeAutoCorrected)
                            .let { lintError ->
                                errors.add(
                                    Pair(
                                        lintError,
                                        // It is assumed that a rule that emits that an error can be autocorrected, also does correct the error.
                                        canBeAutoCorrected,
                                    ),
                                )
                                // In trace mode report the violation immediately. The order in which violations are actually found might be
                                // different from the order in which they are reported. For debugging purposes it can be helpful to know the
                                // exact order in which violations are being solved.
                                LOGGER.trace { "Format violation: ${lintError.logMessage(code)}" }
                            }
                    }
                }
        } while (mutated && formatRunCount < MAX_FORMAT_RUNS_PER_FILE)
        if (formatRunCount == MAX_FORMAT_RUNS_PER_FILE && mutated) {
            // It is unknown if the last format run introduces new lint violations which can be autocorrected. So run lint once more so that
            // the user can be informed about this correctly.
            var hasErrorsWhichCanBeAutocorrected = false
            visitorProvider
                .visitor()
                .invoke { rule ->
                    if (!hasErrorsWhichCanBeAutocorrected) {
                        ruleExecutionContext.executeRule(rule, false) { _, _, canBeAutoCorrected ->
                            if (canBeAutoCorrected) {
                                hasErrorsWhichCanBeAutocorrected = true
                            }
                        }
                    }
                }
            if (hasErrorsWhichCanBeAutocorrected) {
                LOGGER.warn {
                    "Format was not able to resolve all violations which (theoretically) can be autocorrected in file " +
                        "${code.filePathOrStdin()} in $MAX_FORMAT_RUNS_PER_FILE consecutive runs of format."
                }
            }
        }

        errors
            .sortedWith { (l), (r) -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { (e, corrected) -> callback(e, corrected) }

        if (!mutated && formatRunCount == 1) {
            return code.content
        }

        val formattedCode =
            ruleExecutionContext
                .rootNode
                .text
                .replace("\n", ruleExecutionContext.determineLineSeparator(code.content))
        return if (hasUTF8BOM) {
            UTF8_BOM + formattedCode
        } else {
            formattedCode
        }.also {
            LOGGER.debug { "Finished with formatting file '${code.fileNameOrStdin()}'" }
        }
    }

    private fun RuleExecutionContext.determineLineSeparator(fileContent: String): String {
        val eol = editorConfig[END_OF_LINE_PROPERTY]
        return when {
            eol == crlf || eol != lf && fileContent.lastIndexOf('\r') != -1 -> "\r\n"
            else -> "\n"
        }
    }

    /**
     * Generates Kotlin `.editorconfig` file section content based on a path to a file or directory. Given that path, all '.editorconfig'
     * files on that path are taken into account to determine the values of properties which are already used.
     *
     * @return Kotlin section editorconfig content. For example:
     * ```properties
     * final-newline=true
     * indent-size=4
     * ```
     */
    public fun generateKotlinEditorConfigSection(filePath: Path): String {
        val codeStyle =
            editorConfigOverride
                .properties[CODE_STYLE_PROPERTY]
                ?.parsed
                ?.safeAs<CodeStyleValue>()
                ?: CODE_STYLE_PROPERTY.defaultValue
        val rules =
            ruleProviders
                .map { it.createNewRuleInstance() }
                .distinctBy { it.ruleId }
                .toSet()
        return EditorConfigGenerator(fileSystem, editorConfigLoaderEc4j)
            .generateEditorconfig(
                rules,
                codeStyle,
                filePath,
            )
    }

    /**
     * Reduce memory usage by cleaning internal caches.
     */
    public fun trimMemory() {
        THREAD_SAFE_EDITOR_CONFIG_CACHE.clear()
    }

    /**
     * Get the list of files which will be accessed by KtLint when linting or formatting the given file or directory.
     * The API consumer can use this list to observe changes in '.editorconfig' files. Whenever such a change is
     * observed, the API consumer should call [reloadEditorConfigFile].
     * To avoid unnecessary access to the file system, it is best to call this method only once for the root of the
     * project which is to be [lint] or [format].
     */
    public fun editorConfigFilePaths(path: Path): List<Path> = EditorConfigFinder(editorConfigLoaderEc4j).findEditorConfigs(path)

    /**
     * Reloads an '.editorconfig' file given that it is currently loaded into the KtLint cache. This method is intended
     * to be called by the API consumer when it is aware of changes in the '.editorconfig' file that should be taken
     * into account with next calls to [lint] and/or [format]. See [editorConfigFilePaths] to get the list of
     * '.editorconfig' files which need to be observed.
     */
    public fun reloadEditorConfigFile(path: Path) {
        THREAD_SAFE_EDITOR_CONFIG_CACHE.reloadIfExists(
            Resource.Resources.ofPath(path, StandardCharsets.UTF_8),
        )
    }

    public fun transformToAst(code: Code): FileASTNode = createRuleExecutionContext(this, code).rootNode

    private fun LintError.logMessage(code: Code) =
        "${code.fileNameOrStdin()}:$line:$col: $detail ($ruleId)" +
            if (canBeAutoCorrected) {
                ""
            } else {
                " [cannot be autocorrected]"
            }

    public companion object {
        internal const val UTF8_BOM = "\uFEFF"

        public const val STDIN_FILE: String = "<stdin>"

        private const val MAX_FORMAT_RUNS_PER_FILE = 3
    }
}
