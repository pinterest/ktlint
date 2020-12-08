package com.pinterest.ktlint.internal

import com.github.shyiko.klob.Glob
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

internal val workDir: String = File(".").canonicalPath

internal fun List<String>.fileSequence(): Sequence<File> {
    val glob = if (isEmpty()) {
        Glob.from("**/*.kt", "**/*.kts")
    } else {
        Glob.from(*map(::expandTilde).toTypedArray())
    }

    return glob
        .iterate(
            Paths.get(workDir),
            Glob.IterationOption.SKIP_HIDDEN
        )
        .asSequence()
        .map(Path::toFile)
}

/**
 * List of paths to Java `jar` files.
 */
internal typealias JarFiles = List<String>

internal fun JarFiles.toFilesURIList() = map {
    val jarFile = File(expandTilde(it))
    if (!jarFile.exists()) {
        println("Error: $it does not exist")
        exitProcess(1)
    }
    jarFile.toURI().toURL()
}

// a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
// this implementation takes care only of the most commonly used case (~/)
private fun expandTilde(path: String): String = path.replaceFirst(Regex("^~"), System.getProperty("user.home"))

internal fun File.location(
    relative: Boolean
) = if (relative) this.toRelativeString(File(workDir)) else this.path

/**
 * Run lint over common kotlin file or kotlin script file.
 */
internal fun lintFile(
    fileName: String,
    fileContents: String,
    ruleSets: List<RuleSet>,
    userData: Map<String, String> = emptyMap(),
    editorConfigPath: String? = null,
    debug: Boolean = false,
    lintErrorCallback: (LintError) -> Unit = {}
) {
    KtLint.lint(
        KtLint.Params(
            fileName = fileName,
            text = fileContents,
            ruleSets = ruleSets,
            userData = userData,
            script = !fileName.endsWith(".kt", ignoreCase = true),
            editorConfigPath = editorConfigPath,
            cb = { e, _ ->
                lintErrorCallback(e)
            },
            debug = debug
        )
    )
}

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
        KtLint.Params(
            fileName = fileName,
            text = fileContents,
            ruleSets = ruleSets,
            userData = userData,
            script = !fileName.endsWith(".kt", ignoreCase = true),
            editorConfigPath = editorConfigPath,
            cb = cb,
            debug = debug
        )
    )
