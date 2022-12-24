@file:Suppress("MemberVisibilityCanBePrivate")

package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.KtLint.ExperimentalParams
import com.pinterest.ktlint.core.KtLint.format
import com.pinterest.ktlint.core.KtLint.lint
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.core.api.KtLintRuleException
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.api.editorconfig.DEFAULT_EDITOR_CONFIG_PROPERTIES
import com.pinterest.ktlint.core.internal.EditorConfigFinder
import com.pinterest.ktlint.core.internal.EditorConfigGenerator
import com.pinterest.ktlint.core.internal.EditorConfigLoader
import com.pinterest.ktlint.core.internal.RuleExecutionContext
import com.pinterest.ktlint.core.internal.RuleExecutionContext.Companion.createRuleExecutionContext
import com.pinterest.ktlint.core.internal.RuleExecutionException
import com.pinterest.ktlint.core.internal.RuleRunner
import com.pinterest.ktlint.core.internal.ThreadSafeEditorConfigCache.Companion.THREAD_SAFE_EDITOR_CONFIG_CACHE
import com.pinterest.ktlint.core.internal.VisitorProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import mu.KotlinLogging
import org.ec4j.core.Resource
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

@Deprecated("Marked for removal in KtLint 0.49. See changelog or KDoc for more information.")
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

    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("KtLintRuleEngine.Companion.STDIN_FILE"),
    )
    public const val STDIN_FILE: String = "<stdin>"

    /**
     * Parameters to invoke [KtLint.lint] and [KtLint.format] APIs.
     *
     * Marked for removal in KtLint 0.49. See deprecations of methods [KtLint.lint], [KtLint.format], and
     * [KtLint.generateKotlinEditorConfigSection].
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
    @Deprecated("Marked for removal in KtLint 0.49")
    public data class ExperimentalParams(
        val fileName: String? = null,
        val text: String,
        val ruleProviders: Set<RuleProvider> = emptySet(),
        val userData: Map<String, String> = emptyMap(),
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
                    .plus(DEFAULT_EDITOR_CONFIG_PROPERTIES)
                    .map { it.name }
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
     * Marked for removal in KtLint 0.49. The static object [KtLint] is replaced by class [KtLintRuleEngine]. Main
     * difference is that the class needs to be instantiated once with the [KtLintRuleEngineConfiguration] and is reused
     * for all subsequent calls. The call to [KtLintRuleEngine.lint] only takes a reference to the code that needs to be
     * linted instead of all configuration parameter as well.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    @Deprecated("Marked for removal in KtLint 0.49. See changelog or KDOC for migration to KtLintRuleEngine.")
    public fun lint(params: ExperimentalParams) {
        KtLintRuleEngine(
            params.ruleProviders,
            params.editorConfigDefaults,
            params.editorConfigOverride,
            params.isInvokedFromCli,
        ).lint(
            Code.CodeSnippetLegacy(
                content = params.text,
                fileName = params.fileName,
                script = params.script,
                isStdIn = params.isStdIn,
            ),
        ) { params.cb(it, false) }
    }

    /**
     * Fix style violations.
     *
     * Marked for removal in KtLint 0.49. The static object [KtLint] is replaced by class [KtLintRuleEngine]. Main
     * difference is that the class needs to be instantiated once with the [KtLintRuleEngineConfiguration] and is reused
     * for all subsequent calls. The call to [KtLintRuleEngine.format] only takes a reference to the code that needs to
     * be formatted instead of all configuration parameter as well.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    @Deprecated("Marked for removal in KtLint 0.49. See changelog or KDOC for migration to KtLintRuleEngine.")
    public fun format(params: ExperimentalParams): String =
        KtLintRuleEngine(
            params.ruleProviders,
            params.editorConfigDefaults,
            params.editorConfigOverride,
            params.isInvokedFromCli,
        ).format(
            Code.CodeSnippetLegacy(
                content = params.text,
                fileName = params.fileName,
                script = params.script,
                isStdIn = params.isStdIn,
            ),
        ) { lintError, autocorrected -> params.cb(lintError, autocorrected) }

    /**
     * Reduce memory usage by cleaning internal caches.
     */
    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("ktLintRuleEngine.trimMemory"),
    )
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
    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("ktLintRuleEngine.editorConfigFilePaths"),
    )
    public fun editorConfigFilePaths(path: Path): List<Path> =
        EditorConfigFinder().findEditorConfigs(path)

    /**
     * Reloads an '.editorconfig' file given that it is currently loaded into the KtLint cache. This method is intended
     * to be called by the API consumer when it is aware of changes in the '.editorconfig' file that should be taken
     * into account with next calls to [lint] and/or [format]. See [editorConfigFilePaths] to get the list of
     * '.editorconfig' files which need to be observed.
     */
    @Deprecated(
        message = "Marked for removal in KtLint 0.49",
        replaceWith = ReplaceWith("ktLintRuleEngine.reloadEditorConfigFile"),
    )
    public fun reloadEditorConfigFile(path: Path) {
        THREAD_SAFE_EDITOR_CONFIG_CACHE.reloadIfExists(
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
    @Deprecated(
        message = "Marked for removal in KtLint 0.49.",
        replaceWith = ReplaceWith("ktLintRuleEngine.generateKotlinEditorConfigSection(path)"),
    )
    public fun generateKotlinEditorConfigSection(
        params: ExperimentalParams,
    ): String {
        val filePath = params.normalizedFilePath
        requireNotNull(filePath) {
            "Please pass path to existing Kotlin file"
        }
        return KtLintRuleEngine(
            params.ruleProviders,
            params.editorConfigDefaults,
            params.editorConfigOverride,
            params.isInvokedFromCli,
        ).generateKotlinEditorConfigSection(filePath)
    }
}

public sealed class Code(
    internal open val content: String,
    internal open val fileName: String?,
    internal open val filePath: Path?,
    internal open val script: Boolean,
    internal open val isStdIn: Boolean,
) {
    /**
     * A [file] containing valid Kotlin code or script. The '.editorconfig' files on the path to [file] are taken into account.
     */
    public class CodeFile(
        private val file: File,
    ) : Code(
        content = file.readText(),
        fileName = file.name,
        filePath = file.toPath(),
        script = file.name.endsWith(".kts", ignoreCase = true),
        isStdIn = false,
    )

    /**
     * The [content] represent a valid piece of Kotlin code or Kotlin script. The '.editorconfig' files on the filesystem are ignored as the
     * snippet is not associated with a file path. Use [CodeFile] for scanning a file while at the same time respecting the '.editorconfig'
     * files on the path to the file.
     */
    public class CodeSnippet(
        override val content: String,
        override val script: Boolean = false,
    ) : Code(
        content = content,
        filePath = null,
        fileName = null,
        script = script,
        isStdIn = true,
    )

    @Deprecated(message = "Remove in KtLint 0.49 when class ExperimentalParams is removed")
    internal class CodeSnippetLegacy(
        override val content: String,
        override val fileName: String? = null,
        override val script: Boolean = fileName?.endsWith(".kts", ignoreCase = true) == true,
        override val isStdIn: Boolean = fileName == KtLintRuleEngine.STDIN_FILE,
    ) : Code(
        content = content,
        fileName = fileName,
        filePath = fileName?.let { Paths.get(fileName) },
        script = script,
        isStdIn = isStdIn,
    )
}

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
) {
    init {
        require(ruleProviders.any()) {
            "A non-empty set of 'ruleProviders' need to be provided"
        }
    }

    internal val editorConfigLoader = EditorConfigLoader(FileSystems.getDefault())

    /**
     * Check [code] for lint errors. When [filePath] is provided, the '.editorconfig' files on the path are taken into
     * account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    @Deprecated(message = "Marked for removal in Ktlint 0.49")
    public fun lint(
        code: String,
        filePath: Path? = null,
        callback: (LintError) -> Unit = { },
    ) {
        lint(
            Code.CodeSnippetLegacy(
                content = code,
                fileName = filePath?.absolutePathString(),
            ),
            callback,
        )
    }

    /**
     * Check the code in file [filePath] for lint errors. The '.editorconfig' files on the path to file are taken into
     * account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    @Deprecated(message = "Marked for removal in Ktlint 0.49")
    public fun lint(
        filePath: Path,
        callback: (LintError) -> Unit = { },
    ) {
        lint(
            Code.CodeFile(filePath.toFile()),
            callback,
        )
    }

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
        val ruleExecutionContext = createRuleExecutionContext(this, code)
        val errors = mutableListOf<LintError>()

        try {
            VisitorProvider(ruleExecutionContext.ruleRunners)
                .visitor(ruleExecutionContext.editorConfigProperties)
                .invoke { rule, fqRuleId ->
                    ruleExecutionContext.executeRule(rule, fqRuleId, false) { offset, errorMessage, canBeAutoCorrected ->
                        val (line, col) = ruleExecutionContext.positionInTextLocator(offset)
                        errors.add(LintError(line, col, fqRuleId, errorMessage, canBeAutoCorrected))
                    }
                }
        } catch (e: RuleExecutionException) {
            throw e.toKtLintRuleException(code.fileName)
        }

        errors
            .sortedWith { l, r -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { e -> callback(e) }

        LOGGER.debug("Finished with linting file '${code.fileName}'")
    }

    /**
     * Fix style violations in [code] for lint errors when possible. When [filePath] is provided, the '.editorconfig'
     * files on the path are taken into account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    @Deprecated(message = "Marked for removal in Ktlint 0.49")
    public fun format(
        code: String,
        filePath: Path? = null,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
    ): String =
        format(
            Code.CodeSnippetLegacy(
                content = code,
                fileName = filePath?.absolutePathString(),
            ),
            callback,
        )

    /**
     * Fix style violations in code of file [filePath] for lint errors when possible. The '.editorconfig' files on the
     * path are taken into account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    public fun format(
        filePath: Path,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
    ): String =
        format(
            Code.CodeFile(filePath.toFile()),
            callback,
        )

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
        val hasUTF8BOM = code.content.startsWith(UTF8_BOM)
        val ruleExecutionContext = createRuleExecutionContext(this, code)

        var tripped = false
        var mutated = false
        val errors = mutableSetOf<Pair<LintError, Boolean>>()
        val ruleRunners =
            ruleProviders
                .map { RuleRunner(it) }
                .distinctBy { it.ruleId }
                .toSet()
        val visitorProvider = VisitorProvider(ruleRunners)
        try {
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
        } catch (e: RuleExecutionException) {
            throw e.toKtLintRuleException(code.fileName)
        }
        if (tripped) {
            try {
                visitorProvider
                    .visitor(ruleExecutionContext.editorConfigProperties)
                    .invoke { rule, fqRuleId ->
                        ruleExecutionContext.executeRule(
                            rule,
                            fqRuleId,
                            false,
                        ) { offset, errorMessage, canBeAutoCorrected ->
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
            } catch (e: RuleExecutionException) {
                throw e.toKtLintRuleException(code.fileName)
            }
        }

        errors
            .sortedWith { (l), (r) -> if (l.line != r.line) l.line - r.line else l.col - r.col }
            .forEach { (e, corrected) -> callback(e, corrected) }

        if (!mutated) {
            return code.content
        }

        val formattedCode = ruleExecutionContext
            .rootNode
            .text
            .replace("\n", ruleExecutionContext.determineLineSeparator(code.content))
        return if (hasUTF8BOM) {
            UTF8_BOM + formattedCode
        } else {
            formattedCode
        }.also {
            LOGGER.debug("Finished with formatting file '${code.fileName}'")
        }
    }

    private fun RuleExecutionContext.determineLineSeparator(fileContent: String): String {
        val eol =
            editorConfigProperties["end_of_line"]
                ?.sourceValue
        return when {
            eol == "native" -> System.lineSeparator()
            eol == "crlf" || eol != "lf" && fileContent.lastIndexOf('\r') != -1 -> "\r\n"
            else -> "\n"
        }
    }

    /**
     * Generates Kotlin `.editorconfig` file section content based on [ExperimentalParams].
     *
     * Method loads merged `.editorconfig` content from [ExperimentalParams] path, and then, by querying rules from
     * [KtLintRuleEngineConfiguration] for missing properties default values, generates Kotlin section (default is
     * `[*.{kt,kts}]`) new content.
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
                .map { RuleRunner(it) }
                .distinctBy { it.ruleId }
                .toSet()
                .map { it.getRule() }
                .toSet()
        return EditorConfigGenerator(this.editorConfigLoader).generateEditorconfig(
            filePath,
            rules,
            codeStyle,
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
    public fun editorConfigFilePaths(path: Path): List<Path> =
        EditorConfigFinder().findEditorConfigs(path)

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

    public companion object {
        internal const val UTF8_BOM = "\uFEFF"

        public const val STDIN_FILE: String = KtLint.STDIN_FILE
    }
}

private fun RuleExecutionException.toKtLintRuleException(fileName: String?) =
    KtLintRuleException(
        line,
        col,
        ruleId,
        "Rule '$ruleId' throws exception in file '$fileName' at position ($line:$col)",
        cause,
    )
