package com.pinterest.ktlint.internal

import com.github.shyiko.klob.Glob
import com.pinterest.ktlint.core.KtLint.lint
import com.pinterest.ktlint.core.KtLint.lintScript
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal val workDir: String = File(".").canonicalPath

internal fun List<String>.fileSequence(): Sequence<File> {
    val kotlinFiles = if (isEmpty()) {
        Glob.from("**/*.kt", "**/*.kts")
            .iterate(
                Paths.get(workDir),
                Glob.IterationOption.SKIP_HIDDEN
            )
    } else {
        val normalizedPatterns = map(::expandTilde).toTypedArray()
        Glob.from(*normalizedPatterns)
            .iterate(Paths.get(workDir))
    }

    return kotlinFiles
        .asSequence()
        .map(Path::toFile)
}

// a complete solution would be to implement https://www.gnu.org/software/bash/manual/html_node/Tilde-Expansion.html
// this implementation takes care only of the most commonly used case (~/)
internal fun expandTilde(path: String): String = path.replaceFirst(Regex("^~"), System.getProperty("user.home"))

internal fun File.location(
    relative: Boolean
) = if (relative) this.toRelativeString(File(workDir)) else this.path

/**
 * Run lint over common kotlin file or kotlin script file.
 */
internal fun lintFile(
    fileName: String,
    fileContent: String,
    ruleSetList: List<RuleSet>,
    userData: Map<String, String> = emptyMap(),
    lintErrorCallback: (LintError) -> Unit = {}
) {
    if (fileName.endsWith(".kt", ignoreCase = true)) {
        lint(fileContent, ruleSetList, userData, lintErrorCallback)
    } else {
        lintScript(fileContent, ruleSetList, userData, lintErrorCallback)
    }
}
