package com.pinterest.ktlint.core.internal

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import org.ec4j.core.PropertyTypeRegistry
import org.ec4j.core.Resource
import org.ec4j.core.model.EditorConfig
import org.ec4j.core.model.Version
import org.ec4j.core.parser.EditorConfigModelHandler
import org.ec4j.core.parser.EditorConfigParser
import org.ec4j.core.parser.ErrorHandler

/**
 * This class handles traversing the filetree and parsing and merging the contents of any discovered .editorconfig files
 */
class EditorConfigInternal private constructor (
    val parent: EditorConfigInternal?,
    val path: Path,
    private val data: Map<String, String>
) : Map<String, String> by data {

    companion object : EditorConfigLookup {
        override fun of(dir: String) = of(Paths.get(dir))
        override fun of(dir: Path) =
            generateSequence(locate(dir)) { seed -> locate(seed.parent.parent) } // seed.parent == .editorconfig dir
                .map { it to lazy { loadEditorconfigFile(it) } }
                .let { seq ->
                    // stop when .editorconfig with "root = true" is found, go deeper otherwise
                    var prev: Pair<Path, Lazy<Map<String, Map<String, String>>>>? = null
                    seq.takeWhile { pair ->
                        (prev?.second?.value?.get("")?.get("root")?.toBoolean()?.not() ?: true).also { prev = pair }
                    }
                }
                .toList()
                .asReversed()
                .fold(null as EditorConfigInternal?) { parent, (path, data) ->
                    EditorConfigInternal(
                        parent, path,
                        (
                            parent?.data
                                ?: emptyMap()
                            ) + flatten(data.value)
                    )
                }

        fun cached(): EditorConfigLookup = object : EditorConfigLookup {
            // todo: concurrent radix tree can potentially save a lot of memory here
            private val cache = ConcurrentHashMap<Path, CompletableFuture<EditorConfigInternal?>>()

            override fun of(dir: String) = of(Paths.get(dir))
            override fun of(dir: Path): EditorConfigInternal? {
                val cachedEditorConfig = cache[dir]
                return when {
                    cachedEditorConfig != null -> cachedEditorConfig.get()
                    else -> {
                        val future = CompletableFuture<EditorConfigInternal?>()
                        val cachedFuture = cache.putIfAbsent(dir, future)
                        if (cachedFuture == null) {
                            val editorConfigPath = dir.resolve(".editorconfig")
                            val parent = if (dir.parent != null) of(dir.parent) else null
                            try {
                                val editorConfig = if (Files.exists(editorConfigPath)) {
                                    EditorConfigInternal(
                                        parent, editorConfigPath,
                                        (
                                            parent?.data
                                                ?: emptyMap()
                                            ) + flatten(loadEditorconfigFile(editorConfigPath))
                                    )
                                } else {
                                    parent
                                }
                                future.complete(editorConfig)
                                editorConfig
                            } catch (e: Exception) {
                                future.completeExceptionally(e)
                                cache.remove(dir)
                                throw e
                            }
                        } else {
                            cachedFuture.get()
                        }
                    }
                }
            }
        }

        private fun locate(dir: Path?): Path? = when (dir) {
            null -> null
            else -> dir.resolve(".editorconfig").let {
                if (Files.exists(it)) it else locate(dir.parent)
            }
        }

        private fun flatten(data: Map<String, Map<String, String>>): Map<String, String> {
            val map = mutableMapOf<String, String>()
            val patternsToSearchFor = arrayOf("*", "*.kt", "*.kts")
            for ((sectionName, section) in data) {
                if (sectionName == "") {
                    continue
                }
                val patterns = try {
                    parseSection(sectionName)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "ktlint failed to parse .editorconfig section \"$sectionName\"" +
                            " (please report at https://github.com/shyiko/ktlint)",
                        e
                    )
                }
                if (patternsToSearchFor.any { patterns.contains(it) }) {
                    map.putAll(section.toMap())
                }
            }
            return map.toSortedMap()
        }

        private fun parseEditorconfigFile(path: Path): EditorConfig {
            val parser = EditorConfigParser.builder().build()
            val handler = EditorConfigModelHandler(PropertyTypeRegistry.default_(), Version.CURRENT)

            parser.parse(
                Resource.Resources.ofPath(path, StandardCharsets.UTF_8),
                handler,
                ErrorHandler.THROW_SYNTAX_ERRORS_IGNORE_OTHERS
            )
            return handler.editorConfig
        }

        private fun loadEditorconfigFile(editorConfigFile: Path): Map<String, Map<String, String>> {
            val editorConfig = parseEditorconfigFile(editorConfigFile)

            var mapRepresentation = editorConfig.sections
                .associate { section ->
                    section.glob.toString() to section
                        .properties
                        .mapValues { entry ->
                            entry.value.sourceValue
                        }
                }

            if (editorConfig.isRoot) {
                mapRepresentation = mapRepresentation + ("" to mapOf("root" to true.toString()))
            }

            return mapRepresentation
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
                mutableListOf<String>().also { this.collect0(0, arrayOfNulls(this.size), it) }
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

interface EditorConfigLookup {
    fun of(dir: String): EditorConfigInternal?
    fun of(dir: Path): EditorConfigInternal?
}
