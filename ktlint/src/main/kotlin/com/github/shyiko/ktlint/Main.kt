package com.github.shyiko.ktlint

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.ParseException
import com.github.shyiko.ktlint.core.ReporterProvider
import com.github.shyiko.ktlint.core.RuleExecutionException
import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import com.github.shyiko.ktlint.internal.MavenDependencyResolver
import com.github.shyiko.ktlint.internal.path.Glob
import com.github.shyiko.ktlint.internal.path.GlobFileFilter
import com.github.shyiko.ktlint.internal.path.HiddenFileFilter
import com.github.shyiko.ktlint.internal.path.and
import com.github.shyiko.ktlint.internal.path.expandTilde
import com.github.shyiko.ktlint.internal.path.fromSlash
import com.github.shyiko.ktlint.internal.path.or
import com.github.shyiko.ktlint.internal.path.slash
import org.eclipse.aether.RepositoryException
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE
import org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER
import org.ini4j.Wini
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import org.kohsuke.args4j.Argument
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import org.kohsuke.args4j.ParserProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.net.URLDecoder
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.HashMap
import java.util.ServiceLoader
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
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

    // todo: this should have been a command, not a flag (consider changing in 1.0.0)
    @Option(name="--format", aliases = arrayOf("-F"), usage = "Fix any deviations from the code style")
    private var format: Boolean = false

    @Option(name="--reporter",
        usage = "A reporter to use (built-in: plain (default), plain?group_by_file, json, checkstyle). " +
            "To use a third-party reporter specify either a path to a JAR file on the filesystem or a" +
            "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
            "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
            "Maven Central/JCenter/JitPack/user-provided repository)")
    private var reporter: String = "plain"

    @Option(name="--ruleset", aliases = arrayOf("-R"),
        usage = "A path to a JAR file containing additional ruleset(s) or a " +
            "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
            "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
            "Maven Central/JCenter/JitPack/user-provided repository)")
    private var rulesets = ArrayList<String>()

    @Option(name="--repository", aliases = arrayOf("--ruleset-repository", "--reporter-repository"),
        usage = "An additional Maven repository (Maven Central/JCenter/JitPack are active by default)" +
            "(value format: <id>=<url>)")
    private var repositories = ArrayList<String>()

    @Option(name="--repository-update", aliases = arrayOf("-U", "--ruleset-update", "--reporter-update"),
        usage = "Check remote repositories for updated snapshots")
    private var forceUpdate: Boolean = false

    @Option(name="--verbose", aliases = arrayOf("-v"), usage = "Show error codes")
    private var verbose: Boolean = false

    @Option(name="--stdin", usage = "Read file from stdin")
    private var stdin: Boolean = false

    @Option(name="--version", usage = "Version", help = true)
    private var version: Boolean = false

    @Option(name="--help", aliases = arrayOf("-h"), help = true)
    private var help: Boolean = false

    @Option(name="--debug", usage = "Turn on debug output")
    private var debug: Boolean = false

    @Argument
    private var patterns = ArrayList<String>()

    private fun CmdLineParser.usage(): String =
        """
        Kotlin linter (https://github.com/shyiko/ktlint).

        Usage:
          ktlint <flags> [patterns]
          java -jar ktlint <flags> [patterns]

        Flags:
${ByteArrayOutputStream().let { this.printUsage(it); it }.toString().trimEnd().split("\n").map { "         $it" }
            .joinToString("\n")}

        Examples:
          # check the style of all Kotlin files inside the current dir (recursively)
          # (hidden folders will be skipped)
          ktlint

          # check only certain locations (prepend ! to negate the pattern)
          ktlint "src/**/*.kt" "!src/**/*Test.kt"

          # auto-correct style violations
          ktlint -F "src/**/*.kt"

          # use custom reporter
          ktlint --reporter=checkstyle > ktlint-report-in-checkstyle-format.xml
        """.trimIndent()

    @JvmStatic
    fun main(args: Array<String>) {
        args.forEach {
            if (it.startsWith("--") && it.contains("=")) {
                val flag = it.substringBefore("=")
                val alt = DEPRECATED_FLAGS[flag]
                if (alt != null) {
                    System.err.println("$flag flag is deprecated and will be removed in 1.0.0 (use $alt instead)")
                }
            }
        }
        val parser = CmdLineParser(this, ParserProperties.defaults()
            .withShowDefaults(false)
            .withUsageWidth(120)
            .withOptionSorter({ l, r ->
                l.option.toString().replace("-", "").compareTo(r.option.toString().replace("-", ""))
            }))
        try {
            parser.parseArgument(*args)
        } catch (err: CmdLineException) {
            System.err.println("Error: ${err.message}\n\n${parser.usage()}")
            exitProcess(1)
        }
        if (version) { println(javaClass.`package`.implementationVersion); exitProcess(0) }
        if (help) { println(parser.usage()); exitProcess(0) }
        val workDir = File(".").canonicalPath
        var tripped = false
        val start = System.currentTimeMillis()
        // load 3rd party ruleset(s) (if any)
        val dependencyResolver by lazy { buildDependencyResolver() }
        if (!rulesets.isEmpty()) {
            loadJARs(dependencyResolver, rulesets)
        }
        // standard should go first
        val rp = ServiceLoader.load(RuleSetProvider::class.java)
            .map { it.get().id to it }
            .sortedBy { if (it.first == "standard") "\u0000${it.first}" else it.first }
        if (debug) {
            rp.forEach { System.err.println("[DEBUG] Discovered ruleset \"${it.first}\"") }
        }
        val (reporterId, rawReporterConfig) = this.reporter.split("?", limit = 2) + listOf("")
        // load reporter
        val reporterLoader = ServiceLoader.load(ReporterProvider::class.java)
        val reporters = reporterLoader.associate { it.id to it }.let { map ->
            if (!map.containsKey(reporterId)) {
                loadJARs(dependencyResolver, listOf(reporterId))
                reporterLoader.reload()
                reporterLoader.associate { it.id to it }
            } else map
        }
        if (debug) {
            reporters.forEach { (id) -> System.err.println("[DEBUG] Discovered reporter \"$id\"") }
        }
        val reporterConfig = mapOf("verbose" to verbose.toString()) + parseQuery(rawReporterConfig)
        System.err.println("[DEBUG] Initializing \"$reporterId\" reporter with $reporterConfig")
        val reporter = reporters[reporterId]?.get(
            if (stdin) System.err else System.out, reporterConfig
        )
        if (reporter == null) {
            System.err.println("Error: reporter \"$reporterId\" wasn't found")
            exitProcess(1)
        }
        // load .editorconfig
        val userData = locateEditorConfig(File(workDir))?.let {
            if (debug) {
                System.err.println("[DEBUG] Discovered .editorconfig (${it.parent})")
            }
            loadEditorConfig(it)
        } ?: emptyMap()
        if (debug) {
            System.err.println("[DEBUG] ${userData.mapKeys { it.key }} loaded from .editorconfig")
        }
        data class LintErrorWithCorrectionInfo(val err: LintError, val corrected: Boolean)
        fun lintErrorFrom(e: Exception): LintError = when (e) {
            is ParseException ->
                LintError(e.line, e.col, "",
                "Not a valid Kotlin file (${e.message?.toLowerCase()})")
            is RuleExecutionException ->
                LintError(e.line, e.col, "", "Internal Error (${e.ruleId}). " +
                "Please create a ticket at https://github.com/shyiko/ktlint/issue " +
                "(if possible, provide the source code that triggered an error)")
            else -> throw e
        }
        fun process(fileName: String, fileContent: String): List<LintErrorWithCorrectionInfo> {
            if (debug) {
                System.err.println("[DEBUG] Checking $fileName")
            }
            val result = ArrayList<LintErrorWithCorrectionInfo>()
            if (format) {
                val formattedFileContent = try {
                    format(fileName, fileContent, rp.map { it.second.get() }, userData, { err, corrected ->
                        if (!corrected) {
                            result.add(LintErrorWithCorrectionInfo(err, corrected))
                        }
                    })
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(lintErrorFrom(e), false))
                    tripped = true
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
                    lint(fileName, fileContent, rp.map { it.second.get() }, userData, { err ->
                        tripped = true
                        result.add(LintErrorWithCorrectionInfo(err, false))
                    })
                } catch (e: Exception) {
                    result.add(LintErrorWithCorrectionInfo(lintErrorFrom(e), false))
                }
            }
            return result
        }
        var fileNumber = 0
        var errorNumber = 0
        fun report(fileName: String, errList: List<LintErrorWithCorrectionInfo>) {
            fileNumber++
            errorNumber += errList.size
            reporter.before(fileName)
            errList.forEach { (err, corrected) -> reporter.onLintError(fileName, err, corrected) }
            reporter.after(fileName)
        }
        fun process(dir: File, filter: FileFilter) = visit(dir, filter)
            .map { file -> Callable { file.path to process(file.path, file.readText()) } }
            .parallel({ (fileName, errList) -> report(fileName, errList) })
        reporter.beforeAll()
        if (stdin) {
            report("<text>", process("<text>", String(System.`in`.readBytes())))
        } else {
            if (patterns.isEmpty()) {
                val filter = HiddenFileFilter(reverse = true)
                    .and(GlobFileFilter(workDir, "**/*.kt").or(GlobFileFilter(workDir, "**/*.kts")))
                process(File(workDir), filter)
            } else {
                val patterns = patterns.map { expandTilde(it) }
                val filter = GlobFileFilter(workDir, *patterns.toTypedArray())
                patterns
                    .map { Glob.prefix(slash(it)) }
                    .distinct()
                    .map { (if (it.startsWith("/")) File(fromSlash(it)) else
                        File(workDir, fromSlash(it))).canonicalPath }
                    // remove overlapping paths (e.g. /a & /a/b -> /a)
                    .sorted()
                    .fold(ArrayList<String>(), { r, v ->
                        if (r.isEmpty() || !v.startsWith(r.last())) { r.add(v) }; r })
                    .forEach { process(File(it), filter) }
            }
        }
        reporter.afterAll()
        if (debug) {
            System.err.println("[DEBUG] ${(System.currentTimeMillis() - start)
                }ms / ${fileNumber}file(s) / ${errorNumber}error(s)")
        }
        if (tripped) {
            exitProcess(1)
        }
    }

    fun buildDependencyResolver(): MavenDependencyResolver {
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
            ) + repositories.map {
                val colon = it.indexOf("=").apply {
                    if (this == -1) { throw RuntimeException("$it is not a valid repository entry " +
                        "(make sure it's provided as <id>=<url>") }
                }
                val id = it.substring(0, colon)
                val url = it.substring(colon + 1)
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

    fun loadJARs(dependencyResolver: MavenDependencyResolver, artifacts: List<String>) {
        (ClassLoader.getSystemClassLoader() as java.net.URLClassLoader)
            .addURLs(artifacts.flatMap {
                if (debug) {
                    System.err.println("[DEBUG] Resolving $it")
                }
                val result = try {
                    dependencyResolver.resolve(DefaultArtifact(it)).map { it.toURI().toURL() }
                } catch (e: IllegalArgumentException) {
                    val file = File(expandTilde(it))
                    if (!file.exists()) {
                        System.err.println("Error: $it does not exist")
                        exitProcess(1)
                    }
                    listOf(file.toURI().toURL())
                } catch (e: RepositoryException) {
                    if (debug) {
                        e.printStackTrace()
                    }
                    System.err.println("Error: $it wasn't found")
                    exitProcess(1)
                }
                if (debug) {
                    result.forEach { url -> System.err.println("[DEBUG] Loading $url") }
                }
                result
            })
    }

    fun parseQuery(query: String) = query.split("&")
        .fold(HashMap<String, String>(), { map, s ->
            if (!s.isEmpty()) {
                s.split("=", limit = 2).let { map.put(it[0],
                    URLDecoder.decode(it.getOrElse(1, { "true" }), "UTF-8")) }
            }
            map
        })


    fun locateEditorConfig(dir: File?): File? = when (dir) {
        null -> null
        else -> File(dir, ".editorconfig").let {
            if (it.exists()) it else locateEditorConfig(dir.parentFile)
        }
    }

    fun loadEditorConfig(file: File): Map<String, String> {
        val editorConfig = Wini(file)
        // right now ktlint requires explicit [*.{kt,kts}] section
        // (this way we can be sure that users want .editorconfig to be recognized by ktlint)
        val section = editorConfig["*.{kt,kts}"]
        return section?.toSortedMap() ?: emptyMap<String, String>()
    }

    fun lint(fileName: String, text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>,
            cb: (e: LintError) -> Unit) =
        if (fileName.endsWith(".kt", ignoreCase = true)) KtLint.lint(text, ruleSets, userData, cb) else
            KtLint.lintScript(text, ruleSets, userData, cb)

    fun format(fileName: String, text: String, ruleSets: Iterable<RuleSet>, userData: Map<String, String>,
            cb: (e: LintError, corrected: Boolean) -> Unit): String =
        if (fileName.endsWith(".kt", ignoreCase = true)) KtLint.format(text, ruleSets, userData, cb) else
            KtLint.formatScript(text, ruleSets, userData, cb)

    fun visit(dir: File, filter: FileFilter): Sequence<File> {
        val stack = ArrayDeque<File>().apply { push(dir) }
        return generateSequence(fun (): File? {
            while (true) {
                val file = stack.pollLast()
                if (file == null || file.isFile) {
                    return file
                }
                if (file.isDirectory) {
                    val fileList = file.listFiles(filter)
                    if (fileList != null) {
                        stack.addAll(fileList)
                    }
                }
            }
        })
    }

    fun java.net.URLClassLoader.addURLs(url: Iterable<java.net.URL>) {
        val method = java.net.URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
        method.isAccessible = true
        url.forEach { method.invoke(this, it) }
    }

    fun <T>Sequence<Callable<T>>.parallel(cb: (T) -> Unit,
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
