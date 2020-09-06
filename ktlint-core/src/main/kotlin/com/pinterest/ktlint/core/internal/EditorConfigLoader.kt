package com.pinterest.ktlint.core.internal

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Path
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService

/**
 * Loads default `.editorconfig` and ktlint specific properties for files.
 *
 * Contains internal in-memory cache to speedup lookup.
 */
class EditorConfigLoader(
    private val fs: FileSystem
) {
    private val cache = ThreadSafeEditorConfigCache()
    private val editorConfigLoader = EditorConfigLoader.default_()
    private val propService = ResourcePropertiesService.builder()
        .keepUnset(true)
        .cache(cache)
        .loader(editorConfigLoader)
        .build()

    /**
     * Loads applicable properties from `.editorconfig`s for given file.
     *
     * @param filePath path to file that would be checked.
     * @param isStdIn indicates that checked content comes from input.
     * Setting this to `true` overrides [filePath] and uses `.kt` pattern to load properties
     * from current folder `.editorconfig` files.
     * @param alternativeEditorConfig alternative to current [filePath] location where `.editorconfig` files should be
     * looked up
     * @param debug pass `true` to enable some additional debug output
     *
     * @return all possible loaded properties applicable to given file.
     * In case file extensions is not one of [SUPPORTED_FILES] or [filePath] is `null`
     * method will immediately return empty map.
     */
    fun loadPropertiesForFile(
        filePath: Path?,
        isStdIn: Boolean = false,
        alternativeEditorConfig: Path? = null,
        debug: Boolean = false
    ): Map<String, String> {
        if (!isStdIn &&
            (filePath == null || SUPPORTED_FILES.none { filePath.toString().endsWith(it) })
        ) {
            return emptyMap()
        }

        val normalizedFilePath = when {
            alternativeEditorConfig != null -> {
                val editorconfigFilePath = if (isStdIn) "stdin${SUPPORTED_FILES.first()}" else filePath!!.last()
                alternativeEditorConfig
                    .toAbsolutePath()
                    .resolve("$editorconfigFilePath")
            }
            isStdIn ->
                fs
                    .getPath(".")
                    .toAbsolutePath()
                    .resolve("stdin${SUPPORTED_FILES.first()}")
            else -> filePath
        }

        if (debug) println("Resolving .editorconfig files for $normalizedFilePath file path")

        return propService
            .queryProperties(
                Resource.Resources.ofPath(normalizedFilePath, StandardCharsets.UTF_8)
            )
            .properties
            .mapValues {
                if (it.value.isUnset) "unset" else it.value.sourceValue
            }
            .also {
                if (debug) {
                    val editorConfigValues = it
                        .map { entry ->
                            "${entry.key}: ${entry.value}"
                        }
                        .joinToString(
                            separator = ", "
                        )
                    println("Loaded .editorconfig: [$editorConfigValues]")
                }
            }
            .run {
                if (!isStdIn) {
                    plus(FILE_PATH_PROPERTY to filePath.toString())
                } else {
                    this
                }
            }
    }

    /**
     * Trims used in-memory cache.
     */
    fun trimMemory() {
        cache.clear()
    }

    companion object {
        internal const val FILE_PATH_PROPERTY = "file_path"
        /**
         * List of file extensions, editorconfig lookup will be performed.
         */
        internal val SUPPORTED_FILES = arrayOf(
            ".kt",
            ".kts"
        )
    }
}
