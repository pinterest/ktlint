package com.pinterest.ktlint.cli.internal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.pinterest.ktlint.cli.reporter.baseline.Baseline
import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.doesNotContain
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.KOTLIN_PARSE_EXCEPTION
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.KTLINT_RULE_ENGINE_EXCEPTION
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.LINT_CAN_BE_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import com.pinterest.ktlint.cli.reporter.plain.Color
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.logger.api.setDefaultLoggerModifier
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import com.pinterest.ktlint.ruleset.standard.rules.FILENAME_RULE_ID
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private lateinit var logger: KLogger

@Command(
    headerHeading =
        """
An anti-bikeshedding Kotlin linter with built-in formatter.
(https://github.com/pinterest/ktlint).

Usage:
  ktlint <flags> [patterns]
  java -jar ktlint.jar <flags> [patterns]

Examples:
  # Check the style of all Kotlin files (ending with '.kt' or '.kts') inside the current dir (recursively).
  #
  # Hidden folders will be skipped.
  ktlint

  # Check only certain locations starting from the current directory.
  #
  # Prepend ! to negate the pattern, KtLint uses .gitignore pattern style syntax.
  # Globs are applied starting from the last one.
  #
  # Hidden folders will be skipped.
  # Check all '.kt' files in 'src/' directory, but ignore files ending with 'Test.kt':
  ktlint "src/**/*.kt" "!src/**/*Test.kt"
  # Check all '.kt' files in 'src/' directory, but ignore 'generated' directory and its subdirectories:
  ktlint "src/**/*.kt" "!src/**/generated/**"

  # Auto-correct style violations.
  ktlint -F "src/**/*.kt"

  # Using custom reporter jar and overriding report location
  ktlint --reporter=csv,artifact=/path/to/reporter/csv.jar,output=my-custom-report.csv
Flags:
""",
    synopsisHeading = "",
    customSynopsis = [""],
    sortOptions = false,
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class,
)
internal class KtlintCommandLine {
    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    @Option(
        // Ensure that the code-style can be set on sub commands and is visible in the help documentation
        scope = CommandLine.ScopeType.INHERIT,
        names = ["--code-style"],
        description = [
            "Defines the code style (ktlint_official, intellij_idea or android_studio) to be used for formatting the code. This option " +
                "is deprecated, and will be removed in Ktlint 1.1. The code style has to be defined as '.editorconfig' property " +
                "'ktlint_code_style'.",
        ],
        converter = [CodeStyleValueConverter::class],
    )
    @Deprecated("Marked for removal in Ktlint 1.1")
    var codeStyle: CodeStyleValue? = null

    @Option(
        names = ["--color"],
        description = ["Make output colorful"],
    )
    var color: Boolean = false

    @Option(
        names = ["--color-name"],
        description = ["Customize the output color"],
    )
    var colorName: String = Color.DARK_GRAY.name

    @Option(
        names = ["--disabled_rules"],
        description = [
            "Comma-separated list of rules to globally disable. This option is deprecated, and will be removed in Ktlint 1.1. The " +
                "disabled rules have to be defined as '.editorconfig' properties. See " +
                "https://pinterest.github.io/ktlint/1.0.0/faq/#how-do-i-enable-or-disable-a-rule",
        ],
    )
    @Deprecated("Marked for removal in Ktlint 1.1")
    var disabledRules: String = ""

    @Option(
        names = ["--format", "-F"],
        description = ["Fix deviations from the code style when possible"],
    )
    private var format: Boolean = false

    @Option(
        names = ["--limit"],
        description = ["Maximum number of errors to show (default: show all)"],
    )
    private var limit: Int = -1
        get() = if (field < 0) Int.MAX_VALUE else field

    @Option(
        names = ["--relative"],
        description = [
            "Print files relative to the working directory " +
                "(e.g. dir/file.kt instead of /home/user/project/dir/file.kt)",
        ],
    )
    var relative: Boolean = false

    @Option(
        names = ["--reporter"],
        description = [
            "A reporter to use (built-in: plain (default), plain?group_by_file, plain-summary, json, sarif, " +
                "checkstyle, html). To use a third-party reporter specify a path to a JAR file on the filesystem " +
                "via ',artifact=' option. To override reporter output, use ',output=' option.",
        ],
    )
    private var reporterConfigurations: List<String> = ArrayList()

    @Option(
        names = ["--ruleset", "-R"],
        description = ["A path to a JAR file containing additional ruleset(s)"],
    )
    var rulesetJarPaths: List<String> = ArrayList()

    @Option(
        names = ["--stdin"],
        description = ["Read file from stdin"],
    )
    private var stdin: Boolean = false

    @Option(
        names = ["--patterns-from-stdin"],
        description = [
            "Read additional patterns to check/format from stdin. " +
                "Patterns are delimited by the given argument. (default is newline) " +
                "If the argument is an empty string, the NUL byte is used.",
        ],
        arity = "0..1",
        fallbackValue = "\n",
    )
    private var stdinDelimiter: String? = null

    @Option(
        names = ["--editorconfig"],
        description = [
            "Path to the default '.editorconfig'. A property value from this file is used only when no " +
                "'.editorconfig' file on the path to the source file specifies that property. Note: up until ktlint " +
                "0.46 the property value in this file used to override values found in '.editorconfig' files on the " +
                "path to the source file.",
        ],
    )
    private var editorConfigPath: String? = null

    @Option(
        names = ["--experimental"],
        description = [
            "Enable experimental rules. This option is deprecated, and will be removed in Ktlint 1.1. The experimental flag has to be " +
                "set as '.editorconfig' property 'ktlint_experimental'. See " +
                "https://pinterest.github.io/ktlint/1.0.0/faq/#how-do-i-enable-or-disable-a-rule-set",
        ],
    )
    @Deprecated("Marked for removal in Ktlint 1.1")
    var experimental: Boolean = false

    @Option(
        names = ["--baseline"],
        description = ["Defines a baseline file to check against"],
    )
    private var baselinePath: String = ""

    @Parameters(hidden = true)
    private var patterns = emptyList<String>()

    @Option(
        names = ["--log-level", "-l"],
        description = ["Defines the minimum log level (trace, debug, info, warn, error) or none to suppress all logging"],
        converter = [LogLevelConverter::class],
    )
    private var minLogLevel: Level = Level.INFO

    private val tripped = AtomicBoolean()
    private val fileNumber = AtomicInteger()
    private val errorNumber = AtomicInteger()
    private val adviseToUseFormat = AtomicBoolean()

    internal var debug: Boolean = false
        get() = Level.DEBUG.isGreaterOrEqual(minLogLevel)
        private set

    private fun disabledRulesEditorConfigOverrides() =
        disabledRules
            .split(",")
            .filter { it.isNotBlank() }
            .map {
                // For backwards compatibility, prefix the rule id with the standard rule set id when missing
                RuleId.prefixWithStandardRuleSetIdWhenMissing(it)
            }.map { RuleId(it).createRuleExecutionEditorConfigProperty() to RuleExecution.disabled }
            .toTypedArray()

    fun run() {
        val editorConfigOverride =
            EditorConfigOverride
                .EMPTY_EDITOR_CONFIG_OVERRIDE
                .applyIf(experimental) {
                    logger.warn {
                        "Parameter `--experimental is deprecated, and will be removed in Ktlint 1.1. The experimental flag has to be " +
                            "set as '.editorconfig' property 'ktlint_experimental = enabled'. See " +
                            "https://pinterest.github.io/ktlint/1.0.0/faq/#how-do-i-enable-or-disable-a-rule-set"
                    }
                    plus(EXPERIMENTAL_RULES_EXECUTION_PROPERTY to RuleExecution.enabled)
                }.applyIf(disabledRules.isNotBlank()) {
                    logger.warn {
                        "Parameter `--disabled-rules is deprecated, and will be removed in Ktlint 1.1. The disabled rules have to be " +
                            "defined as '.editorconfig' properties. See " +
                            "https://pinterest.github.io/ktlint/1.0.0/faq/#how-do-i-enable-or-disable-a-rule"
                    }
                    plus(*disabledRulesEditorConfigOverrides())
                }.applyIf(codeStyle != null) {
                    logger.warn {
                        "Parameter `--code-style=${codeStyle?.name} is deprecated. The code style should be defined as '.editorconfig' " +
                            "property 'ktlint_code_style'."
                    }
                    plus(CODE_STYLE_PROPERTY to codeStyle)
                }.applyIf(stdin) {
                    logger.debug {
                        "Add editor config override to disable 'filename' rule which can not be used in combination with reading from " +
                            "<stdin>"
                    }
                    plus(FILENAME_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.disabled)
                }

        assertStdinAndPatternsFromStdinOptionsMutuallyExclusive()
        patterns = patterns.replaceWithPatternsFromStdinOrDefaultPatternsWhenEmpty()

        val start = System.currentTimeMillis()

        val ruleProviders = ruleProviders()
        val ktLintRuleEngine =
            KtLintRuleEngine(
                ruleProviders = ruleProviders,
                editorConfigDefaults = editorConfigDefaults(ruleProviders),
                editorConfigOverride = editorConfigOverride,
                isInvokedFromCli = true,
            )

        val baseline =
            if (stdin || baselinePath.isBlank()) {
                Baseline(status = Baseline.Status.DISABLED)
            } else {
                loadBaseline(baselinePath, BaselineErrorHandling.LOG)
            }
        val aggregatedReporter =
            ReporterAggregator(
                baseline,
                reporterConfigurations,
                color,
                colorName,
                stdin,
                format,
                relative,
            ).aggregatedReporter()

        aggregatedReporter.beforeAll()
        if (stdin) {
            lintStdin(
                ktLintRuleEngine,
                aggregatedReporter,
            )
        } else {
            lintFiles(
                ktLintRuleEngine,
                baseline.lintErrorsPerFile,
                aggregatedReporter,
            )
            if (adviseToUseFormat.get()) {
                if (format) {
                    logger.error { "Format was not able to autocorrect all errors that theoretically can be autocorrected." }
                } else {
                    logger.warn { "Lint has found errors than can be autocorrected using 'ktlint --format'" }
                }
            }
        }
        aggregatedReporter.afterAll()

        logger.debug {
            "Finished processing in ${System.currentTimeMillis() - start}ms / $fileNumber file(s) scanned / $errorNumber error(s) found"
        }
        if (fileNumber.get() == 0) {
            // Do not return an error as this would implicate that in a multi-module project, each module has to contain
            // at least one kotlin file.
            logger.warn { "No files matched $patterns" }
        }
        if (tripped.get()) {
            exitKtLintProcess(1)
        } else {
            exitKtLintProcess(0)
        }
    }

    private fun editorConfigDefaults(ruleProviders: Set<RuleProvider>): EditorConfigDefaults {
        val fullyExpandedEditorConfigPath =
            editorConfigPath
                ?.expandTildeToFullPath()
                ?.let { path -> Paths.get(path) }
        return EditorConfigDefaults.load(fullyExpandedEditorConfigPath, ruleProviders.propertyTypes())
    }

    private fun List<String>.replaceWithPatternsFromStdinOrDefaultPatternsWhenEmpty(): List<String> {
        val localStdinDelimiter: String? = stdinDelimiter
        return when {
            localStdinDelimiter != null -> {
                val stdinPatterns: Set<String> = readPatternsFromStdin(localStdinDelimiter.ifEmpty { "\u0000" })
                if (isNotEmpty() && stdinPatterns.isNotEmpty()) {
                    logger.warn {
                        "Patterns specified at command line ($this) and patterns from 'stdin' due to flag '--patterns-from-stdin' " +
                            "($stdinPatterns) are merged"
                    }
                }
                // Note: it is okay in case both the original patterns and the patterns from stdin are empty
                this.plus(stdinPatterns)
            }
            this.isEmpty() -> {
                logger.info { "Enable default patterns $DEFAULT_PATTERNS" }
                DEFAULT_PATTERNS
            }
            else -> {
                // Keep original patterns
                this
            }
        }
    }

    // Do not convert to "val" as the function depends on PicoCli options which are not fully instantiated until the "run" method is started
    internal fun ruleProviders(): Set<RuleProvider> = loadRuleProviders(rulesetJarPaths.toFilesURIList())

    // Do not convert to "val" as the function depends on PicoCli options which are not fully instantiated until the "run" method is started
    internal fun configureLogger() {
        logger =
            KotlinLogging
                .logger {}
                .setDefaultLoggerModifier { logger -> logger.level = minLogLevel }
                .initKtLintKLogger()
    }

    private var KLogger.level: Level?
        get() = underlyingLogger()?.level
        set(value) {
            underlyingLogger()?.level = value
        }

    private fun KLogger.underlyingLogger(): Logger? =
        @Suppress("UNCHECKED_CAST")
        (this as? DelegatingKLogger<Logger>)
            ?.underlyingLogger

    private fun assertStdinAndPatternsFromStdinOptionsMutuallyExclusive() {
        if (stdin && stdinDelimiter != null) {
            throw ParameterException(
                commandSpec.commandLine(),
                "Options --stdin and --patterns-from-stdin are mutually exclusive",
            )
        }
    }

    private fun lintFiles(
        ktLintRuleEngine: KtLintRuleEngine,
        lintErrorsPerFile: Map<String, List<KtlintCliError>>,
        reporter: ReporterV2,
    ) {
        FileSystems
            .getDefault()
            .fileSequence(patterns)
            .map { it.toFile() }
            .takeWhile { errorNumber.get() < limit }
            .map { file ->
                Callable {
                    file to
                        process(
                            ktLintRuleEngine = ktLintRuleEngine,
                            code = Code.fromFile(file),
                            baselineLintErrors =
                                lintErrorsPerFile
                                    .getOrDefault(
                                        // Baseline stores the lint violations as relative path to work dir
                                        file.location(true),
                                        emptyList(),
                                    ),
                        )
                }
            }.parallel({ (file, errList) -> report(file.location(relative), errList, reporter) })
    }

    private fun lintStdin(
        ktLintRuleEngine: KtLintRuleEngine,
        reporter: ReporterV2,
    ) {
        report(
            KtLintRuleEngine.STDIN_FILE,
            process(
                ktLintRuleEngine = ktLintRuleEngine,
                code = Code.fromStdin(),
                baselineLintErrors = emptyList(),
            ),
            reporter,
        )
    }

    private fun report(
        relativeRoute: String,
        ktlintCliErrors: List<KtlintCliError>,
        reporter: ReporterV2,
    ) {
        fileNumber.incrementAndGet()
        val errListLimit = minOf(ktlintCliErrors.size, maxOf(limit - errorNumber.get(), 0))
        errorNumber.addAndGet(errListLimit)
        if (!adviseToUseFormat.get() && ktlintCliErrors.containsErrorThatCanBeAutocorrected()) {
            adviseToUseFormat.set(true)
        }

        reporter.before(relativeRoute)
        ktlintCliErrors
            .take(errListLimit)
            .forEach { reporter.onLintError(relativeRoute, it) }
        reporter.after(relativeRoute)
    }

    private fun List<KtlintCliError>.containsErrorThatCanBeAutocorrected() = any { it.status == LINT_CAN_BE_AUTOCORRECTED }

    private fun process(
        ktLintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineLintErrors: List<KtlintCliError>,
    ): List<KtlintCliError> {
        if (code.fileName != null) {
            logger.trace { "Checking ${File(code.fileName!!).location(relative)}" }
        }
        return if (format) {
            format(ktLintRuleEngine, code, baselineLintErrors)
        } else {
            lint(ktLintRuleEngine, code, baselineLintErrors)
        }
    }

    private fun format(
        ktLintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineLintErrors: List<KtlintCliError>,
    ): List<KtlintCliError> {
        val ktlintCliErrors = mutableListOf<KtlintCliError>()
        try {
            ktLintRuleEngine
                .format(code) { lintError, corrected ->
                    val ktlintCliError =
                        KtlintCliError(
                            line = lintError.line,
                            col = lintError.col,
                            ruleId = lintError.ruleId.value,
                            detail =
                                lintError
                                    .detail
                                    .applyIf(corrected) { "$this (cannot be auto-corrected)" },
                            status =
                                if (corrected) {
                                    FORMAT_IS_AUTOCORRECTED
                                } else {
                                    LINT_CAN_NOT_BE_AUTOCORRECTED
                                },
                        )
                    if (baselineLintErrors.doesNotContain(ktlintCliError)) {
                        ktlintCliErrors.add(ktlintCliError)
                        if (!corrected) {
                            tripped.set(true)
                        }
                    }
                }.also { formattedFileContent ->
                    when {
                        code.isStdIn -> print(formattedFileContent)
                        code.content != formattedFileContent ->
                            code
                                .filePath
                                ?.toFile()
                                ?.writeText(formattedFileContent, charset("UTF-8"))
                    }
                }
        } catch (e: Exception) {
            if (code.isStdIn && e is KtLintParseException) {
                if (code.script) {
                    // When reading from stdin, code is only parsed as Kotlint script, if it could not be parsed as pure Kotlin. Now parsing
                    // of the code has failed for both, the file has to be ignored.
                    logger.error {
                        """
                        Can not parse input from <stdin> as Kotlin, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        """.trimIndent()
                    }
                    ktlintCliErrors.add(e.toKtlintCliError(code))
                } else {
                    // When reading from stdin, it is first assumed that the provided code is pure Kotlin instead of Kotlin script. If
                    // parsing fails, retry parsing at Kotlin script.
                    logger.warn {
                        """
                        Can not parse input from <stdin> as Kotlin, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        Now, trying to read the input as Kotlin Script.
                        """.trimIndent()
                    }
                    return format(
                        ktLintRuleEngine = ktLintRuleEngine,
                        code = Code.fromSnippet(code.content, script = true),
                        baselineLintErrors = baselineLintErrors,
                    )
                }
            } else {
                ktlintCliErrors.add(e.toKtlintCliError(code))
                tripped.set(true)
                code.content // making sure `cat file | ktlint --stdin > file` is (relatively) safe
            }
        }
        return ktlintCliErrors.toList()
    }

    private fun lint(
        ktLintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineLintErrors: List<KtlintCliError>,
    ): List<KtlintCliError> {
        val ktlintCliErrors = mutableListOf<KtlintCliError>()
        try {
            ktLintRuleEngine.lint(code) { lintError ->
                val ktlintCliError =
                    KtlintCliError(
                        line = lintError.line,
                        col = lintError.col,
                        ruleId = lintError.ruleId.value,
                        detail = lintError.detail,
                        status =
                            if (lintError.canBeAutoCorrected) {
                                LINT_CAN_BE_AUTOCORRECTED
                            } else {
                                LINT_CAN_NOT_BE_AUTOCORRECTED
                            },
                    )
                if (baselineLintErrors.doesNotContain(ktlintCliError)) {
                    ktlintCliErrors.add(ktlintCliError)
                    tripped.set(true)
                }
            }
        } catch (e: Exception) {
            if (code.isStdIn && e is KtLintParseException) {
                if (code.script) {
                    // When reading from stdin, code is only parsed as Kotlint script, if it could not be parsed as pure Kotlin. Now parsing
                    // of the code has failed for both, the file has to be ignored.
                    logger.error {
                        """
                        Can not parse input from <stdin> as Kotlin, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        """.trimIndent()
                    }
                    ktlintCliErrors.add(e.toKtlintCliError(code))
                } else {
                    // When reading from stdin, it is first assumed that the provided code is pure Kotlin instead of Kotlin script. If
                    // parsing fails, retry parsing at Kotlin script.
                    logger.warn {
                        """
                        Can not parse input from <stdin> as Kotlin, due to error below:
                            ${e.toKtlintCliError(code).detail}
                        Now, trying to read the input as Kotlin Script.
                        """.trimIndent()
                    }
                    return lint(
                        ktLintRuleEngine = ktLintRuleEngine,
                        code = Code.fromSnippet(code.content, script = true),
                        baselineLintErrors = baselineLintErrors,
                    )
                }
            } else {
                ktlintCliErrors.add(e.toKtlintCliError(code))
                tripped.set(true)
            }
        }
        return ktlintCliErrors.toList()
    }

    private fun Exception.toKtlintCliError(code: Code): KtlintCliError =
        this.let { e ->
            when (e) {
                is KtLintParseException ->
                    KtlintCliError(
                        line = e.line,
                        col = e.col,
                        ruleId = "",
                        detail = "Not a valid Kotlin file (${e.message?.lowercase(Locale.getDefault())})",
                        status = KOTLIN_PARSE_EXCEPTION,
                    )
                is KtLintRuleException -> {
                    logger.debug(e) { "Internal Error (${e.ruleId}) in ${code.fileNameOrStdin()} at position '${e.line}:${e.col}" }
                    KtlintCliError(
                        line = e.line,
                        col = e.col,
                        ruleId = "",
                        detail =
                            "Internal Error (rule '${e.ruleId}') in ${code.fileNameOrStdin()} at position '${e.line}:${e.col}. Please " +
                                "create a ticket at https://github.com/pinterest/ktlint/issues and provide the source code that " +
                                "triggered an error.\n" +
                                e.stackTraceToString(),
                        status = KTLINT_RULE_ENGINE_EXCEPTION,
                    )
                }
                else -> throw e
            }
        }

    private fun readPatternsFromStdin(delimiter: String): Set<String> {
        require(delimiter.isNotEmpty())

        return String(System.`in`.readBytes())
            .split(delimiter)
            .let { patterns: List<String> ->
                patterns.filterTo(LinkedHashSet(patterns.size), String::isNotEmpty)
            }
    }

    /**
     * Executes "Callable"s in parallel (lazily).
     * The results are gathered one-by-one (by `cb(<callable result>)`) in the order of corresponding "Callable"s
     * in the "Sequence" (think `seq.toList().map { executorService.submit(it) }.forEach { cb(it.get()) }` but without
     * buffering an entire sequence).
     *
     * Once kotlinx-coroutines are out of "experimental" stage everything below can be replaced with
     * ```
     * suspend fun <T> Sequence<Callable<T>>.parallel(...) {
     *     val ctx = newFixedThreadPoolContext(numberOfThreads, "Sequence<Callable<T>>.parallel")
     *     ctx.use {
     *         val channel = produce(ctx, numberOfThreads) {
     *             for (task in this@parallel) {
     *                 send(async(ctx) { task.call() })
     *             }
     *         }
     *         for (res in channel) {
     *             cb(res.await())
     *         }
     *     }
     * }
     * ```
     */
    private fun <T> Sequence<Callable<T>>.parallel(
        cb: (T) -> Unit,
        numberOfThreads: Int = Runtime.getRuntime().availableProcessors(),
    ) {
        val pill =
            object : Future<T> {
                override fun isDone(): Boolean = throw UnsupportedOperationException()

                override fun get(
                    timeout: Long,
                    unit: TimeUnit,
                ): T = throw UnsupportedOperationException()

                override fun get(): T = throw UnsupportedOperationException()

                override fun cancel(mayInterruptIfRunning: Boolean): Boolean = throw UnsupportedOperationException()

                override fun isCancelled(): Boolean = throw UnsupportedOperationException()
            }
        val q = ArrayBlockingQueue<Future<T>>(numberOfThreads)
        val producer =
            thread(start = true) {
                val executorService = Executors.newCachedThreadPool()
                try {
                    for (task in this) {
                        q.put(executorService.submit(task))
                    }
                    q.put(pill)
                } catch (e: InterruptedException) {
                    // we've been asked to stop consuming sequence
                } finally {
                    executorService.shutdown()
                }
            }
        try {
            while (true) {
                val result = q.take()
                if (result != pill) cb(result.get()) else break
            }
        } finally {
            producer.interrupt() // in case q.take()/result.get() throws
            producer.join()
        }
    }
}

private class CodeStyleValueConverter : CommandLine.ITypeConverter<CodeStyleValue> {
    @Throws(Exception::class)
    override fun convert(value: String?): CodeStyleValue =
        when (value?.lowercase()?.replace("-", "_")) {
            null -> CODE_STYLE_PROPERTY.defaultValue
            "ktlint_official" -> CodeStyleValue.ktlint_official
            "android_studio" -> CodeStyleValue.android_studio
            "intellij_idea" -> CodeStyleValue.intellij_idea
            else -> throw IllegalArgumentException("Invalid code style value")
        }
}

private class LogLevelConverter : CommandLine.ITypeConverter<Level> {
    @Throws(Exception::class)
    override fun convert(value: String?): Level =
        when (value?.uppercase()) {
            "TRACE" -> Level.TRACE
            "DEBUG" -> Level.DEBUG
            "INFO" -> Level.INFO
            "WARN" -> Level.WARN
            "ERROR" -> Level.ERROR
            "NONE" -> Level.OFF
            else -> throw IllegalStateException("Invalid log level '$value'")
        }
}

// Do not convert to "val" as the function depends on PicoCli options which are not fully instantiated until the "run" method is started
internal fun List<String>.toFilesURIList() =
    map {
        val jarFile = File(it.expandTildeToFullPath())
        if (!jarFile.exists()) {
            logger.error { "File '$it' does not exist" }
            exitKtLintProcess(1)
        }
        jarFile.toURI().toURL()
    }

/**
 * Wrapper around exitProcess which ensure that a proper log line is written which can be used in unit tests for
 * validating the result of the test.
 */
internal fun exitKtLintProcess(status: Int): Nothing {
    logger.debug { "Exit ktlint with exit code: $status" }
    exitProcess(status)
}
