package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.emptyEditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Path
import mu.KotlinLogging
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.PropertyTypeRegistry
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.Property
import org.ec4j.core.model.Version

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Loads default `.editorconfig` and ktlint specific properties for files.
 *
 * Contains internal in-memory cache to speedup lookup.
 */
public class EditorConfigLoader(
    private val fs: FileSystem
) {
    private val cache = ThreadSafeEditorConfigCache()

    /**
     * Loads applicable properties from `.editorconfig`s for given file.
     *
     * @param filePath path to file that would be checked.
     * @param isStdIn indicates that checked content comes from input.
     * Setting this to `true` overrides [filePath] and uses `.kt` pattern to load properties
     * from current folder `.editorconfig` files.
     * @param alternativeEditorConfig alternative to current [filePath] location where `.editorconfig` files should be
     * looked up
     * @param rules set of [Rule]s linting the file
     * @param editorConfigOverride map of values to add/replace values that were loaded from `.editorconfig` files
     * @param debug pass `true` to enable some additional debug output
     *
     * @return all possible loaded properties applicable to given file.
     * In case file extensions is not one of [SUPPORTED_FILES] or [filePath] is `null`
     * method will immediately return empty map.
     */
    public fun loadPropertiesForFile(
        filePath: Path?,
        isStdIn: Boolean = false,
        alternativeEditorConfig: Path? = null,
        rules: Set<Rule>,
        editorConfigOverride: EditorConfigOverride = emptyEditorConfigOverride,
        debug: Boolean = false
    ): EditorConfigProperties {
        if (!isStdIn &&
            (filePath == null || SUPPORTED_FILES.none { filePath.toString().endsWith(it) })
        ) {
            return editorConfigOverride
                .properties
                .map { (property, value) ->
                    property.type.name to Property.builder()
                        .name(property.type.name)
                        .type(property.type)
                        .value(value)
                        .build()
                }.toMap()
        }

        val propService = createLoaderService(rules)

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

        return propService
            .queryProperties(
                Resource.Resources.ofPath(normalizedFilePath, StandardCharsets.UTF_8)
            )
            .properties
            .also { loaded ->
                editorConfigOverride
                    .properties
                    .forEach {
                        loaded[it.key.type.name] = Property.builder()
                            .name(it.key.type.name)
                            .type(it.key.type)
                            .value(it.value)
                            .build()
                    }
            }
            .also {
                logger.trace {
                    it
                        .map { entry -> "${entry.key}: ${entry.value.sourceValue}" }
                        .joinToString(
                            prefix = "Resolving .editorconfig files for $normalizedFilePath file path:\n\t",
                            separator = ", "
                        )
                }
            }
    }

    /**
     * Trims used in-memory cache.
     */
    public fun trimMemory() {
        cache.clear()
    }

    private fun createLoaderService(
        rules: Set<Rule>
    ): ResourcePropertiesService {
        val propertyTypeRegistry = PropertyTypeRegistry.builder()
            .defaults()
            .apply {
                rules
                    .filterIsInstance<UsesEditorConfigProperties>()
                    .flatMap(UsesEditorConfigProperties::editorConfigProperties)
                    .forEach { prop ->
                        type(prop.type)
                    }
            }
            .build()
        val editorConfigLoader = EditorConfigLoader.of(Version.CURRENT, propertyTypeRegistry)
        return ResourcePropertiesService.builder()
            .keepUnset(true)
            .cache(cache)
            .loader(editorConfigLoader)
            .build()
    }

    public companion object {
        /**
         * List of file extensions, editorconfig lookup will be performed.
         */
        internal val SUPPORTED_FILES = arrayOf(
            ".kt",
            ".kts"
        )

        /**
         * Converts loaded [EditorConfigProperties] values into string representation.
         *
         * @return map of key as string and value as string property representation
         */
        public fun EditorConfigProperties.convertToRawValues(): Map<String, String> {
            return if (isEmpty()) {
                emptyMap()
            } else {
                mapValues {
                    if (it.value.isUnset) "unset" else it.value.sourceValue
                }
            }
        }
    }
}
