package com.github.shyiko.ktlint.internal

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

class EditorConfig private constructor (
    val parent: EditorConfig?,
    val path: Path,
    private val data: Map<String, String>
) : Map<String, String> by data {

    companion object {

        fun of(dir: String) =
            of(Paths.get(dir))
        fun of(dir: Path) =
            generateSequence(locate(dir)) { seed -> locate(seed.parent.parent) } // seed.parent == .editorconfig dir
                .map { it to lazy { load(it) } }
                .let { seq ->
                    // stop when .editorconfig with "root = true" is found, go deeper otherwise
                    var prev: Pair<Path, Lazy<Map<String, Map<String, String>>>>? = null
                    seq.takeWhile { pair ->
                        (prev?.second?.value?.get("")?.get("root")?.toBoolean()?.not() ?: true).also { prev = pair }
                    }
                }
                .toList()
                .asReversed()
                .fold(null as EditorConfig?) { parent, (path, data) ->
                    EditorConfig(parent, path, (parent?.data ?: emptyMap()) + flatten(data.value))
                }

        private fun locate(dir: Path?): Path? = when (dir) {
            null -> null
            else -> dir.resolve(".editorconfig").let {
                if (Files.exists(it)) it else locate(dir.parent)
            }
        }

        private fun flatten(data: LinkedHashMap<String, Map<String, String>>): Map<String, String> {
            val map = mutableMapOf<String, String>()
            val patternsToSearchFor = arrayOf("*", "*.kt", "*.kts")
            for ((sectionName, section) in data) {
                if (sectionName == "") {
                    continue
                }
                val patterns = try {
                    parseSection(sectionName.substring(1, sectionName.length - 1))
                } catch (e: Exception) {
                    throw RuntimeException("ktlint failed to parse .editorconfig section \"$sectionName\"" +
                        " (please report at https://github.com/shyiko/ktlint)", e)
                }
                if (patternsToSearchFor.any { patterns.contains(it) }) {
                    map.putAll(section.toMap())
                }
            }
            return map.toSortedMap()
        }

        private fun load(path: Path) =
            linkedMapOf<String, Map<String, String>>().also { map ->
                object : Properties() {

                    private var section: MutableMap<String, String>? = null

                    override fun put(key: Any, value: Any): Any? {
                        val sectionName = (key as String).trim()
                        if (sectionName.startsWith('[') && sectionName.endsWith(']') && value == "") {
                            section = mutableMapOf<String, String>().also { map.put(sectionName, it) }
                        } else {
                            val section = section
                                ?: mutableMapOf<String, String>().also { section = it; map.put("", it) }
                            section[key] = value.toString()
                        }
                        return null
                    }
                }.load(ByteArrayInputStream(Files.readAllBytes(path)))
            }

        internal fun parseSection(sectionName: String): List<String> {
            val result = mutableListOf<String>()
            fun List<List<String>>.collect0(i: Int = 0, str: Array<String?>, acc: MutableList<String>) {
                if (i == str.size) {
                    acc.add(str.joinToString(""))
                    return
                }
                for (k in 0 until this[i].size) {
                    str[i] = this[i][k]
                    collect0(i + 1, str, acc)
                }
            }
            // [["*.kt"], ["", "s"], ["~"]] -> [*.kt~, *.kts~]
            fun List<List<String>>.collect(): List<String> =
                mutableListOf<String>().also { this.collect0(0, arrayOfNulls<String>(this.size), it) }
            val chunks: MutableList<MutableList<String>> = mutableListOf()
            chunks.add(mutableListOf())
            var l = 0
            var r = 0
            var partOfBraceExpansion = false
            for (c in sectionName) {
                when (c) {
                    ',' -> {
                        chunks.last().add(sectionName.substring(l, r))
                        l = r + 1
                        if (!partOfBraceExpansion) {
                            result += chunks.collect()
                            chunks.clear()
                            chunks.add(mutableListOf())
                        }
                    }
                    '{', '}' -> {
                        if (partOfBraceExpansion == (c == '}')) {
                            chunks.last().add(sectionName.substring(l, r))
                            l = r + 1
                            chunks.add(mutableListOf())
                            partOfBraceExpansion = !partOfBraceExpansion
                        }
                    }
                }
                r++
            }
            chunks.last().add(sectionName.substring(l, r))
            result += chunks.collect()
            return result
        }
    }
}
