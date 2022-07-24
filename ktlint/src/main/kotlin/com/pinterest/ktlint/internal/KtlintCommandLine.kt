package com.pinterest.ktlint.internal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.ParseException
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.core.RuleExecutionException
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.codeStyleSetProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.disabledRulesProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.internal.containsLintError
import com.pinterest.ktlint.core.internal.loadBaseline
import com.pinterest.ktlint.core.internal.relativeRoute
import com.pinterest.ktlint.core.setDefaultLoggerModifier
import com.pinterest.ktlint.reporter.plain.internal.Color
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.file.FileSystems
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
import picocli.CommandLine.Command
import picocli.CommandLine.Option
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
    versionProvider = KtlintVersionProvider::class
)
internal class KtlintCommandLine {

    @Option(
        names = ["--android", "-a"],
        description = ["Turn on Android Kotlin Style Guide compatibility"]
    )
    var android: Boolean = false

    @Option(
        names = ["--color"],
        description = ["Make output colorful"]
    )
    var color: Boolean = false

    @Option(
        names = ["--color-name"],
        description = ["Customize the output color"]
    )
    var colorName: String = Color.DARK_GRAY.name

    @Option(
        names = ["--debug"],
        description = ["Turn on debug output"]
    )
    var debug: Boolean = false

    @Option(
        names = ["--trace"],
        description = ["Turn on trace output"]
    )
    var trace: Boolean = false

    @Option(
        names = ["--disabled_rules"],
        description = [
            "Comma-separated list of rules to globally disable." +
                " To disable standard ktlint rule-set use --disabled_rules=standard"
        ]
    )
    var disabledRules: String = ""

    // todo: this should have been a command, not a flag (consider changing in 1.0.0)
    @Option(
        names = ["--format", "-F"],
        description = ["Fix any deviations from the code style"]
    )
    private var format: Boolean = false

    @Option(
        names = ["--limit"],
        description = ["Maximum number of errors to show (default: show all)"]
    )
    private var limit: Int = -1
        get() = if (field < 0) Int.MAX_VALUE else field

    @Option(
        names = ["--relative"],
        description = [
            "Print files relative to the working directory " +
                "(e.g. dir/file.kt instead of /home/user/project/dir/file.kt)"
        ]
    )
    var relative: Boolean = false

    @Option(
        names = ["--reporter"],
        description = [
            "A reporter to use (built-in: plain (default), plain?group_by_file, json, sarif, checkstyle, html). " +
                "To use a third-party reporter specify a path to a JAR file on the filesystem via ',artifact=' option. " +
                "To override reporter output, use ',output=' option."
        ]
    )
    private var reporters: JarFiles = ArrayList()

    @Option(
        names = ["--ruleset", "-R"],
        description = ["A path to a JAR file containing additional ruleset(s)"]
    )
    var rulesetJarFiles: JarFiles = ArrayList()

    @Option(
        names = ["--stdin"],
        description = ["Read file from stdin"]
    )
    private var stdin: Boolean = false

    @Option(
        names = ["--verbose", "-v"],
        description = ["Show error codes"]
    )
    private var verbose: Boolean = false

    @Option(
        names = ["--editorconfig"],
        description = ["Path to .editorconfig"]
    )
    private var editorConfigPath: String? = null

    @Option(
        names = ["--experimental"],
        description = ["Enabled experimental rules (ktlint-ruleset-experimental)"]
    )
    var experimental: Boolean = false

    @Option(
        names = ["--baseline"],
        description = ["Defines a baseline file to check against"]
    )
    private var baseline: String = ""

    @Parameters(hidden = true)
    private var patterns = ArrayList<String>()

    private val tripped = AtomicBoolean()
    private val fileNumber = AtomicInteger()
    private val errorNumber = AtomicInteger()

    fun run() {
        if (verbose) {
            debug = true
        }
        logger = configureLogger()

        failOnOldRulesetProviderUsage()

        // Set default value to patterns only after the logger has been configured to avoid a warning about initializing
        // the logger multiple times
        if (patterns.isEmpty()) {
            logger.info { "Enable default patterns $defaultPatterns" }
            patterns = ArrayList(defaultPatterns)
        }

        val start = System.currentTimeMillis()

        val baselineResults = loadBaseline(baseline)
        val ruleProviders = rulesetJarFiles.loadRuleProviders(experimental, debug, disabledRules)
        var reporter = loadReporter()
        if (baselineResults.baselineGenerationNeeded) {
            val baselineReporter = ReporterTemplate("baseline", null, emptyMap(), baseline)
            val reporterProviderById = loadReporters(emptyList())
            reporter = Reporter.from(reporter, baselineReporter.toReporter(reporterProviderById))
        }
        val editorConfigOverride =
            EditorConfigOverride
                .emptyEditorConfigOverride
                .applyIf(disabledRules.isNotBlank()) {
                    plus(disabledRulesProperty to disabledRules)
                }.applyIf(android) {
                    plus(codeStyleSetProperty to android)
                }

        reporter.beforeAll()
        if (stdin) {
            lintStdin(ruleProviders, editorConfigOverride, reporter)
        } else {
            lintFiles(
                ruleProviders,
                editorConfigOverride,
                baselineResults.baselineRules,
                reporter
            )
        }
        reporter.afterAll()
        if (fileNumber.get() == 0) {
            logger.error { "No files matched $patterns" }
            exitProcess(1)
        }
        logger.debug { "${System.currentTimeMillis() - start}ms / $fileNumber file(s) / $errorNumber error(s)" }
        if (tripped.get()) {
            exitProcess(1)
        }
    }

    private fun configureLogger() =
        KotlinLogging
            .logger {}
            .setDefaultLoggerModifier { logger ->
                (logger.underlyingLogger as Logger).level = when {
                    trace -> Level.TRACE
                    debug -> Level.DEBUG
                    else -> Level.INFO
                }
            }
            .initKtLintKLogger()

    private fun lintFiles(
        ruleProviders: Set<RuleProvider>,
        editorConfigOverride: EditorConfigOverride,
        baseline: Map<String, List<LintError>>?,
        reporter: Reporter
    ) {
        FileSystems.getDefault()
            .fileSequence(patterns)
            .map { it.toFile() }
            .takeWhile { errorNumber.get() < limit }
            .map { file ->
                Callable {
                    file to process(
                        file.path,
                        file.readText(),
                        editorConfigOverride,
                        baseline?.get(file.relativeRoute),
                        ruleProviders
                    )
                }
            }.parallel({ (file, errList) -> report(file.location(relative), errList, reporter) })
    }

    private fun lintStdin(
        ruleProviders: Set<RuleProvider>,
        editorConfigOverride: EditorConfigOverride,
        reporter: Reporter
    ) {
        report(
            KtLint.STDIN_FILE,
            process(
                KtLint.STDIN_FILE,
                String(System.`in`.readBytes()),
                editorConfigOverride,
                null,
                ruleProviders
            ),
            reporter
        )
    }

    /**
     * Detect custom rulesets that have not been moved to the new package.
     */
    @Suppress("Deprecation")
    private fun failOnOldRulesetProviderUsage() {
        if (ServiceLoader.load(com.github.shyiko.ktlint.core.RuleSetProvider::class.java).any()) {
            logger.error {
                """
                Cannot load custom ruleset!")
                RuleSetProvider has moved to com.pinterest.ktlint.core.")
                Please rename META-INF/services/com.github.shyiko.ktlint.core.RuleSetProvider to META-INF/services/com.pinterest.ktlint.core.RuleSetProvider")
                """.trimIndent()
            }
            exitProcess(1)
        }
    }

    private fun report(
        fileName: String,
        errList: List<LintErrorWithCorrectionInfo>,
        reporter: Reporter
    ) {
        fileNumber.incrementAndGet()
        val errListLimit = minOf(errList.size, maxOf(limit - errorNumber.get(), 0))
        errorNumber.addAndGet(errListLimit)

        reporter.before(fileName)
        errList.take(errListLimit).forEach { (err, corrected) ->
            reporter.onLintError(
                fileName,
                if (!err.canBeAutoCorrected) err.copy(detail = err.detail + " (cannot be auto-corrected)") else err,
                corrected
            )
        }
        reporter.after(fileName)
    }

    private fun process(
        fileName: String,
        fileContent: String,
        editorConfigOverride: EditorConfigOverride,
        baselineErrors: List<LintError>?,
        ruleProviders: Set<RuleProvider>
    ): List<LintErrorWithCorrectionInfo> {
        logger.trace {
            val fileLocation = if (fileName != KtLint.STDIN_FILE) File(fileName).location(relative) else fileName
            "Checking $fileLocation"
        }
        val result = ArrayList<LintErrorWithCorrectionInfo>()
        if (format) {
            val formattedFileContent = try {
                formatFile(
                    fileName,
                    fileContent,
                    ruleProviders,
                    editorConfigOverride,
                    editorConfigPath,
                    debug
                ) { err, corrected ->
                    if (baselineErrors == null || !baselineErrors.containsLintError(err)) {
                        result.add(LintErrorWithCorrectionInfo(err, corrected))
                        if (!corrected) {
                            tripped.set(true)
                        }
                    }
                }
            } catch (e: Exception) {
                result.add(LintErrorWithCorrectionInfo(e.toLintError(fileName), false))
                tripped.set(true)
                fileContent // making sure `cat file | ktlint --stdint > file` is (relatively) safe
            }
            if (stdin) {
                print(formattedFileContent)
            } else {
                if (fileContent !== formattedFileContent) {
                    File(fileName).writeText(formattedFileContent, charset("UTF-8"))
                }
            }
        } else {
            try {
                lintFile(
                    fileName,
                    fileContent,
                    ruleProviders,
                    editorConfigOverride,
                    editorConfigPath,
                    debug
                ) { err ->
                    if (baselineErrors == null || !baselineErrors.containsLintError(err)) {
                        result.add(LintErrorWithCorrectionInfo(err, false))
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
        val configuredReporters = reporters.ifEmpty { listOf("plain") }

        val tpls = configuredReporters
            .map { reporter ->
                val split = reporter.split(",")
                val (reporterId, rawReporterConfig) = split[0].split("?", limit = 2) + listOf("")
                ReporterTemplate(
                    reporterId,
                    split.lastOrNull { it.startsWith("artifact=") }?.let { it.split("=")[1] },
                    mapOf(
                        "verbose" to verbose.toString(),
                        "color" to color.toString(),
                        "color_name" to colorName,
                        "format" to format.toString()
                    ) + parseQuery(rawReporterConfig),
                    split.lastOrNull { it.startsWith("output=") }?.let { it.split("=")[1] }
                )
            }
            .distinct()
        val reporterProviderById = loadReporters(tpls.mapNotNull { it.artifact })
        return Reporter.from(*tpls.map { it.toReporter(reporterProviderById) }.toTypedArray())
    }

    private fun ReporterTemplate.toReporter(
        reporterProviderById: Map<String, ReporterProvider<*>>
    ): Reporter {
        val reporterProvider = reporterProviderById[id]
        if (reporterProvider == null) {
            logger.error {
                "reporter \"$id\" wasn't found (available: ${
                reporterProviderById.keys.sorted().joinToString(",")
                })"
            }
            exitProcess(1)
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
            is ParseException ->
                LintError(
                    e.line,
                    e.col,
                    "",
                    "Not a valid Kotlin file (${e.message?.lowercase(Locale.getDefault())})"
                )
            is RuleExecutionException -> {
                logger.debug("Internal Error (${e.ruleId}) in file '$filename' at position '${e.line}:${e.col}", e)
                LintError(
                    e.line,
                    e.col,
                    "",
                    "Internal Error (${e.ruleId}) in file '$filename' at position '${e.line}:${e.col}. " +
                        "Please create a ticket at https://github.com/pinterest/ktlint/issues " +
                        "(if possible, please re-run with the --debug flag to get the stacktrace " +
                        "and provide the source code that triggered an error)"
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
                            URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8")
                        )
                    }
                }
                map
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
        numberOfThreads: Int = Runtime.getRuntime().availableProcessors()
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
            URLClassLoader(externalReportersJarPaths.toFilesURIList().toTypedArray())
        )
        .associateBy { it.id }
        .onEach { entry ->
            logger.debug { "Discovered reporter with \"${entry.key}\" id." }
        }

    private data class LintErrorWithCorrectionInfo(
        val err: LintError,
        val corrected: Boolean
    )

    private data class ReporterTemplate(
        val id: String,
        val artifact: String?,
        val config: Map<String, String>,
        val output: String?
    )
}
