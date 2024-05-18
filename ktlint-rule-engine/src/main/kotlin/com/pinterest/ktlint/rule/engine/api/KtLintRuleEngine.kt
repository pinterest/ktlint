@file:Suppress("MemberVisibilityCanBePrivate")

package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.rule.engine.internal.AutoCorrectEnabledHandler
import com.pinterest.ktlint.rule.engine.internal.AutoCorrectOffsetRangeHandler
import com.pinterest.ktlint.rule.engine.internal.CodeFormatter
import com.pinterest.ktlint.rule.engine.internal.CodeLinter
import com.pinterest.ktlint.rule.engine.internal.EditorConfigFinder
import com.pinterest.ktlint.rule.engine.internal.EditorConfigGenerator
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoader
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoaderEc4j
import com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.Companion.createRuleExecutionContext
import com.pinterest.ktlint.rule.engine.internal.ThreadSafeEditorConfigCache.Companion.THREAD_SAFE_EDITOR_CONFIG_CACHE
import org.ec4j.core.Resource
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

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

    private val codeLinter = CodeLinter(this)
    private val codeFormatter = CodeFormatter(this)

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
    ): Unit = codeLinter.lint(code, callback)

    /**
     * Fix style violations in [code] for lint errors when possible. If [code] is passed as file reference then the '.editorconfig' files on
     * the path are taken into account. For each lint violation found, the [callback] is invoked.
     *
     * If [code] contains lint errors which have been autocorrected, then the resulting code is formatted again (up until
     * [MAX_FORMAT_RUNS_PER_FILE] times) in order to fix lint errors that might result from the previous formatting run.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(
        code: Code,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
    ): String = codeFormatter.format(code, AutoCorrectEnabledHandler, callback, MAX_FORMAT_RUNS_PER_FILE)

    /**
     * Fix style violations in [code] for lint errors found in the [autoCorrectOffsetRange] when possible. If [code] is passed as file
     * reference then the '.editorconfig' files on the path are taken into account. For each lint violation found, the [callback] is
     * invoked.
     *
     * If [code] contains lint errors which have been autocorrected, then the resulting code is formatted again (up until
     * [MAX_FORMAT_RUNS_PER_FILE] times) in order to fix lint errors that might result from the previous formatting run.
     *
     * IMPORTANT: Partial formatting not always works as expected. The offset of the node which is triggering the violation does not
     * necessarily to be close to the offset at which the violation is reported. Counter-intuitively the offset of the trigger node must be
     * located inside the [autoCorrectOffsetRange] instead of the offset at which the violation is reported.
     *
     * For example, the given code might contain the when-statement below:
     * ```
     *    // code with lint violations
     *
     *     when(foobar) {
     *         FOO -> "Single line"
     *         BAR ->
     *             """
     *             Multi line
     *             """.trimIndent()
     *         else -> null
     *     }
     *
     *    // more code with lint violations
     * ```
     * The `blank-line-between-when-conditions` rule requires blank lines to be added between the conditions. If the when-keyword above is
     * included in the range which is to be formatted then the blank lines before the conditions are added. If only the when-conditions
     * itself are selected, but not the when-keyword, then the blank lines are not added.
     *
     * This unexpected behavior is a side effect of the way the partial formatting is implemented currently. The side effects can be
     * prevented by delaying the decision to autocorrect as late as possible and the exact offset of the error is known. This however would
     * cause a breaking change, and needs to wait until Ktlint V2.x.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(
        code: Code,
        autoCorrectOffsetRange: IntRange,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
    ): String =
        codeFormatter.format(
            code,
            AutoCorrectOffsetRangeHandler(autoCorrectOffsetRange),
            callback,
            MAX_FORMAT_RUNS_PER_FILE,
        )

    /**
     * Formats style violations in [code]. Whenever a [LintError] is found which can be autocorrected by the rule that detected the
     * violation, the [callback] is invoked to let the calling API Consumer to decide whether the [LintError] is actually to be fixed.
     *
     * Important: [callback] is only invoked if the rule that found that the violation has implemented the [RuleAutocorrectApproveHandler]
     * and the violation can be autocorrected.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun interactiveFormat(
        code: Code,
        callback: (LintError) -> Boolean,
    ): String =
        codeFormatter.format(
            code = code,
            autoCorrectHandler = AutoCorrectEnabledHandler,
            callback = { lintError, _ -> callback(lintError) },
            maxFormatRunsPerFile = 1,
        )

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
     * into account with next calls to [lint] and/or [formatBatch]. See [editorConfigFilePaths] to get the list of
     * '.editorconfig' files which need to be observed.
     */
    public fun reloadEditorConfigFile(path: Path) {
        THREAD_SAFE_EDITOR_CONFIG_CACHE.reloadIfExists(
            Resource.Resources.ofPath(path, StandardCharsets.UTF_8),
        )
    }

    public fun transformToAst(code: Code): FileASTNode = createRuleExecutionContext(this, code).rootNode

    public companion object {
        internal const val UTF8_BOM = "\uFEFF"

        public const val STDIN_FILE: String = "<stdin>"

        private const val MAX_FORMAT_RUNS_PER_FILE = 3
    }
}
