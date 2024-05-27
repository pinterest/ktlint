@file:Suppress("MemberVisibilityCanBePrivate")

package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.rule.engine.internal.AllAutocorrectHandler
import com.pinterest.ktlint.rule.engine.internal.CodeFormatter
import com.pinterest.ktlint.rule.engine.internal.EditorConfigFinder
import com.pinterest.ktlint.rule.engine.internal.EditorConfigGenerator
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoader
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoaderEc4j
import com.pinterest.ktlint.rule.engine.internal.LintErrorAutocorrectHandler
import com.pinterest.ktlint.rule.engine.internal.NoneAutocorrectHandler
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
    ) {
        codeFormatter.format(
            code = code,
            autocorrectHandler = NoneAutocorrectHandler,
            callback = { lintError, _ -> callback(lintError) },
            maxFormatRunsPerFile = 1,
        )
    }

    /**
     * Fix all style violations in [code] for lint errors when possible. If [code] is passed as file reference then the '.editorconfig'
     * files on the path are taken into account. For each lint violation found, the [callback] is invoked.
     *
     * If [code] contains lint errors which have been autocorrected, then the resulting code is formatted again (up until
     * [MAX_FORMAT_RUNS_PER_FILE] times) in order to fix lint errors that might result from the previous formatting run.
     *
     * [callback] is invoked once for each [LintError] found during any runs. As of that the [callback] might be invoked multiple times for
     * the same [LintError].
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    @Deprecated(message = "Marked for removal in Ktlint 2.0")
    public fun format(
        code: Code,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
    ): String = codeFormatter.format(code, AllAutocorrectHandler, callback, MAX_FORMAT_RUNS_PER_FILE)

    /**
     * Formats style violations in [code]. Whenever a [LintError] is found the [callback] is invoked. If the [LintError] can be
     * autocorrected *and* the rule that found that the violation has implemented the [RuleAutocorrectApproveHandler] interface, the API
     * Consumer determines whether that [LintError] is to autocorrected, or not.
     *
     * When autocorrecting a [LintError] it is possible that other violations are introduced. By default, format is run up until
     * [MAX_FORMAT_RUNS_PER_FILE] times. It is still possible that violations remain after the last run. This is a trait-off between solving
     * as many errors as possible versus bad performance in case an endless loop of violations exists. In case the [callback] is implemented
     * to let the user of the API Consumer to decide which [LintError] it to be autocorrected, or not, it might be better to disable this
     * behavior by disabling [rerunAfterAutocorrect].
     *
     * In case the rule has not implemented the [RuleAutocorrectApproveHandler] interface, then the result of the [callback] is ignored as
     * the rule is not able to process it. For such rules the [defaultAutocorrect] determines whether autocorrect for this rule is to be
     * applied, or not. By default, the autocorrect will be applied (backwards compatability).
     *
     * [callback] is invoked once for each [LintError] found during any runs. As of that the [callback] might be invoked multiple times for
     * the same [LintError].
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(
        code: Code,
        rerunAfterAutocorrect: Boolean = true,
        defaultAutocorrect: Boolean = true,
        callback: (LintError) -> AutocorrectDecision,
    ): String =
        codeFormatter.format(
            code = code,
            autocorrectHandler = LintErrorAutocorrectHandler(defaultAutocorrect, callback),
            maxFormatRunsPerFile =
                if (rerunAfterAutocorrect) {
                    MAX_FORMAT_RUNS_PER_FILE
                } else {
                    1
                },
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
     * into account with next calls to [lint] and/or [format]. See [editorConfigFilePaths] to get the list of
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
