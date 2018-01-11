package com.github.shyiko.ktlint

import com.github.shyiko.klob.Glob
import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.ParseException
import com.github.shyiko.ktlint.core.Reporter
import com.github.shyiko.ktlint.core.ReporterProvider
import com.github.shyiko.ktlint.core.RuleExecutionException
import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import com.github.shyiko.ktlint.internal.EditorConfig
import com.github.shyiko.ktlint.internal.IntellijIDEAIntegration
import com.github.shyiko.ktlint.internal.MavenDependencyResolver
import com.github.shyiko.ktlint.test.DumpAST
import org.eclipse.aether.RepositoryException
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
import org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import org.kohsuke.args4j.Argument
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.NamedOptionDef
import org.kohsuke.args4j.Option
import org.kohsuke.args4j.OptionHandlerFilter
import org.kohsuke.args4j.ParserProperties
import org.kohsuke.args4j.spi.OptionHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream
import java.io.PrintWriter
import java.math.BigInteger
import java.net.URLDecoder
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedHashMap
import java.util.NoSuchElementException
import java.util.ResourceBundle
import java.util.Scanner
import java.util.ServiceLoader
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

object Main {

    private val DEPRECATED_FLAGS = mapOf(
        "--ruleset-repository" to
            "--repository",
        "--reporter-repository" to
            "--repository",
        "--ruleset-update" to
            "--repository-update",
        "--reporter-update" to
            "--repository-update"
    )
    private val CLI_MAX_LINE_LENGTH_REGEX = Regex("(.{0,120})(?:\\s|$)")

    // todo: this should have been a command, not a flag (consider changing in 1.0.0)
    @Option(name = "--format", aliases = arrayOf("-F"), usage = "Fix any deviations from the code style")
    private var format: Boolean = false

    @Option(name = "--android", aliases = arrayOf("-a"), usage = "Turn on Android Kotlin Style Guide compatibility")
    private var android: Boolean = false

    @Option(name = "--reporter",
        usage = "A reporter to use (built-in: plain (default), plain?group_by_file, json, checkstyle). " +
            "To use a third-party reporter specify either a path to a JAR file on the filesystem or a" +
            "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
            "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
            "Maven Central/JCenter/JitPack/user-provided repository)")
    private var reporters = ArrayList<String>()

    @Option(name = "--ruleset", aliases = arrayOf("-R"),
        usage = "A path to a JAR file containing additional ruleset(s) or a " +
            "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
            "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
            "Maven Central/JCenter/JitPack/user-provided repository)")
    private var rulesets = ArrayList<String>()

    @Option(name = "--repository", aliases = arrayOf("--ruleset-repository", "--reporter-repository"),
        usage = "An additional Maven repository (Maven Central/JCenter/JitPack are active by default) " +
            "(value format: <id>=<url>)")
    private var repositories = ArrayList<String>()

    @Option(name = "--repository-update", aliases = arrayOf("-U", "--ruleset-update", "--reporter-update"),
        usage = "Check remote repositories for updated snapshots")
    private var forceUpdate: Boolean = false

    @Option(name = "--limit", usage = "Maximum number of errors to show (default: show all)")
    private var limit: Int = -1
        get() = if (field < 0) Int.MAX_VALUE else field

    @Option(name = "--relative", usage = "Print files relative to the working directory " +
        "(e.g. dir/file.kt instead of /home/user/project/dir/file.kt)")
    private var relative: Boolean = false

    @Option(name = "--verbose", aliases = arrayOf("-v"), usage = "Show error codes")
    private var verbose: Boolean = false

    @Option(name = "--stdin", usage = "Read file from stdin")
    private var stdin: Boolean = false

    @Option(name = "--version", usage = "Version", help = true)
    private var version: Boolean = false

    @Option(name = "--help", aliases = arrayOf("-h"), help = true)
    private var help: Boolean = false

    @Option(name = "--debug", usage = "Turn on debug output")
    private var debug: Boolean = false

    // todo: make it a command in 1.0.0 (it's too late now as we might interfere with valid "lint" patterns)
    @Option(name = "--apply-to-idea", usage = "Update Intellij IDEA project settings")
    private var apply: Boolean = false

    @Option(name = "-y", hidden = true)
    private var forceApply: Boolean = false

    @Option(name = "--install-git-pre-commit-hook",
        usage = "Install git hook to automatically check files for style violations on commit")
    private var installGitPreCommitHook: Boolean = false

    @Option(name = "--print-ast", usage = "Print AST (useful when writing/debugging rules)")
    private var printAST: Boolean = false
    @Option(name = "--color", usage = "Make output colorful")
    private var color: Boolean = false

    @Argument
    private var patterns = ArrayList<String>()

    private fun CmdLineParser.usage(): String =
        """
        An anti-bikeshedding Kotlin linter with built-in formatter (https://github.com/shyiko/ktlint).

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

          # use custom reporter
          ktlint --reporter=plain?group_by_file
          # multiple reporters can be specified like this
          ktlint --reporter=plain --reporter=checkstyle,output=ktlint-checkstyle-report.xml

        Flags:
${ByteArrayOutputStream().let { this.printUsage(it); it }.toString().trimEnd().split("\n")
            .map { line -> ("  " + line).replace(CLI_MAX_LINE_LENGTH_REGEX, "        $1\n").trimEnd() }
            .joinToString("\n")}
        """.trimIndent()

    private val workDir = File(".").canonicalPath
    private fun File.location() = if (relative) this.toRelativeString(File(workDir)) else this.path

    private fun parseCmdLine(args: Array<String>) {
        args.forEach { arg ->
            if (arg.startsWith("--") && arg.contains("=")) {
                val flag = arg.substringBefore("=")
                val alt = DEPRECATED_FLAGS[flag]
                if (alt != null) {
                    System.err.println("$flag flag is deprecated and will be removed in 1.0.0 (use $alt instead)")
                }
            }
        }
        val parser = object : CmdLineParser(this, ParserProperties.defaults()
            .withShowDefaults(false)
            .withUsageWidth(512)
            .withOptionSorter { l, r ->
                l.option.toString().replace("-", "").compareTo(r.option.toString().replace("-", ""))
            }) {

            override fun printOption(out: PrintWriter, handler: OptionHandler<*>, len: Int, rb: ResourceBundle?, filter: OptionHandlerFilter?) {
                handler.defaultMetaVariable
                val opt = handler.option as? NamedOptionDef ?: return
                if (opt.hidden() || opt.help()) {
                    return
                }
                val maxNameLength = options.map { h ->
                    val o = h.option
                    (o as? NamedOptionDef)?.let { it.name().length + 1 + (h.defaultMetaVariable ?: "").length } ?: 0
                }.max()!!
                val shorthand = opt.aliases().find { it.startsWith("-") && !it.startsWith("--") }
                val line = (if (shorthand != null) "$shorthand, " else "    ") +
                    (opt.name() + " " + (handler.defaultMetaVariable ?: "")).padEnd(maxNameLength, ' ') + "  " + opt.usage()
                out.println(line)
            }
        }
        try {
            parser.parseArgument(*args)
        } catch (err: CmdLineException) {
            System.err.println("Error: ${err.message}\n\n${parser.usage()}")
            exitProcess(1)
        }
        if (help) { println(parser.usage()); exitProcess(0) }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        parseCmdLine(args)
        if (version) {
            println(javaClass.`package`.implementationVersion)
            exitProcess(0)
        }
        if (installGitPreCommitHook) {
            installGitPreCommitHook()
            if (!apply) {
                exitProcess(0)
            }
        }
        if (apply) {
            applyToIDEA()
            exitProcess(0)
        }
        if (printAST) {
            printAST()
            exitProcess(0)
        }
        val start = System.currentTimeMillis()
        // load 3rd party ruleset(s) (if any)
        val dependencyResolver = lazy(LazyThreadSafetyMode.NONE) { buildDependencyResolver() }
        if (!rulesets.isEmpty()) {
            loadJARs(dependencyResolver, rulesets)
        }
        // standard should go first
        val ruleSetProviders = ServiceLoader.load(RuleSetProvider::class.java)
            .map { it.get().id to it }
            .sortedBy { if (it.first == "standard") "\u0000${it.first}" else it.first }
        if (debug) {
            ruleSetProviders.forEach { System.err.println("[DEBUG] Discovered ruleset \"${it.first}\"") }
        }
        val reporter = loadReporter(dependencyResolver)
        // load .editorconfig
        val userData = (
            EditorConfig.of(workDir)
                ?.also { editorConfig ->
                    if (debug) {
                        System.err.println("[DEBUG] Discovered .editorconfig (${
                            generateSequence(editorConfig) { it.parent }.map { it.path.parent.toFile().location() }.joinToString()
                        })")
                        System.err.println("[DEBUG] ${editorConfig.mapKeys { it.key }} loaded from .editorconfig")
                    }
                }
                ?: emptyMap<String, String>()
            ) + mapOf("android" to android.toString())
        val tripped = AtomicBoolean()
        data class LintErrorWithCorrectionInfo(val err: LintError, val corrected: Boolean)
        fun process(fileName: String, fileContent: String): List<LintErrorWithCorrectionInfo> {
            if (debug) {
                System.err.println("[DEBUG] Checking ${if (fileName != "<text>") File(fileName).location() else fileName}")
            }
            val result = ArrayList<LintErrorWithCorrectionInfo>()
            if (format) {
                val formattedFileContent = try {
                    format(fileName, fileContent, ruleSetProviders.map { it.second.get() }, userData) { err, corrected ->
                        if (!corrected) {
                            result.add(LintErrorWithCorrectionInfo(err, corrected))
                        }
                    }
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(e.toLintError(), false))
                    tripped.set(true)
                    fileContent // making sure `cat file | ktlint --stdint > file` is (relatively) safe
                }
                if (stdin) {
                    println(formattedFileContent)
                } else {
                    if (fileContent !== formattedFileContent) {
                        File(fileName).writeText(formattedFileContent, charset("UTF-8"))
                    }
                }
            } else {
                try {
                    lint(fileName, fileContent, ruleSetProviders.map { it.second.get() }, userData) { err ->
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
                reporter.onLintError(fileName, err, corrected)
            }
            reporter.after(fileName)
        }
        reporter.beforeAll()
        if (stdin) {
            report("<text>", process("<text>", String(System.`in`.readBytes())))
        } else {
            fileSequence()
                .takeWhile { errorNumber.get() < limit }
                .map { file -> Callable { file to process(file.path, file.readText()) } }
                .parallel({ (file, errList) -> report(file.location(), errList) })
        }
        reporter.afterAll()
        if (debug) {
            System.err.println("[DEBUG] ${
                System.currentTimeMillis() - start
            }ms / $fileNumber file(s) / $errorNumber error(s)")
        }
        if (tripped.get()) {
            exitProcess(1)
        }
    }

    private fun loadReporter(dependencyResolver: Lazy<MavenDependencyResolver>): Reporter {
        data class ReporterTemplate(val id: String, val config: Map<String, String>, var output: String?)
        val tpls = (if (reporters.isEmpty()) listOf("plain") else reporters)
            .map { reporter ->
                val split = reporter.split(",")
                val (reporterId, rawReporterConfig) = split[0].split("?", limit = 2) + listOf("")
                ReporterTemplate(
                    reporterId,
                    mapOf("verbose" to verbose.toString(), "color" to color.toString()) + parseQuery(rawReporterConfig),
                    split.getOrNull(1)?.let { if (it.startsWith("output=")) it.split("=")[1] else null }
                )
            }
            .distinct()
        val reporterLoader = ServiceLoader.load(ReporterProvider::class.java)
        val reporterProviderById = reporterLoader.associate { it.id to it }.let { map ->
            val missingReporters = tpls.map { it.id }.distinct().filter { !map.containsKey(it) }
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
                System.err.println("Error: reporter \"$id\" wasn't found (available: ${
                    reporterProviderById.keys.sorted().joinToString(",")
                })")
                exitProcess(1)
            }
            if (debug) {
                System.err.println("[DEBUG] Initializing \"$id\" reporter with $config" +
                    (output?.let { ", output=$it" } ?: ""))
            }
            val stream = if (output != null) {
                File(output).parentFile?.mkdirsOrFail(); PrintStream(output, "UTF-8")
            } else if (stdin) System.err else System.out
            return reporterProvider.get(stream, config)
                .let { reporter ->
                    if (output != null)
                        object : Reporter by reporter {
                            override fun afterAll() {
                                reporter.afterAll()
                                stream.close()
                            }
                        }
                    else reporter
                }
        }
        return Reporter.from(*tpls.map { it.toReporter() }.toTypedArray())
    }

    private fun Exception.toLintError(): LintError = this.let { e ->
        when (e) {
            is ParseException ->
                LintError(e.line, e.col, "",
                    "Not a valid Kotlin file (${e.message?.toLowerCase()})")
            is RuleExecutionException -> {
                if (debug) {
                    System.err.println("[DEBUG] Internal Error (${e.ruleId})")
                    e.printStackTrace(System.err)
                }
                LintError(e.line, e.col, "", "Internal Error (${e.ruleId}). " +
                    "Please create a ticket at https://github.com/shyiko/ktlint/issue " +
                    "(if possible, provide the source code that triggered an error)")
            }
            else -> throw e
        }
    }

    private fun printAST() {
        fun process(fileName: String, fileContent: String) {
            if (debug) {
                System.err.println("[DEBUG] Analyzing ${if (fileName != "<text>") File(fileName).location() else fileName}")
            }
            try {
                lint(fileName, fileContent, listOf(RuleSet("debug", DumpAST(System.out, color))), emptyMap()) {}
            } catch (e: Exception) {
                if (e is ParseException) {
                    throw ParseException(e.line, e.col, "Not a valid Kotlin file (${e.message?.toLowerCase()})")
                }
                throw e
            }
        }
        if (stdin) {
            process("<text>", String(System.`in`.readBytes()))
        } else {
            for (file in fileSequence()) {
                process(file.path, file.readText())
            }
        }
    }

    private fun fileSequence() =
        when {
            patterns.isEmpty() ->
                Glob.from("**/*.kt", "**/*.kts")
                    .iterate(Paths.get(workDir), Glob.IterationOption.SKIP_HIDDEN)
            else ->
                Glob.from(*patterns.map { expandTilde(it) }.toTypedArray())
                    .iterate(Paths.get(workDir))
        }
            .asSequence()
            .map(Path::toFile)

    private fun installGitPreCommitHook() {
        if (!File(".git").isDirectory) {
            System.err.println(".git directory not found. " +
                "Are you sure you are inside project root directory?")
            exitProcess(1)
        }
        val hooksDir = File(".git", "hooks")
        hooksDir.mkdirsOrFail()
        val preCommitHookFile = File(hooksDir, "pre-commit")
        val expectedPreCommitHook = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("ktlint-git-pre-commit-hook${if (android) "-android" else ""}.sh").readBytes()
        // backup existing hook (if any)
        val actualPreCommitHook = try { preCommitHookFile.readBytes() } catch (e: FileNotFoundException) { null }
        if (actualPreCommitHook != null && !actualPreCommitHook.isEmpty() && !Arrays.equals(actualPreCommitHook, expectedPreCommitHook)) {
            val backupFile = File(hooksDir, "pre-commit.ktlint-backup." + hex(actualPreCommitHook))
            System.err.println(".git/hooks/pre-commit -> $backupFile")
            preCommitHookFile.copyTo(backupFile, overwrite = true)
        }
        // > .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit
        preCommitHookFile.writeBytes(expectedPreCommitHook)
        preCommitHookFile.setExecutable(true)
        System.err.println(".git/hooks/pre-commit installed")
    }

    private fun applyToIDEA() {
        try {
            val workDir = Paths.get(".")
            if (!forceApply) {
                val fileList = IntellijIDEAIntegration.apply(workDir, true, android)
                System.err.println("The following files are going to be updated:\n\n\t" +
                    fileList.joinToString("\n\t") +
                    "\n\nDo you wish to proceed? [y/n]\n" +
                    "(in future, use -y flag if you wish to skip confirmation)")
                val scanner = Scanner(System.`in`)

                val res = generateSequence {
                        try { scanner.next() } catch (e: NoSuchElementException) { null }
                    }
                    .filter { line -> !line.trim().isEmpty() }
                    .first()
                if (!"y".equals(res, ignoreCase = true)) {
                    System.err.println("(update canceled)")
                    exitProcess(1)
                }
            }
            IntellijIDEAIntegration.apply(workDir, false, android)
        } catch (e: IntellijIDEAIntegration.ProjectNotFoundException) {
            System.err.println(".idea directory not found. " +
                "Are you sure you are inside project root directory?")
            exitProcess(1)
        }
        System.err.println("(updated)")
        System.err.println("\nPlease restart your IDE")
        System.err.println("(if you experience any issues please report them at https://github.com/shyiko/ktlint)")
    }

    private fun hex(input: ByteArray) = BigInteger(MessageDigest.getInstance("SHA-256").digest(input)).toString(16)

    // a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
    // this implementation takes care only of the most commonly used case (~/)
    private fun expandTilde(path: String) = path.replaceFirst(Regex("^~"), System.getProperty("user.home"))

    private fun <T> List<T>.head(limit: Int) = if (limit == size) this else this.subList(0, limit)

    private fun buildDependencyResolver(): MavenDependencyResolver {
        val mavenLocal = File(File(System.getProperty("user.home"), ".m2"), "repository")
        mavenLocal.mkdirsOrFail()
        val dependencyResolver = MavenDependencyResolver(
            mavenLocal,
            listOf(
                RemoteRepository.Builder(
                    "central", "default", "http://repo1.maven.org/maven2/"
                ).setSnapshotPolicy(RepositoryPolicy(false, UPDATE_POLICY_NEVER,
                    CHECKSUM_POLICY_IGNORE)).build(),
                RemoteRepository.Builder(
                    "bintray", "default", "http://jcenter.bintray.com"
                ).setSnapshotPolicy(RepositoryPolicy(false, UPDATE_POLICY_NEVER,
                    CHECKSUM_POLICY_IGNORE)).build(),
                RemoteRepository.Builder(
                    "jitpack", "default", "http://jitpack.io").build()
            ) + repositories.map { repository ->
                val colon = repository.indexOf("=").apply {
                    if (this == -1) { throw RuntimeException("$repository is not a valid repository entry " +
                        "(make sure it's provided as <id>=<url>") }
                }
                val id = repository.substring(0, colon)
                val url = repository.substring(colon + 1)
                RemoteRepository.Builder(id, "default", url).build()
            },
            forceUpdate
        )
        if (debug) {
            dependencyResolver.setTransferEventListener { e ->
                System.err.println("[DEBUG] Transfer ${e.type.toString().toLowerCase()} ${e.resource.repositoryUrl}" +
                    e.resource.resourceName + (e.exception?.let { " (${it.message})" } ?: ""))
            }
        }
        return dependencyResolver
    }

    private fun loadJARs(dependencyResolver: Lazy<MavenDependencyResolver>, artifacts: List<String>) {
        (ClassLoader.getSystemClassLoader() as java.net.URLClassLoader)
            .addURLs(artifacts.flatMap { artifact ->
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
                result
            })
    }

    private fun parseQuery(query: String) = query.split("&")
        .fold(LinkedHashMap<String, String>()) { map, s ->
            if (!s.isEmpty()) {
                s.split("=", limit = 2).let { e -> map.put(e[0],
                    URLDecoder.decode(e.getOrElse(1) { "true" }, "UTF-8")) }
            }
            map
        }

    private fun lint(fileName: String, text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>,
            cb: (e: LintError) -> Unit) =
        if (fileName.endsWith(".kt", ignoreCase = true)) KtLint.lint(text, ruleSets, userData, cb) else KtLint.lintScript(text, ruleSets, userData, cb)

    private fun format(fileName: String, text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>,
            cb: (e: LintError, corrected: Boolean) -> Unit): String =
        if (fileName.endsWith(".kt", ignoreCase = true)) KtLint.format(text, ruleSets, userData, cb) else KtLint.formatScript(text, ruleSets, userData, cb)

    private fun java.net.URLClassLoader.addURLs(url: Iterable<java.net.URL>) {
        val method = java.net.URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
        method.isAccessible = true
        url.forEach { method.invoke(this, it) }
    }

    private fun <T> Sequence<Callable<T>>.parallel(cb: (T) -> Unit,
        numberOfThreads: Int = Runtime.getRuntime().availableProcessors()) {
        val q = ArrayBlockingQueue<Future<T>>(numberOfThreads)
        val pill = object : Future<T> {

            override fun isDone(): Boolean { throw UnsupportedOperationException() }
            override fun get(timeout: Long, unit: TimeUnit): T { throw UnsupportedOperationException() }
            override fun get(): T { throw UnsupportedOperationException() }
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean { throw UnsupportedOperationException() }
            override fun isCancelled(): Boolean { throw UnsupportedOperationException() }
        }
        val consumer = Thread(Runnable {
            while (true) {
                val future = q.poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                if (future === pill) {
                    break
                }
                cb(future.get())
            }
        })
        consumer.start()
        val executorService = Executors.newCachedThreadPool()
        for (v in this) {
            q.put(executorService.submit(v))
        }
        q.put(pill)
        executorService.shutdown()
        consumer.join()
    }
}
