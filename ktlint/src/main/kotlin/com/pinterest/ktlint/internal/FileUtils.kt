package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
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
import kotlin.system.exitProcess
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

internal val workDir: String = File(".").canonicalPath
private val tildeRegex = Regex("^(!)?~")

internal fun FileSystem.fileSequence(
    globs: List<String>,
    rootDir: Path = Paths.get(".").toAbsolutePath().normalize()
): Sequence<Path> {
    val result = mutableListOf<Path>()

    val (existingFiles, actualGlobs) = globs.partition {
        try {
            Files.isRegularFile(rootDir.resolve(it))
        } catch (e: InvalidPathException) {
            // Windows throws an exception when you pass a glob to Path#resolve.
            false
        }
    }
    existingFiles.mapTo(result) { rootDir.resolve(it) }

    // Return early and don't traverse the file system if all the input globs are absolute paths
    if (result.isNotEmpty() && actualGlobs.isEmpty()) {
        return result.asSequence()
    }

    val pathMatchers = if (actualGlobs.isEmpty()) {
        setOf(
            getPathMatcher("glob:**$globSeparator*.kt"),
            getPathMatcher("glob:**$globSeparator*.kts")
        )
    } else {
        actualGlobs
            .filterNot { it.startsWith("!") }
            .map {
                getPathMatcher(toGlob(it, rootDir))
            }
    }

    val negatedPathMatchers = if (actualGlobs.isEmpty()) {
        emptySet()
    } else {
        actualGlobs
            .filter { it.startsWith("!") }
            .map {
                getPathMatcher(toGlob(it.removePrefix("!"), rootDir))
            }
    }

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
                    result.add(filePath)
                }
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(
                dirPath: Path,
                dirAttr: BasicFileAttributes
            ): FileVisitResult {
                return if (Files.isHidden(dirPath)) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    FileVisitResult.CONTINUE
                }
            }
        }
    )

    return result.asSequence()
}

private fun FileSystem.isGlobAbsolutePath(glob: String): Boolean {
    val rootDirs = rootDirectories.map { it.toString() }
    return rootDirs.any { glob.removePrefix("!").startsWith(it) }
}

internal fun FileSystem.toGlob(
    pattern: String,
    rootDir: Path
): String {
    val os = System.getProperty("os.name")
    val expandedPath = if (os.startsWith("windows", true)) {
        // Windows sometimes inserts `~` into paths when using short directory names notation, e.g. `C:\Users\USERNA~1\Documents
        pattern
    } else {
        expandTilde(pattern)
    }

    val fullPath = if (isGlobAbsolutePath(expandedPath)) {
        expandedPath
    } else {
        val rootDirPath = rootDir
            .toAbsolutePath()
            .toString()
            .run {
                val normalizedPath = if (!endsWith(File.separator)) "$this${File.separator}" else this
                normalizedPath
            }
        "$rootDirPath$expandedPath"
    }
        .replace(File.separator, globSeparator)
    return "glob:$fullPath"
}

private val globSeparator: String get() {
    val os = System.getProperty("os.name")
    return when {
        os.startsWith("windows", ignoreCase = true) -> "\\\\"
        else -> "/"
    }
}

/**
 * List of paths to Java `jar` files.
 */
internal typealias JarFiles = List<String>

internal fun JarFiles.toFilesURIList() = map {
    val jarFile = File(expandTilde(it))
    if (!jarFile.exists()) {
        logger.error { "File $it does not exist" }
        exitProcess(1)
    }
    jarFile.toURI().toURL()
}

// a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
// this implementation takes care only of the most commonly used case (~/)
private fun expandTilde(path: String): String = path.replaceFirst(tildeRegex, System.getProperty("user.home"))

internal fun File.location(
    relative: Boolean
) = if (relative) this.toRelativeString(File(workDir)) else this.path

/**
 * Run lint over common kotlin file or kotlin script file.
 */
internal fun lintFile(
    fileName: String,
    fileContents: String,
    ruleSets: Iterable<RuleSet>,
    userData: Map<String, String> = emptyMap(),
    editorConfigPath: String? = null,
    debug: Boolean = false,
    lintErrorCallback: (LintError) -> Unit = {}
) = KtLint.lint(
    KtLint.ExperimentalParams(
        fileName = fileName,
        text = fileContents,
        ruleSets = ruleSets,
        userData = userData,
        script = !fileName.endsWith(".kt", ignoreCase = true),
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
    ruleSets: Iterable<RuleSet>,
    userData: Map<String, String>,
    editorConfigPath: String?,
    debug: Boolean,
    cb: (e: LintError, corrected: Boolean) -> Unit
): String =
    KtLint.format(
        KtLint.ExperimentalParams(
            fileName = fileName,
            text = fileContents,
            ruleSets = ruleSets,
            userData = userData,
            script = !fileName.endsWith(".kt", ignoreCase = true),
            editorConfigPath = editorConfigPath,
            cb = cb,
            debug = debug,
            isInvokedFromCli = true
        )
    )
