package com.pinterest.ktlint.cli.internal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.logger.api.setDefaultLoggerModifier
import io.github.oshai.kotlinlogging.DelegatingKLogger
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Deque
import java.util.LinkedList
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.relativeToOrSelf
import kotlin.system.measureTimeMillis

private val LOGGER =
    KotlinLogging
        .logger {}
        .setDefaultLoggerModifier { it.level = Level.TRACE }
        .initKtLintKLogger()

private var KLogger.level: Level?
    get() = underlyingLogger()?.level
    set(value) {
        underlyingLogger()?.level = value
    }

private fun KLogger.underlyingLogger(): Logger? =
    @Suppress("UNCHECKED_CAST")
    (this as? DelegatingKLogger<Logger>)
        ?.underlyingLogger

private val ROOT_DIR_PATH: Path = Paths.get("").toAbsolutePath()

private val TILDE_REGEX = Regex("^(!)?~")
private const val NEGATION_PREFIX = "!"

private val USER_HOME = System.getProperty("user.home")

private val DEFAULT_KOTLIN_FILE_EXTENSIONS = setOf("kt", "kts")
internal val DEFAULT_PATTERNS = DEFAULT_KOTLIN_FILE_EXTENSIONS.map { "**/*.$it" }

/**
 * Transform the [patterns] to a sequence of files. Each element in [patterns] can be a glob, a file or directory path
 * relative to the [rootDir] or an absolute file or directory path.
 */
internal fun FileSystem.fileSequence(
    patterns: List<String>,
    rootDir: Path = Paths.get(".").toAbsolutePath().normalize(),
): Sequence<Path> {
    val result = mutableListOf<Path>()

    val (existingFiles, patternsExclusiveExistingFiles) =
        patterns.partition {
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

    val negatedPathMatchers =
        globs
            .filter { it.startsWith(NEGATION_PREFIX) }
            .map { getPathMatcher(it.removePrefix(NEGATION_PREFIX)) }

    val includeGlobs =
        globs
            .filterNot { it.startsWith(NEGATION_PREFIX) }
            .let { includeMatchers ->
                if (negatedPathMatchers.isNotEmpty() && includeMatchers.isEmpty()) {
                    LOGGER.info {
                        "A negate pattern is specified without an include pattern. As default, the include patterns '$DEFAULT_PATTERNS' " +
                            "are used."
                    }
                    includeMatchers.plus(
                        expand(DEFAULT_PATTERNS, rootDir),
                    )
                } else {
                    includeMatchers
                }
            }
    var commonRootDir = rootDir
    patterns.forEach { pattern ->
        try {
            val patternDir =
                rootDir
                    .resolve(pattern)
                    .normalize()
            commonRootDir = commonRootDir.findCommonParentDir(patternDir)
        } catch (e: InvalidPathException) {
            // Windows throws an exception when you pass a glob to Path#resolve.
        }
    }

    val pathMatchers = includeGlobs.map { getPathMatcher(it) }

    LOGGER.debug { "Start walkFileTree from directory: '$commonRootDir'" }
    val duration =
        measureTimeMillis {
            Files.walkFileTree(
                commonRootDir,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(
                        filePath: Path,
                        fileAttrs: BasicFileAttributes,
                    ): FileVisitResult {
                        val path =
                            if (onWindowsOS) {
                                Paths
                                    .get(
                                        filePath
                                            .absolutePathString()
                                            .replace(File.separatorChar, '/'),
                                    ).also {
                                        if (it != filePath) {
                                            LOGGER.trace { "On WindowsOS transform '$filePath' to '$it'" }
                                        }
                                    }
                            } else {
                                filePath
                            }
                        if (negatedPathMatchers.none { it.matches(path) } &&
                            pathMatchers.any { it.matches(path) }
                        ) {
                            LOGGER.trace { "- File: $path: Include as it matches patterns ${pathMatchers.filter { it.matches(path) }}" }
                            result.add(path)
                        } else {
                            LOGGER.trace { "- File: $path: Ignore" }
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(
                        dirPath: Path,
                        dirAttr: BasicFileAttributes,
                    ): FileVisitResult =
                        if (Files.isHidden(dirPath)) {
                            if (dirPath == commonRootDir) {
                                LOGGER.trace { "- Dir: $dirPath: Traverse started from hidden directory" }
                                FileVisitResult.CONTINUE
                            } else {
                                LOGGER.trace { "- Dir: $dirPath: Ignore traversal of hidden directory" }
                                FileVisitResult.SKIP_SUBTREE
                            }
                        } else {
                            LOGGER.trace { "- Dir: $dirPath: Traverse" }
                            FileVisitResult.CONTINUE
                        }
                },
            )
        }
    LOGGER.debug { "Discovered ${result.count()} files to be processed in $duration ms" }

    return result.asSequence()
}

private fun Path.findCommonParentDir(path: Path): Path =
    when {
        path.startsWith(this) ->
            this

        startsWith(path) ->
            path

        else ->
            this@findCommonParentDir.findCommonParentDir(path.parent)
    }

private fun FileSystem.expand(
    patterns: List<String>,
    rootDir: Path,
) = patterns
    .mapNotNull {
        if (onWindowsOS) {
            it.normalizeWindowsPattern()
        } else {
            it
        }
    }.map { it.expandTildeToFullPath() }
    .map {
        if (onWindowsOS) {
            // By definition the globs should use "/" as separator. Out of courtesy replace "\" with "/"
            it
                .replace(File.separator, "/")
                .also { transformedPath ->
                    if (it != transformedPath) {
                        LOGGER.trace { "On WindowsOS transform '$it' to '$transformedPath'" }
                    }
                }
        } else {
            it
        }
    }.flatMap { path -> toGlob(path, rootDir) }

private fun FileSystem.toGlob(
    path: String,
    rootDir: Path,
): List<String> {
    val negation =
        if (path.startsWith(NEGATION_PREFIX)) {
            NEGATION_PREFIX
        } else {
            ""
        }
    val pathWithoutNegationPrefix =
        path
            .removePrefix(NEGATION_PREFIX)
    val expandedPatterns =
        try {
            val resolvedPath =
                rootDir
                    .resolve(pathWithoutNegationPrefix)
                    .normalize()
            if (resolvedPath.isDirectory()) {
                resolvedPath
                    .expandPathToDefaultPatterns()
                    .also {
                        LOGGER.trace { "Expanding resolved directory path '$resolvedPath' to patterns: [$it]" }
                    }
            } else {
                resolvedPath
                    .pathString
                    .expandDoubleStarPatterns()
                    .also {
                        LOGGER.trace { "Expanding resolved path '$resolvedPath` to patterns: [$it]" }
                    }
            }
        } catch (e: InvalidPathException) {
            if (onWindowsOS) {
                //  Windows throws an exception when passing a wildcard (*) to Path#resolve.
                pathWithoutNegationPrefix
                    .expandDoubleStarPatterns()
                    .also {
                        LOGGER.trace { "On WindowsOS: expanding unresolved path '$pathWithoutNegationPrefix` to patterns: [$it]" }
                    }
            } else {
                emptyList()
            }
        }
    return expandedPatterns
        .map { originalPattern ->
            if (onWindowsOS) {
                originalPattern
                    // Replace "\" with "/"
                    .replace(this.separator, "/")
                    // Remove drive letter (and colon) from path as this will lead to invalid globs and replace it with a double
                    // star pattern. Technically this is not functionally identical as the pattern could match on multiple drives.
                    .substringAfter(":")
                    .removePrefix("/")
                    .prefixIfNot("**/")
                    .also { transformedPattern ->
                        if (transformedPattern != originalPattern) {
                            LOGGER.trace { "On WindowsOS, transform '$originalPattern' to '$transformedPattern'" }
                        }
                    }
            } else {
                originalPattern
            }
        }.map { "${negation}glob:$it" }
}

/**
 * For each double star pattern in the path, create an additional path in which the double star pattern is removed.
 * In this way a pattern like some-directory/**/*.kt will match while files in some-directory or any of its
 * subdirectories.
 */
private fun String?.expandDoubleStarPatterns(): Set<String> {
    val paths = mutableSetOf(this)
    val parts = this?.split("/").orEmpty()
    parts
        .filter { it == "**" }
        .filterNot {
            // When the pattern ends with a double star, it might not be expanded. According to the https://git-scm.com/docs/gitignore a
            // trailing "**" matches any file in the directory.
            // Given pattern "**/Test*/**" the file "src/Foo/TestFoo.kt" should not be matched, and file "src/TestFoo/FooTest.kt" is
            // matched. If the original pattern would be expanded with additional pattern "**/Test*" then both files would have been
            // matched.
            it === parts.last()
        }.forEach { doubleStarPart ->
            run {
                // The original path can contain multiple double star patterns. Replace only one double star pattern
                // with an additional path pattern and call recursively for remaining double star patterns
                val expandedPath =
                    parts
                        .filter { it !== doubleStarPart }
                        .joinToString(separator = "/")
                paths.addAll(expandedPath.expandDoubleStarPatterns())
            }
        }
    return paths.filterNotNull().toSet()
}

private fun String?.normalizeWindowsPattern() =
    if (onWindowsOS) {
        val parts: Deque<String> = LinkedList()
        // Replace "\" with "/"
        this
            ?.replace("\\", "/")
            ?.split("/")
            ?.filterNot {
                // Reference to current directory can simply be ignored
                it == "."
            }?.forEach {
                if (it == "..") {
                    // Whenever the parent directory reference follows a part not containing a wildcard, then the parent
                    // reference and the preceding element can be ignored. In other cases, the result pattern can not be
                    // cleaned. If that pattern would be transformed to a glob then the result regular expression of
                    // that glob results in a pattern that will never be matched as the ".." reference will not occur in
                    // the filepath that is being checked with the regular expression.
                    if (parts.isEmpty()) {
                        LOGGER.warn {
                            "On WindowsOS the pattern '$this' can not be used as it refers to a path outside of the current directory"
                        }
                        return@normalizeWindowsPattern null
                    } else if (parts.peekLast().contains('*')) {
                        LOGGER.warn {
                            "On WindowsOS the pattern '$this' can not be used as '/..' follows the wildcard pattern ${parts.peekLast()}"
                        }
                        return@normalizeWindowsPattern null
                    } else {
                        parts.removeLast()
                    }
                } else {
                    parts.addLast(it)
                }
            }
        parts.joinToString(separator = "/")
    } else {
        this
    }

private fun Path.expandPathToDefaultPatterns() =
    DEFAULT_KOTLIN_FILE_EXTENSIONS
        .flatMap {
            listOf(
                "$this/*.$it",
                "$this/**/*.$it",
            )
        }

// a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
// this implementation takes care only of the most commonly used case (~/)
internal fun String.expandTildeToFullPath(): String =
    if (onWindowsOS) {
        // Windows sometimes inserts `~` into paths when using short directory names notation, e.g. `C:\Users\USERNA~1\Documents
        this
    } else {
        replaceFirst(TILDE_REGEX, USER_HOME)
            .also {
                if (it != this) {
                    LOGGER.trace { "On non-WindowsOS expand '$this' to '$it'" }
                }
            }
    }

private val onWindowsOS
    get() =
        System
            .getProperty("os.name")
            .startsWith("windows", true)

/**
 * Gets the relative route of the path. Also adjusts the slashes for uniformity between file systems.
 */
internal fun File.location(relative: Boolean) =
    if (relative) {
        this
            .toPath()
            .relativeToOrSelf(ROOT_DIR_PATH)
            .pathString
            .replace(File.separatorChar, '/')
    } else {
        this
            .path
            .replace(File.separatorChar, '/')
    }
