package com.github.shyiko.ktlint.io

import java.io.File

/**
 * Settings `restrictToBaseDir` and `includeChildren` to true should yield [gitignore](https://git-scm.com/docs/gitignore)
 * -mostly-compatible pattern (leading "\#", "\!", trailing "\ " were "left out"). Another difference is that if pattern
 * ends with / (say a/) it will match both directories & files (that is a and a/).
 */
data class Glob(val baseDir: String, val pattern: String, val restrictToBaseDir: Boolean = false,
    val includeChildren: Boolean = false) {

    companion object {
        fun isExclusionPattern(pattern: String) = pattern.startsWith("!")
        fun prefix(pattern: String) = pattern.removePrefix("!").removeSuffix("/").let {
            val split = it.split("/")
            val wildcardIndex = split.indexOfFirst { it.contains("*") }.let {
                if (it == -1 && split.size > 1) split.size - 1 else it
            }
            if (wildcardIndex == -1) "" else split.subList(0, wildcardIndex).joinToString("/")
        }
    }

    private val states: List<State>
    private val negate: Boolean = isExclusionPattern(pattern)

    init {
        val prefix = prefix(pattern)
        val pattern = pattern
            .removePrefix("!")
            // remove trailing space as we don't distinguish between files & directories
            // (even though gitignore does)
            .removeSuffix("/")
            // *.js -> **/*.js
            // include children -> <original pattern>/**
            .let { if (it.startsWith("*") && !it.contains("/")) "**/$it" else
                if (includeChildren) slashJoin(it, "**") else it }
            // collapse **/** (if any)
            .replace(Regex("(/[*][*]){2,}/"), "/**/").replace("**/**/", "**/").replace("/**/**", "/**")
        val canonicalBaseDir = when {
            restrictToBaseDir -> File(baseDir)
            prefix == "" -> if (pattern.startsWith("/")) File("/") else File(baseDir)
            prefix.startsWith("/") -> File(prefix) // absolute path
            else -> File(slashJoin(baseDir, prefix))
        }.canonicalPath
        val expectedPath = slashJoin(slash(canonicalBaseDir),
            if (restrictToBaseDir) pattern else pattern.substring(prefix.length))
        states = expectedPath.split('/').map {
            if (it == "**") State(Regex(".*"), true) else
                State(Regex(it.replace(Regex("([^a-zA-Z0-9 *])"), "\\\\$1").replace("*", ".*")), false)
        }
    }

    private fun slashJoin(l: String, r: String) = "${l.removeSuffix("/")}/${r.removePrefix("/")}"

    private fun matches0(path: List<String>, pathIndex: Int = 0, stateIndex: Int = 0): Boolean = when {
        pathIndex == path.size ->
            stateIndex == states.lastIndex && states[stateIndex].optional
        stateIndex != states.size && states[stateIndex].matches(path[pathIndex]) ->
            stateIndex == states.lastIndex && pathIndex == path.lastIndex ||
            (states[stateIndex].optional && (matches0(path, pathIndex, stateIndex + 1) ||
                matches0(path, pathIndex + 1, stateIndex))) ||
            matches0(path, pathIndex + 1, stateIndex + 1)
        else -> false
    }

    data class State(val regex: Regex, val optional: Boolean) {
        fun matches(input: CharSequence) = regex.matches(input)
    }

    fun matches(absolutePath: String): Boolean = matches0(absolutePath.split('/')) != negate

}
