package com.pinterest.ktlint.internal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.Baseline.Status.INVALID
import com.pinterest.ktlint.core.api.Baseline.Status.NOT_FOUND
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.core.api.KtLintParseException
import com.pinterest.ktlint.core.api.KtLintRuleException
import com.pinterest.ktlint.core.api.doesNotContain
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty
import com.pinterest.ktlint.core.api.loadBaseline
import com.pinterest.ktlint.core.api.relativeRoute
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.setDefaultLoggerModifier
import com.pinterest.ktlint.reporter.plain.Color
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.util.Locale
import java.util.ServiceLoader
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import mu.KLogger
import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.Parameters

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
        names = ["--android", "-a"],
        description = ["Turn on Android Kotlin Style Guide compatibility"],
    )
    var android: Boolean = false

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
        names = ["--debug"],
        description = ["Turn on debug output. Deprecated, use '--log-level=debug' instead."],
    )
    @Deprecated(message = "Replaced with minLogLevel")
    var debugOld: Boolean? = null

    @Option(
        names = ["--trace"],
        description = ["Turn on trace output. Deprecated, use '--log-level=trace' instead."],
    )
    @Deprecated(message = "Replaced with minLogLevel")
    var trace: Boolean? = null

    @Option(
        names = ["--disabled_rules"],
        description = [
            "Comma-separated list of rules to globally disable." +
                " To disable standard ktlint rule-set use --disabled_rules=standard",
        ],
    )
    var disabledRules: String = ""

    // TODO: this should have been a command, not a flag (consider changing in 1.0.0)
    @Option(
        names = ["--format", "-F"],
        description = ["Fix any deviations from the code style"],
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
    private var reporterJarPaths: List<String> = ArrayList()

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
        names = ["--verbose", "-v"],
        description = ["Show error codes. Deprecated, use '--log-level=info' instead."],
    )
    @Deprecated(message = "Replaced with minLogLevel")
    private var verbose: Boolean? = null

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
        description = ["Enabled experimental rules (ktlint-ruleset-experimental)"],
    )
    var experimental: Boolean = false

    @Option(
        names = ["--baseline"],
        description = ["Defines a baseline file to check against"],
    )
    private var baselinePath: String = ""

    @Parameters(hidden = true)
    private var patterns = ArrayList<String>()

    @Option(
        names = ["--log-level", "-l"],
        description = ["Defines the minimum log level (trace, debug, info, warn, error) or none to suppress all logging"],
        converter = [LogLevelConverter::class],
    )
    private var minLogLevel: Level = Level.INFO

    private val tripped = AtomicBoolean()
    private val fileNumber = AtomicInteger()
    private val errorNumber = AtomicInteger()

    internal var debug: Boolean = false
        get() = Level.DEBUG.isGreaterOrEqual(minLogLevel)
        private set

    private val editorConfigDefaults: EditorConfigDefaults
        get() = EditorConfigDefaults.load(
            editorConfigPath
                ?.expandTildeToFullPath()
                ?.let { path -> Paths.get(path) },
        )

    private fun disabledRulesEditorConfigOverrides() =
        disabledRules
            .split(",")
            .filter { it.isNotBlank() }
            .map { ruleId -> createRuleExecutionEditorConfigProperty(ruleId) to RuleExecution.disabled }
            .toTypedArray()

    fun run() {
        if (debugOld != null || trace != null || verbose != null) {
            if (minLogLevel == Level.OFF) {
                minLogLevel = Level.ERROR
            }
            logger.error {
                "Options '--debug', '--trace', '--verbose' and '-v' are deprecated and replaced with option '--log-level=<level>' or '-l=<level>'."
            }
            exitKtLintProcess(1)
        }

        val editorConfigOverride = EditorConfigOverride
            .EMPTY_EDITOR_CONFIG_OVERRIDE
            .applyIf(experimental) {
                logger.debug { "Add editor config override to allow the experimental rule set" }
                plus(createRuleSetExecutionEditorConfigProperty("experimental:all") to RuleExecution.enabled)
            }.applyIf(disabledRules.isNotBlank()) {
                logger.debug { "Add editor config override to disable rules: '$disabledRules'" }
                plus(*disabledRulesEditorConfigOverrides())
            }.applyIf(android) {
                logger.debug { "Add editor config override to set code style to 'android'" }
                plus(CODE_STYLE_PROPERTY to CodeStyleValue.android)
            }.applyIf(stdin) {
                logger.debug { "Add editor config override to disable 'filename' rule which can not be used in combination with reading from <stdin>" }
                plus(createRuleExecutionEditorConfigProperty("standard:filename") to RuleExecution.disabled)
            }

        assertStdinAndPatternsFromStdinOptionsMutuallyExclusive()

        val stdinPatterns: Set<String> = readPatternsFromStdin()
        patterns.addAll(stdinPatterns)

        // Set default value to patterns only after the logger has been configured to avoid a warning about initializing
        // the logger multiple times
        if (patterns.isEmpty()) {
            logger.info { "Enable default patterns $DEFAULT_PATTERNS" }
            patterns = ArrayList(DEFAULT_PATTERNS)
        }

        val start = System.currentTimeMillis()

        var reporter = loadReporter()

        val ktLintRuleEngine = KtLintRuleEngine(
            ruleProviders = ruleProviders(),
            editorConfigDefaults = editorConfigDefaults,
            editorConfigOverride = editorConfigOverride,
            isInvokedFromCli = true,
        )

        reporter.beforeAll()
        if (stdin) {
            lintStdin(
                ktLintRuleEngine,
                reporter,
            )
        } else {
            val baselineLintErrorsPerFile =
                if (baselinePath == "") {
                    emptyMap()
                } else {
                    loadBaseline(baselinePath)
                        .also { baseline ->
                            if (baseline.status == INVALID || baseline.status == NOT_FOUND) {
                                val baselineReporter = ReporterTemplate("baseline", null, emptyMap(), baselinePath)
                                val reporterProviderById = loadReporters(emptyList())
                                reporter = Reporter.from(reporter, baselineReporter.toReporter(reporterProviderById))
                            }
                        }.lintErrorsPerFile
                }
            lintFiles(
                ktLintRuleEngine,
                baselineLintErrorsPerFile,
                reporter,
            )
        }
        reporter.afterAll()

        logger.debug { "Finished processing in ${System.currentTimeMillis() - start}ms / $fileNumber file(s) scanned / $errorNumber error(s) found" }
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

    // Do not convert to "val" as the function depends on PicoCli options which are not fully instantiated until the "run" method is started
    internal fun ruleProviders(): Set<RuleProvider> =
        rulesetJarPaths
            .toFilesURIList()
            .loadRuleProviders(debug)

    // Do not convert to "val" as the function depends on PicoCli options which are not fully instantiated until the "run" method is started
    private fun List<String>.toFilesURIList() =
        map {
            val jarFile = File(it.expandTildeToFullPath())
            if (!jarFile.exists()) {
                logger.error { "File '$it' does not exist" }
                exitKtLintProcess(1)
            }
            jarFile.toURI().toURL()
        }

    // Do not convert to "val" as the function depends on PicoCli options which are not fully instantiated until the "run" method is started
    internal fun configureLogger() {
        logger = KotlinLogging
            .logger {}
            .setDefaultLoggerModifier { logger ->
                (logger.underlyingLogger as Logger).level = minLogLevel
            }
            .initKtLintKLogger()
    }

    private fun assertStdinAndPatternsFromStdinOptionsMutuallyExclusive() {
        if (stdin && stdinDelimiter != null) {
            throw ParameterException(
                commandSpec.commandLine(),
                "Options --stdin and --patterns-from-stdin mutually exclusive",
            )
        }
    }

    private fun lintFiles(
        ktLintRuleEngine: KtLintRuleEngine,
        lintErrorsPerFile: Map<String, List<LintError>>,
        reporter: Reporter,
    ) {
        FileSystems.getDefault()
            .fileSequence(patterns)
            .map { it.toFile() }
            .takeWhile { errorNumber.get() < limit }
            .map { file ->
                Callable {
                    file to process(
                        ktLintRuleEngine = ktLintRuleEngine,
                        code = file.readText(),
                        fileName = file.path,
                        baselineLintErrors = lintErrorsPerFile.getOrDefault(file.toPath().relativeRoute, emptyList()),
                    )
                }
            }.parallel({ (file, errList) -> report(file.location(relative), errList, reporter) })
    }

    private fun lintStdin(
        ktLintRuleEngine: KtLintRuleEngine,
        reporter: Reporter,
    ) {
        report(
            KtLintRuleEngine.STDIN_FILE,
            process(
                ktLintRuleEngine = ktLintRuleEngine,
                code = String(System.`in`.readBytes()),
                baselineLintErrors = emptyList(),
            ),
            reporter,
        )
    }

    private fun report(
        fileName: String,
        errList: List<LintErrorWithCorrectionInfo>,
        reporter: Reporter,
    ) {
        fileNumber.incrementAndGet()
        val errListLimit = minOf(errList.size, maxOf(limit - errorNumber.get(), 0))
        errorNumber.addAndGet(errListLimit)

        reporter.before(fileName)
        errList.take(errListLimit).forEach { (err, corrected) ->
            reporter.onLintError(
                fileName,
                if (!err.canBeAutoCorrected) err.copy(detail = err.detail + " (cannot be auto-corrected)") else err,
                corrected,
            )
        }
        reporter.after(fileName)
    }

    private fun process(
        ktLintRuleEngine: KtLintRuleEngine,
        code: String,
        fileName: String? = null,
        baselineLintErrors: List<LintError>,
    ): List<LintErrorWithCorrectionInfo> {
        if (fileName != null) {
            logger.trace { "Checking ${File(fileName).location(relative)}" }
        }
        val filePath = fileName?.let { Paths.get(it) }
        val result = ArrayList<LintErrorWithCorrectionInfo>()
        if (format) {
            val formattedFileContent = try {
                ktLintRuleEngine.format(code, filePath) { lintError, corrected ->
                    if (baselineLintErrors.doesNotContain(lintError)) {
                        result.add(LintErrorWithCorrectionInfo(lintError, corrected))
                        if (!corrected) {
                            tripped.set(true)
                        }
                    }
                }
            } catch (e: Exception) {
                result.add(LintErrorWithCorrectionInfo(e.toLintError(fileName), false))
                tripped.set(true)
                code // making sure `cat file | ktlint --stdin > file` is (relatively) safe
            }
            if (stdin) {
                print(formattedFileContent)
            } else {
                if (code !== formattedFileContent) {
                    File(fileName).writeText(formattedFileContent, charset("UTF-8"))
                }
            }
        } else {
            try {
                ktLintRuleEngine.lint(code, filePath) { lintError ->
                    if (baselineLintErrors.doesNotContain(lintError)) {
                        result.add(LintErrorWithCorrectionInfo(lintError, false))
                        tripped.set(true)
                    }
                }
            } catch (e: Exception) {
                result.add(LintErrorWithCorrectionInfo(e.toLintError(fileName), false))
                tripped.set(true)
            }
        }
        return result
    }

    private fun loadReporter(): Reporter {
        val configuredReporters = reporterJarPaths.ifEmpty { listOf("plain") }

        val tpls = configuredReporters
            .map { reporter ->
                val split = reporter.split(",")
                val (reporterId, rawReporterConfig) = split[0].split("?", limit = 2) + listOf("")
                ReporterTemplate(
                    reporterId,
                    split.lastOrNull { it.startsWith("artifact=") }?.let { it.split("=")[1] },
                    mapOf(
                        "color" to color.toString(),
                        "color_name" to colorName,
                        "format" to format.toString(),
                    ) + parseQuery(rawReporterConfig),
                    split.lastOrNull { it.startsWith("output=") }?.let { it.split("=")[1] },
                )
            }
            .distinct()
        val reporterProviderById = loadReporters(tpls.mapNotNull { it.artifact })
        return Reporter.from(*tpls.map { it.toReporter(reporterProviderById) }.toTypedArray())
    }

    private fun ReporterTemplate.toReporter(
        reporterProviderById: Map<String, ReporterProvider<*>>,
    ): Reporter {
        val reporterProvider = reporterProviderById[id]
        if (reporterProvider == null) {
            logger.error {
                reporterProviderById
                    .keys
                    .sorted()
                    .joinToString(
                        separator = ", ",
                        prefix = "reporter \"$id\" wasn't found (available: ",
                        postfix = ")",
                    )
            }
            exitKtLintProcess(1)
        }
        logger.debug {
            "Initializing \"$id\" reporter with $config" +
                (output?.let { ", output=$it" } ?: "")
        }
        val stream = if (output != null) {
            File(output).parentFile?.mkdirsOrFail(); PrintStream(output, "UTF-8")
        } else if (stdin) System.err else System.out
        return reporterProvider.get(stream, config)
            .let { reporter ->
                if (output != null) {
                    object : Reporter by reporter {
                        override fun afterAll() {
                            reporter.afterAll()
                            stream.close()
                            if (tripped.get()) {
                                val outputLocation = File(output).absoluteFile.location(relative)
                                logger.info {
                                    "\"$id\" report written to $outputLocation"
                                }
                            }
                        }
                    }
                } else {
                    reporter
                }
            }
    }

    private fun Exception.toLintError(filename: Any?): LintError = this.let { e ->
        when (e) {
            is KtLintParseException ->
                LintError(
                    e.line,
                    e.col,
                    "",
                    "Not a valid Kotlin file (${e.message?.lowercase(Locale.getDefault())})",
                )
            is KtLintRuleException -> {
                logger.debug("Internal Error (${e.ruleId}) in file '$filename' at position '${e.line}:${e.col}", e)
                LintError(
                    e.line,
                    e.col,
                    "",
                    "Internal Error (rule '${e.ruleId}') in file '$filename' at position '${e.line}:${e.col}. " +
                        "Please create a ticket at https://github.com/pinterest/ktlint/issues " +
                        "and provide the source code that triggered an error.\n" +
                        e.stackTraceToString(),
                )
            }
            else -> throw e
        }
    }

    private fun parseQuery(query: String) =
        query.split("&")
            .fold(LinkedHashMap<String, String>()) { map, s ->
                if (s.isNotEmpty()) {
                    s.split("=", limit = 2).let { e ->
                        map.put(
                            e[0],
                            URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8"),
                        )
                    }
                }
                map
            }

    private fun readPatternsFromStdin(): Set<String> {
        val delimiter: String = stdinDelimiter
            ?.ifEmpty { "\u0000" }
            ?: return emptySet()

        return String(System.`in`.readBytes())
            .split(delimiter)
            .let { patterns: List<String> ->
                patterns.filterTo(LinkedHashSet(patterns.size), String::isNotEmpty)
            }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !isDirectory) {
            throw IOException("Unable to create \"${this}\" directory")
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
        val pill = object : Future<T> {
            override fun isDone(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun get(timeout: Long, unit: TimeUnit): T {
                throw UnsupportedOperationException()
            }

            override fun get(): T {
                throw UnsupportedOperationException()
            }

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                throw UnsupportedOperationException()
            }

            override fun isCancelled(): Boolean {
                throw UnsupportedOperationException()
            }
        }
        val q = ArrayBlockingQueue<Future<T>>(numberOfThreads)
        val producer = thread(start = true) {
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

    private fun loadReporters(externalReportersJarPaths: List<String>) = ServiceLoader
        .load(
            ReporterProvider::class.java,
            URLClassLoader(externalReportersJarPaths.toFilesURIList().toTypedArray()),
        )
        .associateBy { it.id }
        .onEach { entry ->
            logger.debug { "Discovered reporter with \"${entry.key}\" id." }
        }

    private data class LintErrorWithCorrectionInfo(
        val err: LintError,
        val corrected: Boolean,
    )

    private data class ReporterTemplate(
        val id: String,
        val artifact: String?,
        val config: Map<String, String>,
        val output: String?,
    )
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

/**
 * Wrapper around exitProcess which ensure that a proper log line is written which can be used in unit tests for
 * validating the result of the test.
 */
internal fun exitKtLintProcess(status: Int): Nothing {
    logger.debug { "Exit ktlint with exit code: $status" }
    exitProcess(status)
}
