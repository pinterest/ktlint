@file:JvmName("Main")
package com.pinterest.ktlint

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.ParseException
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.core.RuleExecutionException
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.internal.GitPreCommitHookSubCommand
import com.pinterest.ktlint.internal.GitPrePushHookSubCommand
import com.pinterest.ktlint.internal.IntellijIDEAIntegration
import com.pinterest.ktlint.internal.KtlintVersionProvider
import com.pinterest.ktlint.internal.MavenDependencyResolver
import com.pinterest.ktlint.internal.PrintASTSubCommand
import com.pinterest.ktlint.internal.expandTilde
import com.pinterest.ktlint.internal.fileSequence
import com.pinterest.ktlint.internal.formatFile
import com.pinterest.ktlint.internal.lintFile
import com.pinterest.ktlint.internal.location
import com.pinterest.ktlint.internal.printHelpOrVersionUsage
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.net.URLDecoder
import java.nio.file.Paths
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.NoSuchElementException
import java.util.Scanner
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
import org.eclipse.aether.RepositoryException
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
import org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

fun main(args: Array<String>) {
    val ktlintCommand = KtlintCommandLine()
    val commandLine = CommandLine(ktlintCommand)
        .addSubcommand(GitPreCommitHookSubCommand.COMMAND_NAME, GitPreCommitHookSubCommand())
        .addSubcommand(GitPrePushHookSubCommand.COMMAND_NAME, GitPrePushHookSubCommand())
        .addSubcommand(PrintASTSubCommand.COMMAND_NAME, PrintASTSubCommand())
    val parseResult = commandLine.parseArgs(*args)

    commandLine.printHelpOrVersionUsage()

    if (parseResult.hasSubcommand()) {
        handleSubCommand(commandLine, parseResult)
    } else {
        ktlintCommand.run()
    }
}

fun handleSubCommand(
    commandLine: CommandLine,
    parseResult: CommandLine.ParseResult
) {
    when (val subCommand = parseResult.subcommand().commandSpec().userObject()) {
        is GitPreCommitHookSubCommand -> subCommand.run()
        is GitPrePushHookSubCommand -> subCommand.run()
        is PrintASTSubCommand -> subCommand.run()
        else -> commandLine.usage(System.out, CommandLine.Help.Ansi.OFF)
    }
}

@Command(
    headerHeading =
"""An anti-bikeshedding Kotlin linter with built-in formatter
(https://github.com/pinterest/ktlint).

Usage:
  ktlint <flags> [patterns]
  java -jar ktlint <flags> [patterns]

Examples:
  # check the style of all Kotlin files inside the current dir (recursively)
  # (hidden folders will be skipped)
  ktlint

  # check only certain locations (prepend ! to negate the pattern)
  ktlint "src/**/*.kt" "!src/**/*Test.kt"

  # auto-correct style violations
  ktlint -F "src/**/*.kt"

  # custom reporter
  ktlint --reporter=plain?group_by_file
  # multiple reporters can be specified like this
  ktlint --reporter=plain \
    --reporter=checkstyle,output=ktlint-checkstyle-report.xml
  # 3rd-party reporter
  ktlint --reporter=html,artifact=com.github.user:repo:master-SNAPSHOT

Flags:""",
    synopsisHeading = "",
    customSynopsis = [""],
    sortOptions = false,
    mixinStandardHelpOptions = true,
    versionProvider = KtlintVersionProvider::class
)
class KtlintCommandLine {

    @Option(
        names = ["--android", "-a"],
        description = ["Turn on Android Kotlin Style Guide compatibility"]
    )
    var android: Boolean = false

    // todo: make it a command in 1.0.0 (it's too late now as we might interfere with valid "lint" patterns)
    @Option(
        names = ["--apply-to-idea"],
        description = ["Update Intellij IDEA settings (global)"]
    )
    private var apply: Boolean = false

    // todo: make it a command in 1.0.0 (it's too late now as we might interfere with valid "lint" patterns)
    @Option(
        names = ["--apply-to-idea-project"],
        description = ["Update Intellij IDEA project settings"]
    )
    private var applyToProject: Boolean = false

    @Option(
        names = ["--color"],
        description = ["Make output colorful"]
    )
    var color: Boolean = false

    @Option(
        names = ["--debug"],
        description = ["Turn on debug output"]
    )
    var debug: Boolean = false

    @Option(
        names = ["--disabled_rules"],
        description = ["Comma-separated list of rules to globally disable"]
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
            "A reporter to use (built-in: plain (default), plain?group_by_file, json, checkstyle). " +
                "To use a third-party reporter specify a path to a JAR file on the filesystem."
        ]
    )
    private var reporters = ArrayList<String>()

    @Deprecated("See https://github.com/pinterest/ktlint/issues/451")
    @Option(
        names = ["--repository"],
        description = [
            "An additional Maven repository (Maven Central/JCenter/JitPack are active by default) " +
                "(value format: <id>=<url>)"
        ],
        hidden = true
    )
    private var repositories = ArrayList<String>()

    @Option(
        names = ["--repository-update", "-U"],
        description = ["Check remote repositories for updated snapshots"]
    )
    private var forceUpdate: Boolean? = null

    @Option(
        names = ["--ruleset", "-R"],
        description = ["A path to a JAR file containing additional ruleset(s)"]
    )
    private var rulesets = ArrayList<String>()

    @Option(
        names = ["--skip-classpath-check"],
        description = ["Do not check classpath for potential conflicts"]
    )
    private var skipClasspathCheck: Boolean = false

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
        names = ["-y"],
        hidden = true
    )
    private var forceApply: Boolean = false

    @Option(
        names = ["--editorconfig"],
        description = ["Path to .editorconfig"]
    )
    private var editorConfigPath: String? = null

    @Option(
        names = ["--experimental"],
        description = ["Enabled experimental rules (ktlint-ruleset-experimental)"]
    )
    private var experimental: Boolean = false

    @Parameters(hidden = true)
    private var patterns = ArrayList<String>()

    fun run() {
        if (apply || applyToProject) {
            applyToIDEA()
            exitProcess(0)
        }
        val start = System.currentTimeMillis()

        // load 3rd party ruleset(s) (if any)
        val dependencyResolver = lazy(LazyThreadSafetyMode.NONE) { buildDependencyResolver() }
        if (!rulesets.isEmpty()) {
            loadJARs(dependencyResolver, rulesets)
        }

        // Detect custom rulesets that have not been moved to the new package
        if (ServiceLoader.load(com.github.shyiko.ktlint.core.RuleSetProvider::class.java).any()) {
            System.err.println("[ERROR] Cannot load custom ruleset!")
            System.err.println("[ERROR] RuleSetProvider has moved to com.pinterest.ktlint.core.")
            System.err.println("[ERROR] Please rename META-INF/services/com.github.shyiko.ktlint.core.RuleSetProvider to META-INF/services/com.pinterest.ktlint.core.RuleSetProvider")
            exitProcess(1)
        }

        // standard should go first
        val ruleSetProviders =
            ServiceLoader.load(RuleSetProvider::class.java)
                .map { it.get().id to it }
                .filter { (id) -> experimental || id != "experimental" }
                .sortedBy { if (it.first == "standard") "\u0000${it.first}" else it.first }
        if (debug) {
            ruleSetProviders.forEach { System.err.println("[DEBUG] Discovered ruleset \"${it.first}\"") }
        }
        val tripped = AtomicBoolean()
        val reporter = loadReporter(dependencyResolver) { tripped.get() }
        data class LintErrorWithCorrectionInfo(val err: LintError, val corrected: Boolean)
        val userData = listOfNotNull(
            "android" to android.toString(),
            if (disabledRules.isNotBlank()) "disabled_rules" to disabledRules else null
        ).toMap()

        fun process(fileName: String, fileContent: String): List<LintErrorWithCorrectionInfo> {
            if (debug) {
                val fileLocation = if (fileName != KtLint.STDIN_FILE) File(fileName).location(relative) else fileName
                System.err.println("[DEBUG] Checking $fileLocation")
            }
            val result = ArrayList<LintErrorWithCorrectionInfo>()
            if (format) {
                val formattedFileContent = try {
                    formatFile(
                        fileName,
                        fileContent,
                        ruleSetProviders.map { it.second.get() },
                        userData,
                        editorConfigPath,
                        debug
                    ) { err, corrected ->
                        if (!corrected) {
                            result.add(LintErrorWithCorrectionInfo(err, corrected))
                            tripped.set(true)
                        }
                    }
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(e.toLintError(), false))
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
                        ruleSetProviders.map { it.second.get() },
                        userData,
                        editorConfigPath,
                        debug
                    ) { err ->
                        result.add(LintErrorWithCorrectionInfo(err, false))
                        tripped.set(true)
                    }
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(e.toLintError(), false))
                    tripped.set(true)
                }
            }
            return result
        }
        val (fileNumber, errorNumber) = Pair(AtomicInteger(), AtomicInteger())
        fun report(fileName: String, errList: List<LintErrorWithCorrectionInfo>) {
            fileNumber.incrementAndGet()
            val errListLimit = minOf(errList.size, maxOf(limit - errorNumber.get(), 0))
            errorNumber.addAndGet(errListLimit)
            reporter.before(fileName)
            errList.head(errListLimit).forEach { (err, corrected) ->
                reporter.onLintError(
                    fileName,
                    if (!err.canBeAutoCorrected) err.copy(detail = err.detail + " (cannot be auto-corrected)") else err,
                    corrected
                )
            }
            reporter.after(fileName)
        }
        reporter.beforeAll()
        if (stdin) {
            report(KtLint.STDIN_FILE, process(KtLint.STDIN_FILE, String(System.`in`.readBytes())))
        } else {
            patterns.fileSequence()
                .takeWhile { errorNumber.get() < limit }
                .map { file -> Callable { file to process(file.path, file.readText()) } }
                .parallel({ (file, errList) -> report(file.location(relative), errList) })
        }
        reporter.afterAll()
        if (debug) {
            System.err.println(
                "[DEBUG] ${
                System.currentTimeMillis() - start
                }ms / $fileNumber file(s) / $errorNumber error(s)"
            )
        }
        if (tripped.get()) {
            exitProcess(1)
        }
    }

    private fun loadReporter(dependencyResolver: Lazy<MavenDependencyResolver>, tripped: () -> Boolean): Reporter {
        data class ReporterTemplate(val id: String, val artifact: String?, val config: Map<String, String>, var output: String?)
        val tpls = (if (reporters.isEmpty()) listOf("plain") else reporters)
            .map { reporter ->
                val split = reporter.split(",")
                val (reporterId, rawReporterConfig) = split[0].split("?", limit = 2) + listOf("")
                ReporterTemplate(
                    reporterId,
                    split.lastOrNull { it.startsWith("artifact=") }?.let { it.split("=")[1] },
                    mapOf("verbose" to verbose.toString(), "color" to color.toString()) + parseQuery(rawReporterConfig),
                    split.lastOrNull { it.startsWith("output=") }?.let { it.split("=")[1] }
                )
            }
            .distinct()
        val reporterLoader = ServiceLoader.load(ReporterProvider::class.java)
        val reporterProviderById = reporterLoader.associate { it.id to it }.let { map ->
            val missingReporters = tpls.filter { !map.containsKey(it.id) }.mapNotNull { it.artifact }.distinct()
            if (!missingReporters.isEmpty()) {
                loadJARs(dependencyResolver, missingReporters)
                reporterLoader.reload()
                reporterLoader.associate { it.id to it }
            } else map
        }
        if (debug) {
            reporterProviderById.forEach { (id) -> System.err.println("[DEBUG] Discovered reporter \"$id\"") }
        }
        fun ReporterTemplate.toReporter(): Reporter {
            val reporterProvider = reporterProviderById[id]
            if (reporterProvider == null) {
                System.err.println(
                    "Error: reporter \"$id\" wasn't found (available: ${
                    reporterProviderById.keys.sorted().joinToString(",")
                    })"
                )
                exitProcess(1)
            }
            if (debug) {
                System.err.println(
                    "[DEBUG] Initializing \"$id\" reporter with $config" +
                        (output?.let { ", output=$it" } ?: "")
                )
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
                                if (tripped()) {
                                    val outputLocation = File(output).absoluteFile.location(relative)
                                    System.err.println("\"$id\" report written to $outputLocation")
                                }
                            }
                        }
                    } else {
                        reporter
                    }
                }
        }
        return Reporter.from(*tpls.map { it.toReporter() }.toTypedArray())
    }

    private fun Exception.toLintError(): LintError = this.let { e ->
        when (e) {
            is ParseException ->
                LintError(
                    e.line, e.col, "",
                    "Not a valid Kotlin file (${e.message?.toLowerCase()})"
                )
            is RuleExecutionException -> {
                if (debug) {
                    System.err.println("[DEBUG] Internal Error (${e.ruleId})")
                    e.printStackTrace(System.err)
                }
                LintError(
                    e.line, e.col, "",
                    "Internal Error (${e.ruleId}). " +
                        "Please create a ticket at https://github.com/pinterest/ktlint/issue " +
                        "(if possible, provide the source code that triggered an error)"
                )
            }
            else -> throw e
        }
    }

    private fun applyToIDEA() {
        try {
            val workDir = Paths.get(".")
            if (!forceApply) {
                val fileList = IntellijIDEAIntegration.apply(workDir, true, android, applyToProject)
                System.err.println(
                    "The following files are going to be updated:\n\n\t" +
                        fileList.joinToString("\n\t") +
                        "\n\nDo you wish to proceed? [y/n]\n" +
                        "(in future, use -y flag if you wish to skip confirmation)"
                )
                val scanner = Scanner(System.`in`)
                val res =
                    generateSequence {
                        try { scanner.next() } catch (e: NoSuchElementException) { null }
                    }
                        .filter { line -> !line.trim().isEmpty() }
                        .first()
                if (!"y".equals(res, ignoreCase = true)) {
                    System.err.println("(update canceled)")
                    exitProcess(1)
                }
            }
            IntellijIDEAIntegration.apply(workDir, false, android, applyToProject)
        } catch (e: IntellijIDEAIntegration.ProjectNotFoundException) {
            System.err.println(
                ".idea directory not found. " +
                    "Are you sure you are inside project root directory?"
            )
            exitProcess(1)
        }
        System.err.println("(updated)")
        System.err.println("\nPlease restart your IDE")
        System.err.println("(if you experience any issues please report them at https://github.com/pinterest/ktlint)")
    }

    private fun <T> List<T>.head(limit: Int) = if (limit == size) this else this.subList(0, limit)

    private fun buildDependencyResolver(): MavenDependencyResolver {
        val mavenLocal = File(File(System.getProperty("user.home"), ".m2"), "repository")
        mavenLocal.mkdirsOrFail()
        val dependencyResolver = MavenDependencyResolver(
            mavenLocal,
            listOf(
                RemoteRepository.Builder(
                    "central", "default", "https://repo1.maven.org/maven2/"
                ).setSnapshotPolicy(
                    RepositoryPolicy(
                        false, UPDATE_POLICY_NEVER,
                        CHECKSUM_POLICY_IGNORE
                    )
                ).build(),
                RemoteRepository.Builder(
                    "bintray", "default", "https://jcenter.bintray.com"
                ).setSnapshotPolicy(
                    RepositoryPolicy(
                        false, UPDATE_POLICY_NEVER,
                        CHECKSUM_POLICY_IGNORE
                    )
                ).build(),
                RemoteRepository.Builder(
                    "jitpack", "default", "https://jitpack.io"
                ).build()
            ) + repositories.map { repository ->
                val colon = repository.indexOf("=").apply {
                    if (this == -1) {
                        throw RuntimeException(
                            "$repository is not a valid repository entry " +
                                "(make sure it's provided as <id>=<url>"
                        )
                    }
                }
                val id = repository.substring(0, colon)
                val url = repository.substring(colon + 1)
                RemoteRepository.Builder(id, "default", url).build()
            },
            forceUpdate == true
        )
        if (debug) {
            dependencyResolver.setTransferEventListener { e ->
                System.err.println(
                    "[DEBUG] Transfer ${e.type.toString().toLowerCase()} ${e.resource.repositoryUrl}" +
                        e.resource.resourceName + (e.exception?.let { " (${it.message})" } ?: "")
                )
            }
        }
        return dependencyResolver
    }

    // fixme: isn't going to work on JDK 9
    private fun loadJARs(dependencyResolver: Lazy<MavenDependencyResolver>, artifacts: List<String>) {
        (ClassLoader.getSystemClassLoader() as java.net.URLClassLoader)
            .addURLs(
                artifacts.flatMap { artifact ->
                    if (debug) {
                        System.err.println("[DEBUG] Resolving $artifact")
                    }
                    val result = try {
                        dependencyResolver.value.resolve(DefaultArtifact(artifact)).map { it.toURI().toURL() }
                    } catch (e: IllegalArgumentException) {
                        val file = File(expandTilde(artifact))
                        if (!file.exists()) {
                            System.err.println("Error: $artifact does not exist")
                            exitProcess(1)
                        }
                        listOf(file.toURI().toURL())
                    } catch (e: RepositoryException) {
                        if (debug) {
                            e.printStackTrace()
                        }
                        System.err.println("Error: $artifact wasn't found")
                        exitProcess(1)
                    }
                    if (debug) {
                        result.forEach { url -> System.err.println("[DEBUG] Loading $url") }
                    }
                    if (!skipClasspathCheck) {
                        if (result.any { it.toString().substringAfterLast("/").startsWith("ktlint-core-") }) {
                            System.err.println(
                                "\"$artifact\" appears to have a runtime/compile dependency on \"ktlint-core\".\n" +
                                    "Please inform the author that \"com.pinterest:ktlint*\" should be marked " +
                                    "compileOnly (Gradle) / provided (Maven).\n" +
                                    "(to suppress this warning use --skip-classpath-check)"
                            )
                        }
                        if (result.any { it.toString().substringAfterLast("/").startsWith("kotlin-stdlib-") }) {
                            System.err.println(
                                "\"$artifact\" appears to have a runtime/compile dependency on \"kotlin-stdlib\".\n" +
                                    "Please inform the author that \"org.jetbrains.kotlin:kotlin-stdlib*\" should be marked " +
                                    "compileOnly (Gradle) / provided (Maven).\n" +
                                    "(to suppress this warning use --skip-classpath-check)"
                            )
                        }
                    }
                    result
                }
            )
    }

    private fun parseQuery(query: String) =
        query.split("&")
            .fold(LinkedHashMap<String, String>()) { map, s ->
                if (!s.isEmpty()) {
                    s.split("=", limit = 2).let { e ->
                        map.put(
                            e[0],
                            URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8")
                        )
                    }
                }
                map
            }

    private fun java.net.URLClassLoader.addURLs(url: Iterable<java.net.URL>) {
        val method = java.net.URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
        method.isAccessible = true
        url.forEach { method.invoke(this, it) }
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
            override fun isDone(): Boolean { throw UnsupportedOperationException() }
            override fun get(timeout: Long, unit: TimeUnit): T { throw UnsupportedOperationException() }
            override fun get(): T { throw UnsupportedOperationException() }
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean { throw UnsupportedOperationException() }
            override fun isCancelled(): Boolean { throw UnsupportedOperationException() }
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
}
