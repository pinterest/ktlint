package com.pinterest.ktlint.internal

import ch.qos.logback.classic.Level
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.setDefaultLoggerModifier
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import mu.KotlinLogging
import org.jetbrains.kotlin.util.prefixIfNot

private val logger = KotlinLogging.logger {}.initKtLintKLogger()
    .setDefaultLoggerModifier { Level.TRACE } // TODO: remove

internal val workDir: String = File(".").canonicalPath

private val tildeRegex = Regex("^(!)?~")
private const val NEGATION_PREFIX = "!"

private val userHome = System.getProperty("user.home")

private val defaultKotlinFileExtensions = setOf("kt", "kts")
internal val defaultPatterns = defaultKotlinFileExtensions.map { "**/*.$it" }

/**
 * Transform the [patterns] to a sequence of files. Each element in [patterns] can be a glob, a file or directory path
 * relative to the [rootDir] or a absolute file or directory path.
 */
internal fun FileSystem.fileSequence(
    patterns: List<String>,
    rootDir: Path = Paths.get(".").toAbsolutePath().normalize(),
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
           exclude:
        ${negatedPathMatchers.map { "      - $it" }}
        """.trimIndent()
    }
    val duration = measureTimeMillis {
        Files.walkFileTree(
            rootDir,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    filePath: Path,
                    fileAttrs: BasicFileAttributes,
                ): FileVisitResult {
                    val path =
//                        filePath
//                            .absolutePathString()
//                            .removePrefix(rootDir.absolutePathString())
//                            .replace(File.separatorChar, '/')
//                            .let { Paths.get(it) }
                        if (onWindowsOS) {
                            filePath
                                .absolutePathString()
                                .replace(File.separatorChar, '/')
                                .toPath()
                        } else {
                            filePath
                        }
                    if (negatedPathMatchers.none { it.matches(path) } &&
                        pathMatchers.any { it.matches(path) }
                    ) {
                        logger.trace { "- File: $filePath: Include" }
                        result.add(filePath)
                    } else {
                        logger.trace { "- File: $filePath: Ignore" }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(
                    dirPath: Path,
                    dirAttr: BasicFileAttributes,
                ): FileVisitResult {
                    return if (Files.isHidden(dirPath)) {
                        logger.trace { "- Dir: $dirPath: Ignore" }
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        logger.trace { "- Dir: $dirPath: Traverse" }
                        FileVisitResult.CONTINUE
                    }
                }
            },
        )
    }
    logger.debug { "Discovered ${result.count()} files to be processed in $duration ms" }

    return result.asSequence()
}

internal fun FileSystem.expand(
    patterns: List<String>,
    rootDir: Path,
) =
    patterns
        .map { it.expandTildeToFullPath() }
        .map {
            if (onWindowsOS) {
                // By definition the globs should use "/" as separator. Out of courtesy replace "\" with "/"
                it.replace(File.separator, "/")
            } else {
                it
            }
        }.flatMap { path -> toGlob(path, rootDir) }

private fun FileSystem.toGlob(
    path: String,
    rootDir: Path,
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
        //  Windows throws an exception when passing a wildcard (*) to Path#resolve.
        null
    }

    val safeResolvedPath =
        (
            resolvedPath
                ?: pathWithoutNegationPrefix.toPath()
            )
            ?.let { path ->
                if (onWindowsOS) {
                    // Remove drive letter (and colon) from path as this will lead to invalid globs
                    path
                        .toString()
                        .replace(this.separator, "/")
                        .substringAfter(":")
                        .prefixIfNot("**/")
                        .toPath()
                } else {
                    path
                }
            }

    val expandedGlobs = when {
        resolvedPath != null && resolvedPath.isDirectory() ->
            getDefaultPatternsForPath(safeResolvedPath)
        else -> {
            expandDoubleStarPatterns(safeResolvedPath)
        }
    }
    return expandedGlobs.map { "${negation}glob:$it" }
}

/**
 * For each double star pattern in the path, create and additional path in which the double start pattern is removed.
 * In this way a pattern like some-directory/**/*.kt will match wile files in some-directory or any of its
 * subdirectories.
 */
private fun expandDoubleStarPatterns(path: Path?): List<Path> {
    val paths = mutableListOf(path)
    val parts = path.toString().split("/")
    parts
        .filter { it == "**" }
        .forEach { doubleStarPart ->
            run {
                val expandedPath =
                    parts
                        .filter { it !== doubleStarPart }
                        .joinToString(separator = "/")
                        .toPath()
                // The original path can contain multiple double star patterns. Replace only one double start pattern
                // with an additional path patter and call recursively for remain double star patterns
                paths.addAll(expandDoubleStarPatterns(expandedPath))
            }
        }
    return paths.filterNotNull()
}

private fun getDefaultPatternsForPath(path: Path?) =
    defaultKotlinFileExtensions
        .flatMap {
            listOf(
                "$path/*.$it",
                "$path/**/*.$it",
            )
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
internal fun String.expandTildeToFullPath(): String =
    if (onWindowsOS) {
        // Windows sometimes inserts `~` into paths when using short directory names notation, e.g. `C:\Users\USERNA~1\Documents
        this
    } else {
        replaceFirst(tildeRegex, userHome)
    }

private fun String.toPath() =
    Paths.get(this)

private val onWindowsOS
    get() =
        System
            .getProperty("os.name")
            .startsWith("windows", true)

internal fun File.location(
    relative: Boolean,
) = if (relative) this.toRelativeString(File(workDir)) else this.path

/**
 * Run lint over common kotlin file or kotlin script file.
 */
internal fun lintFile(
    fileName: String,
    fileContents: String,
    ruleProviders: Set<RuleProvider>,
    editorConfigDefaults: EditorConfigDefaults,
    editorConfigOverride: EditorConfigOverride,
    editorConfigPath: String? = null,
    debug: Boolean = false,
    lintErrorCallback: (LintError) -> Unit = {},
) = KtLint.lint(
    KtLint.ExperimentalParams(
        fileName = fileName,
        text = fileContents,
        ruleProviders = ruleProviders,
        script = !fileName.endsWith(".kt", ignoreCase = true),
        editorConfigDefaults = editorConfigDefaults,
        editorConfigOverride = editorConfigOverride,
        editorConfigPath = editorConfigPath,
        cb = { e, _ ->
            lintErrorCallback(e)
        },
        debug = debug,
        isInvokedFromCli = true,
    ),
)

/**
 * Format a kotlin file or script file
 */
internal fun formatFile(
    fileName: String,
    fileContents: String,
    ruleProviders: Set<RuleProvider>,
    editorConfigDefaults: EditorConfigDefaults,
    editorConfigOverride: EditorConfigOverride,
    editorConfigPath: String?,
    debug: Boolean,
    cb: (e: LintError, corrected: Boolean) -> Unit,
): String =
    KtLint.format(
        KtLint.ExperimentalParams(
            fileName = fileName,
            text = fileContents,
            ruleProviders = ruleProviders,
            script = !fileName.endsWith(".kt", ignoreCase = true),
            editorConfigDefaults = editorConfigDefaults,
            editorConfigOverride = editorConfigOverride,
            editorConfigPath = editorConfigPath,
            cb = cb,
            debug = debug,
            isInvokedFromCli = true,
        ),
    )
