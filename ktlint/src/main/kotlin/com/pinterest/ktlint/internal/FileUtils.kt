package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.initKtLintKLogger
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import mu.KotlinLogging
import org.jetbrains.kotlin.util.prefixIfNot

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

internal val workDir: String = File(".").canonicalPath

private val tildeRegex = Regex("^(!)?~")
private const val NEGATION_PREFIX = "!"

private val os = System.getProperty("os.name")
private val userHome = System.getProperty("user.home")

private val defaultKotlinFileExtensions = setOf("kt", "kts")
internal val defaultPatterns = defaultKotlinFileExtensions.map { "**$globSeparator*.$it" }

/**
 * Transform the [patterns] to a sequence of files. Each element in [patterns] can be a glob, a file or directory path
 * relative to the [rootDir] or a absolute file or directory path.
 */
internal fun FileSystem.fileSequence(
    patterns: List<String>,
    rootDir: Path = Paths.get(".").toAbsolutePath().normalize()
): Sequence<Path> {
    val result = mutableListOf<Path>()

    val (existingFiles, patternsExclusiveExistingFiles) = patterns.partition {
        try {
            Files.isRegularFile(rootDir.resolve(it))
        } catch (e: InvalidPathException) {
            // Windows throws an exception when you pass a glob to Path#resolve.
            false
        }
    }
    existingFiles.mapTo(result) { rootDir.resolve(it) }

    // Return early and don't traverse the file system if all the input globs are absolute paths
    if (result.isNotEmpty() && patternsExclusiveExistingFiles.isEmpty()) {
        return result.asSequence()
    }

    val globs = expand(patternsExclusiveExistingFiles, rootDir)

    val pathMatchers = if (globs.isEmpty()) {
        defaultPatterns
            .map { getPathMatcher("glob:$it") }
            .toSet()
    } else {
        globs
            .filterNot { it.startsWith(NEGATION_PREFIX) }
            .map { getPathMatcher(it) }
    }

    val negatedPathMatchers = if (globs.isEmpty()) {
        emptySet()
    } else {
        globs
            .filter { it.startsWith(NEGATION_PREFIX) }
            .map { getPathMatcher(it.removePrefix(NEGATION_PREFIX)) }
    }

    logger.debug {
        """
        Start walkFileTree for rootDir: '$rootDir'
           include:
        ${pathMatchers.map { "      - $it" }}
           exlcude:
        ${negatedPathMatchers.map { "      - $it" }}
        """.trimIndent()
    }
    val duration = measureTimeMillis {
        Files.walkFileTree(
            rootDir,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    filePath: Path,
                    fileAttrs: BasicFileAttributes
                ): FileVisitResult {
                    if (negatedPathMatchers.none { it.matches(filePath) } &&
                        pathMatchers.any { it.matches(filePath) }
                    ) {
                        logger.debug { "- File: $filePath: Include" }
                        result.add(filePath)
                    } else {
                        logger.debug { "- File: $filePath: Ignore" }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(
                    dirPath: Path,
                    dirAttr: BasicFileAttributes
                ): FileVisitResult {
                    return if (Files.isHidden(dirPath)) {
                        logger.debug { "- Dir: $dirPath: Ignore" }
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        logger.debug { "- Dir: $dirPath: Traverse" }
                        FileVisitResult.CONTINUE
                    }
                }
            }
        )
    }
    logger.debug { "Results: include ${result.count()} files in $duration ms" }

    return result.asSequence()
}

private fun FileSystem.expand(
    patterns: List<String>,
    rootDir: Path
) =
    patterns
        .map { it.expandTildeToFullPath() }
        .map { it.replace(File.separator, globSeparator) }
        .flatMap { path -> toGlob(path, rootDir) }

private fun FileSystem.toGlob(
    path: String,
    rootDir: Path
): List<String> {
    val negation = if (path.startsWith(NEGATION_PREFIX)) {
        NEGATION_PREFIX
    } else {
        ""
    }
    val pathWithoutNegationPrefix = path.removePrefix(NEGATION_PREFIX)
    val resolvedPath = try {
        rootDir.resolve(pathWithoutNegationPrefix)
    } catch (e: InvalidPathException) {
        // Windows throws an exception when you pass a glob to Path#resolve.
        null
    }
    val expandedGlobs = if (resolvedPath != null && resolvedPath.isDirectory()) {
        getDefaultPatternsForPath(resolvedPath)
    } else if (isGlobAbsolutePath(pathWithoutNegationPrefix)) {
        listOf(pathWithoutNegationPrefix)
    } else {
        listOf(pathWithoutNegationPrefix.prefixIfNot("**$globSeparator"))
    }
    return expandedGlobs.map { "${negation}glob:$it" }
}

private fun getDefaultPatternsForPath(path: Path?) = defaultKotlinFileExtensions
    .flatMap {
        listOf(
            "$path$globSeparator*.$it",
            "$path$globSeparator**$globSeparator*.$it"
        )
    }

private fun FileSystem.isGlobAbsolutePath(glob: String) =
    rootDirectories
        .map { it.toString() }
        .any { glob.startsWith(it) }

private val globSeparator: String get() =
    when {
        os.startsWith("windows", ignoreCase = true) -> "\\\\"
        else -> "/"
    }

/**
 * List of paths to Java `jar` files.
 */
internal typealias JarFiles = List<String>

internal fun JarFiles.toFilesURIList() = map {
    val jarFile = File(it.expandTildeToFullPath())
    if (!jarFile.exists()) {
        logger.error { "File $it does not exist" }
        exitProcess(1)
    }
    jarFile.toURI().toURL()
}

// a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
// this implementation takes care only of the most commonly used case (~/)
private fun String.expandTildeToFullPath(): String =
    if (os.startsWith("windows", true)) {
        // Windows sometimes inserts `~` into paths when using short directory names notation, e.g. `C:\Users\USERNA~1\Documents
        this
    } else {
        replaceFirst(tildeRegex, userHome)
    }

internal fun File.location(
    relative: Boolean
) = if (relative) this.toRelativeString(File(workDir)) else this.path

/**
 * Run lint over common kotlin file or kotlin script file.
 */
internal fun lintFile(
    fileName: String,
    fileContents: String,
    ruleProviders: Set<RuleProvider>,
    editorConfigOverride: EditorConfigOverride,
    editorConfigPath: String? = null,
    debug: Boolean = false,
    lintErrorCallback: (LintError) -> Unit = {}
) = KtLint.lint(
    KtLint.ExperimentalParams(
        fileName = fileName,
        text = fileContents,
        ruleProviders = ruleProviders,
        script = !fileName.endsWith(".kt", ignoreCase = true),
        editorConfigOverride = editorConfigOverride,
        editorConfigPath = editorConfigPath,
        cb = { e, _ ->
            lintErrorCallback(e)
        },
        debug = debug,
        isInvokedFromCli = true
    )
)

/**
 * Format a kotlin file or script file
 */
internal fun formatFile(
    fileName: String,
    fileContents: String,
    ruleProviders: Set<RuleProvider>,
    editorConfigOverride: EditorConfigOverride,
    editorConfigPath: String?,
    debug: Boolean,
    cb: (e: LintError, corrected: Boolean) -> Unit
): String =
    KtLint.format(
        KtLint.ExperimentalParams(
            fileName = fileName,
            text = fileContents,
            ruleProviders = ruleProviders,
            script = !fileName.endsWith(".kt", ignoreCase = true),
            editorConfigOverride = editorConfigOverride,
            editorConfigPath = editorConfigPath,
            cb = cb,
            debug = debug,
            isInvokedFromCli = true
        )
    )
