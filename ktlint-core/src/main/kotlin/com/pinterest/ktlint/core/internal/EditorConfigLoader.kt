package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint.STDIN_FILE
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService

/**
 * Loads default `.editorconfig` and ktlint specific properties for files.
 *
 * Contains internal in-memory cache to speedup lookup.
 */
class EditorConfigLoader {
    private val cache = ThreadSafeEditorConfigCache()
    private val editorConfigLoader = EditorConfigLoader.default_()
    private val propService = ResourcePropertiesService.builder()
        .cache(cache)
        .loader(editorConfigLoader)
        .build()

    /**
     * Loads applicable properties from `.editorconfig`s for given file.
     *
     * @param filePath path to file that would be checked.
     * Could end with [STDIN_FILE] to indicate that _stdin_ would be checked
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
        alternativeEditorConfig: Path? = null,
        debug: Boolean = false
    ): Map<String, String> {
        if (filePath == null || SUPPORTED_FILES.none { filePath.toString().endsWith(it) }) {
            return emptyMap()
        }

        val normalizedFilePath = when {
            filePath.endsWith(STDIN_FILE) ->
                Paths
                    .get(".")
                    .toAbsolutePath()
                    .resolve("stdin${SUPPORTED_FILES.first()}")
            alternativeEditorConfig != null ->
                alternativeEditorConfig
                    .toAbsolutePath()
                    .resolve("${filePath.last()}")
            else -> filePath
        }

        if (debug) println("Resolving .editorconfig files for $normalizedFilePath file path")

        return propService
            .queryProperties(
                Resource.Resources.ofPath(normalizedFilePath, StandardCharsets.UTF_8)
            )
            .properties
            .mapValues { it.value.sourceValue }
            .run {
                if (!filePath.endsWith(STDIN_FILE)) {
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
            ".kts",
            STDIN_FILE
        )
    }
}
