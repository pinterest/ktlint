package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Path
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.PropertyTypeRegistry
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.Version

/**
 * Map contains [UsesEditorConfigProperties.EditorConfigProperty] and related
 * [PropertyType.PropertyValue] entries to add/replace loaded from `.editorconfig` files values.
 */
@FeatureInAlphaState
public typealias EditorConfigOverridesMap =
    Map<UsesEditorConfigProperties.EditorConfigProperty<*>, PropertyType.PropertyValue<*>>

/**
 * Loads default `.editorconfig` and ktlint specific properties for files.
 *
 * Contains internal in-memory cache to speedup lookup.
 */
@OptIn(FeatureInAlphaState::class)
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
     * @param loadedValuesOverride map of values to add/replace values that were loaded from `.editorconfig` files
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
        loadedValuesOverride: EditorConfigOverridesMap = emptyMap(),
        debug: Boolean = false
    ): EditorConfigProperties {
        if (!isStdIn &&
            (filePath == null || SUPPORTED_FILES.none { filePath.toString().endsWith(it) })
        ) {
            return emptyMap()
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

        if (debug) println("Resolving .editorconfig files for $normalizedFilePath file path")

        return propService
            .queryProperties(
                Resource.Resources.ofPath(normalizedFilePath, StandardCharsets.UTF_8)
            )
            .properties
            .also { loaded ->
                loadedValuesOverride.forEach {
                    loaded[it.key.type.name] = Property.builder()
                        .name(it.key.type.name)
                        .type(it.key.type)
                        .value(it.value)
                        .build()
                }
            }
            .also {
                if (debug) {
                    val editorConfigValues = it
                        .map { entry ->
                            "${entry.key}: ${entry.value.sourceValue}"
                        }
                        .joinToString(
                            separator = ", "
                        )
                    println("Loaded .editorconfig: [$editorConfigValues]")
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
