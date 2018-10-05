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
import org.jetbrains.kotlin.backend.common.onlyIf
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.PrintStream
import java.math.BigInteger
import java.net.URLDecoder
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedHashMap
import java.util.NoSuchElementException
import java.util.Scanner
import java.util.ServiceLoader
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.Manifest
import kotlin.concurrent.thread
import kotlin.system.exitProcess

@Command(
    headerHeading = """An anti-bikeshedding Kotlin linter with built-in formatter
(https://github.com/shyiko/ktlint).

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
    customSynopsis = arrayOf(""),
    sortOptions = false
)
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
    @Option(names = arrayOf("--android", "-a"), description = arrayOf("Turn on Android Kotlin Style Guide compatibility"))
    private var android: Boolean = false

    // todo: make it a command in 1.0.0 (it's too late now as we might interfere with valid "lint" patterns)
    @Option(names = arrayOf("--apply-to-idea"), description = arrayOf("Update Intellij IDEA settings (global)"))
    private var apply: Boolean = false

    // todo: make it a command in 1.0.0 (it's too late now as we might interfere with valid "lint" patterns)
    @Option(names = arrayOf("--apply-to-idea-project"), description = arrayOf("Update Intellij IDEA project settings"))
    private var applyToProject: Boolean = false

    @Option(names = arrayOf("--color"), description = arrayOf("Make output colorful"))
    private var color: Boolean = false

    @Option(names = arrayOf("--debug"), description = arrayOf("Turn on debug output"))
    private var debug: Boolean = false

    // todo: this should have been a command, not a flag (consider changing in 1.0.0)
    @Option(names = arrayOf("--format", "-F"), description = arrayOf("Fix any deviations from the code style"))
    private var format: Boolean = false

    @Option(names = arrayOf("--install-git-pre-commit-hook"), description = arrayOf(
        "Install git hook to automatically check files for style violations on commit"
    ))
    private var installGitPreCommitHook: Boolean = false

    @Option(names = arrayOf("--install-git-pre-push-hook"), description = arrayOf(
        "Install git hook to automatically check files for style violations before push"
    ))
    private var installGitPrePushHook: Boolean = false

    @Option(names = arrayOf("--limit"), description = arrayOf(
        "Maximum number of errors to show (default: show all)"
    ))
    private var limit: Int = -1
        get() = if (field < 0) Int.MAX_VALUE else field

    @Option(names = arrayOf("--print-ast"), description = arrayOf(
        "Print AST (useful when writing/debugging rules)"
    ))
    private var printAST: Boolean = false

    @Option(names = arrayOf("--relative"), description = arrayOf(
        "Print files relative to the working directory " +
            "(e.g. dir/file.kt instead of /home/user/project/dir/file.kt)"
    ))
    private var relative: Boolean = false

    @Option(names = arrayOf("--reporter"), description = arrayOf(
        "A reporter to use (built-in: plain (default), plain?group_by_file, json, checkstyle). " +
        "To use a third-party reporter specify either a path to a JAR file on the filesystem or a" +
        "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
        "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
        "Maven Central/JCenter/JitPack/user-provided repository)\n" +
        "e.g. \"html,artifact=com.github.username:ktlint-reporter-html:master-SNAPSHOT\""
    ))
    private var reporters = ArrayList<String>()

    @Option(names = arrayOf("--repository"), description = arrayOf(
        "An additional Maven repository (Maven Central/JCenter/JitPack are active by default) " +
            "(value format: <id>=<url>)"
    ))
    private var repositories = ArrayList<String>()
    @Option(names = arrayOf("--ruleset-repository", "--reporter-repository"), hidden = true)
    private var repositoriesDeprecated = ArrayList<String>()

    @Option(names = arrayOf("--repository-update", "-U"), description = arrayOf(
        "Check remote repositories for updated snapshots"
    ))
    private var forceUpdate: Boolean? = null
    @Option(names = arrayOf("--ruleset-update", "--reporter-update"), hidden = true)
    private var forceUpdateDeprecated: Boolean? = null

    @Option(names = arrayOf("--ruleset", "-R"), description = arrayOf(
        "A path to a JAR file containing additional ruleset(s) or a " +
        "<groupId>:<artifactId>:<version> triple pointing to a remote artifact (in which case ktlint will first " +
        "check local cache (~/.m2/repository) and then, if not found, attempt downloading it from " +
        "Maven Central/JCenter/JitPack/user-provided repository)"
    ))
    private var rulesets = ArrayList<String>()

    @Option(names = arrayOf("--skip-classpath-check"), description = arrayOf("Do not check classpath for potential conflicts"))
    private var skipClasspathCheck: Boolean = false

    @Option(names = arrayOf("--stdin"), description = arrayOf("Read file from stdin"))
    private var stdin: Boolean = false

    @Option(names = arrayOf("--verbose", "-v"), description = arrayOf("Show error codes"))
    private var verbose: Boolean = false

    @Option(names = arrayOf("--version"), description = arrayOf("Print version information"))
    private var version: Boolean = false

    @Option(names = arrayOf("--help", "-h"), help = true, hidden = true)
    private var help: Boolean = false

    @Option(names = arrayOf("-y"), hidden = true)
    private var forceApply: Boolean = false

    @Option(names = arrayOf("--editorconfig"), description = arrayOf("Path to .editorconfig"))
    private var editorConfigPath: String? = null

    @Parameters(hidden = true)
    private var patterns = ArrayList<String>()

    private val workDir = File(".").canonicalPath
    private fun File.location() = if (relative) this.toRelativeString(File(workDir)) else this.path

    private fun usage() =
        ByteArrayOutputStream()
            .also { CommandLine.usage(this, PrintStream(it), CommandLine.Help.Ansi.OFF) }
            .toString()
            .replace(" ".repeat(32), " ".repeat(30))

    private fun parseCmdLine(args: Array<String>) {
        try {
            CommandLine.populateCommand(this, *args)
            repositories.addAll(repositoriesDeprecated)
            if (forceUpdateDeprecated != null && forceUpdate == null) {
                forceUpdate = forceUpdateDeprecated
            }
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}\n\n${usage()}")
            exitProcess(1)
        }
        if (help) {
            println(usage())
            exitProcess(0)
        }
        args.forEach { arg ->
            if (arg.startsWith("--") && arg.contains("=")) {
                val flag = arg.substringBefore("=")
                val alt = DEPRECATED_FLAGS[flag]
                if (alt != null) {
                    System.err.println("$flag flag is deprecated and will be removed in 1.0.0 (use $alt instead)")
                }
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        parseCmdLine(args)
        if (version) {
            println(getImplementationVersion())
            exitProcess(0)
        }
        if (installGitPreCommitHook) {
            installGitPreCommitHook()
            if (!apply) {
                exitProcess(0)
            }
        }
        if (installGitPrePushHook) {
            installGitPrePushHook()
            if (!apply) {
                exitProcess(0)
            }
        }
        if (apply || applyToProject) {
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
        val tripped = AtomicBoolean()
        val reporter = loadReporter(dependencyResolver) { tripped.get() }
        val resolveUserData = userDataResolver()
        data class LintErrorWithCorrectionInfo(val err: LintError, val corrected: Boolean)
        fun process(fileName: String, fileContent: String): List<LintErrorWithCorrectionInfo> {
            if (debug) {
                System.err.println("[DEBUG] Checking ${if (fileName != "<text>") File(fileName).location() else fileName}")
            }
            val result = ArrayList<LintErrorWithCorrectionInfo>()
            val userData = resolveUserData(fileName)
            if (format) {
                val formattedFileContent = try {
                    format(fileName, fileContent, ruleSetProviders.map { it.second.get() }, userData) { err, corrected ->
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

    private fun userDataResolver(): (String) -> Map<String, String> {
        val cliUserData = mapOf("android" to android.toString())
        if (editorConfigPath != null) {
            val userData = (
                EditorConfig.of(File(editorConfigPath).canonicalPath)
                    ?.onlyIf({ debug }) { printEditorConfigChain(it) }
                ?: emptyMap<String, String>()
            ) + cliUserData
            return fun (fileName: String) = userData + ("file_path" to fileName)
        }
        val workdirUserData = lazy {
            (
                EditorConfig.of(workDir)
                    ?.onlyIf({ debug }) { printEditorConfigChain(it) }
                ?: emptyMap<String, String>()
            ) + cliUserData
        }
        val editorConfig = EditorConfig.cached()
        val editorConfigSet = ConcurrentHashMap<Path, Boolean>()
        return fun (fileName: String): Map<String, String> {
            if (fileName == "<text>") {
                return workdirUserData.value
            }
            return (
                editorConfig.of(Paths.get(fileName).parent)
                    ?.onlyIf({ debug }) {
                        printEditorConfigChain(it) {
                            editorConfigSet.put(it.path, true) != true
                        }
                    }
                ?: emptyMap<String, String>()
            ) + cliUserData + ("file_path" to fileName)
        }
    }

    private fun printEditorConfigChain(ec: EditorConfig, predicate: (EditorConfig) -> Boolean = { true }) {
        for (lec in generateSequence(ec) { it.parent }.takeWhile(predicate)) {
            System.err.println("[DEBUG] Discovered .editorconfig (${lec.path.parent.toFile().location()})" +
                " {${lec.entries.joinToString(", ")}}")
        }
    }

    private fun getImplementationVersion() = javaClass.`package`.implementationVersion
        // JDK 9 regression workaround (https://bugs.openjdk.java.net/browse/JDK-8190987, fixed in JDK 10)
        // (note that version reported by the fallback might not be null if META-INF/MANIFEST.MF is
        // loaded from another JAR on the classpath (e.g. if META-INF/MANIFEST.MF wasn't created as part of the build))
        ?: javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")
            ?.let { stream ->
                Manifest(stream).mainAttributes.getValue("Implementation-Version")
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
                    if (output != null) {
                        object : Reporter by reporter {
                            override fun afterAll() {
                                reporter.afterAll()
                                stream.close()
                                if (tripped()) {
                                    System.err.println("\"$id\" report written to ${File(output).absoluteFile.location()}")
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

    private fun installGitPrePushHook() {
        if (!File(".git").isDirectory) {
            System.err.println(".git directory not found. " +
                "Are you sure you are inside project root directory?")
            exitProcess(1)
        }
        val hooksDir = File(".git", "hooks")
        hooksDir.mkdirsOrFail()
        val prePushHookFile = File(hooksDir, "pre-push")
        val expectedPrePushHook = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("ktlint-git-pre-push-hook${if (android) "-android" else ""}.sh").readBytes()
        // backup existing hook (if any)
        val actualPrePushHook = try { prePushHookFile.readBytes() } catch (e: FileNotFoundException) { null }
        if (actualPrePushHook != null && !actualPrePushHook.isEmpty() && !Arrays.equals(actualPrePushHook, expectedPrePushHook)) {
            val backupFile = File(hooksDir, "pre-push.ktlint-backup." + hex(actualPrePushHook))
            System.err.println(".git/hooks/pre-push -> $backupFile")
            prePushHookFile.copyTo(backupFile, overwrite = true)
        }
        // > .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit
        prePushHookFile.writeBytes(expectedPrePushHook)
        prePushHookFile.setExecutable(true)
        System.err.println(".git/hooks/pre-push installed")
    }

    private fun applyToIDEA() {
        try {
            val workDir = Paths.get(".")
            if (!forceApply) {
                val fileList = IntellijIDEAIntegration.apply(workDir, true, android, applyToProject)
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
            IntellijIDEAIntegration.apply(workDir, false, android, applyToProject)
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
            forceUpdate == true
        )
        if (debug) {
            dependencyResolver.setTransferEventListener { e ->
                System.err.println("[DEBUG] Transfer ${e.type.toString().toLowerCase()} ${e.resource.repositoryUrl}" +
                    e.resource.resourceName + (e.exception?.let { " (${it.message})" } ?: ""))
            }
        }
        return dependencyResolver
    }

    // fixme: isn't going to work on JDK 9
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
                if (!skipClasspathCheck) {
                    if (result.any { it.toString().substringAfterLast("/").startsWith("ktlint-core-") }) {
                        System.err.println("\"$artifact\" appears to have a runtime/compile dependency on \"ktlint-core\".\n" +
                            "Please inform the author that \"com.github.shyiko:ktlint*\" should be marked " +
                            "compileOnly (Gradle) / provided (Maven).\n" +
                            "(to suppress this warning use --skip-classpath-check)")
                    }
                    if (result.any { it.toString().substringAfterLast("/").startsWith("kotlin-stdlib-") }) {
                        System.err.println("\"$artifact\" appears to have a runtime/compile dependency on \"kotlin-stdlib\".\n" +
                            "Please inform the author that \"org.jetbrains.kotlin:kotlin-stdlib*\" should be marked " +
                            "compileOnly (Gradle) / provided (Maven).\n" +
                            "(to suppress this warning use --skip-classpath-check)")
                    }
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

    private fun lint(
        fileName: String,
        text: String,
        ruleSets: Iterable<RuleSet>,
        userData: Map<String, String>,
        cb: (e: LintError) -> Unit
    ) =
        if (fileName.endsWith(".kt", ignoreCase = true)) {
            KtLint.lint(text, ruleSets, userData, cb)
        } else {
            KtLint.lintScript(text, ruleSets, userData, cb)
        }

    private fun format(
        fileName: String,
        text: String,
        ruleSets: Iterable<RuleSet>,
        userData: Map<String, String>,
        cb: (e: LintError, corrected: Boolean) -> Unit
    ): String =
        if (fileName.endsWith(".kt", ignoreCase = true)) {
            KtLint.format(text, ruleSets, userData, cb)
        } else {
            KtLint.formatScript(text, ruleSets, userData, cb)
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
