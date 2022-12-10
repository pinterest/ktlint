package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.internal.ThreadSafeEditorConfigCache.Companion.THREAD_SAFE_EDITOR_CONFIG_CACHE
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import mu.KotlinLogging
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.PropertyTypeRegistry
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.Version
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

@Deprecated("Marked for removal of public API in KtLint 0.49. Please raise an issue if you use this function.")
/**
 * Loader for `.editorconfig` properties for files on [fileSystem].
 */
public class EditorConfigLoader(
    private val fileSystem: FileSystem = FileSystems.getDefault(),
) {
    /**
     * Loads properties used by [Rule]s from the `.editorconfig` file on given [filePath]. When [filePath] is null, the
     * properties for the ".kt" pattern in the current directory are loaded. The '.editorconfig' files on the [filePath]
     * are read starting from the [filePath] upwards until an '.editorconfig' file is found in which the property "root"
     * is found with value "true" or until the root of the filesystem is reached.
     *
     * Properties specified in [editorConfigDefaults] will be used in case the property was not found in any
     * '.editorconfig' on [filePath]. If the property is not specified in [editorConfigDefaults] then the default value
     * as specified in the property definition [UsesEditorConfigProperties.EditorConfigProperty] is used.
     *
     * Properties specified in [editorConfigOverride] take precedence above any other '.editorconfig' file on [filePath]
     * or default value.
     */
    internal fun load(
        filePath: Path?,
        rules: Set<Rule> = emptySet(),
        editorConfigDefaults: EditorConfigDefaults = EMPTY_EDITOR_CONFIG_DEFAULTS,
        editorConfigOverride: EditorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
    ): EditorConfigProperties {
        val normalizedFilePath = filePath ?: defaultFilePath()

        return createLoaderService(rules, editorConfigDefaults)
            .queryProperties(normalizedFilePath.resource())
            .properties
            .also { loaded ->
                if (loaded[TAB_WIDTH_PROPERTY_NAME]?.sourceValue == loaded[INDENT_SIZE_PROPERTY.name]?.sourceValue &&
                    editorConfigOverride.properties[INDENT_SIZE_PROPERTY] != null
                ) {
                    // The tab_width property can not be overridden via the editorConfigOverride. So if it has been
                    // set to the same value as the indent_size property then keep its value in sync with that
                    // property.
                    loaded[TAB_WIDTH_PROPERTY_NAME] = Property
                        .builder()
                        .name(TAB_WIDTH_PROPERTY_NAME)
                        .type(PropertyType.tab_width)
                        .value(editorConfigOverride.properties[INDENT_SIZE_PROPERTY]?.source)
                        .build()
                }
                editorConfigOverride
                    .properties
                    .forEach {
                        loaded[it.key.name] = property(it.key, it.value)
                    }
            }.also { editorConfigProperties ->
                LOGGER.debug { editorConfigProperties.prettyPrint(normalizedFilePath) }
            }
    }

    private fun MutableMap<String, Property>.prettyPrint(
        normalizedFilePath: Path?,
    ) = map { entry -> "${entry.key}: ${entry.value.sourceValue}" }
        .joinToString(
            prefix = "Resolving .editorconfig files for $normalizedFilePath file path:\n\t",
            separator = "\n\t",
        )

    private fun Path?.resource() =
        Resource.Resources.ofPath(this, StandardCharsets.UTF_8)

    private fun property(
        property: EditorConfigProperty<*>,
        value: PropertyType.PropertyValue<*>,
    ) = Property
        .builder()
        .name(property.name)
        .type(property.type)
        .value(value)
        .build()

    private fun defaultFilePath() =
        fileSystem
            .getPath(".")
            .toAbsolutePath()
            .resolve(SUPPORTED_FILES.first())

    private fun createLoaderService(
        rules: Set<Rule>,
        editorConfigDefaults: EditorConfigDefaults,
    ) = createResourcePropertiesService(
        editorConfigLoader(rules),
        editorConfigDefaults,
    )

    private fun createResourcePropertiesService(
        editorConfigLoader: EditorConfigLoader,
        editorConfigDefaults: EditorConfigDefaults,
    ) =
        ResourcePropertiesService.builder()
            .keepUnset(true)
            .cache(THREAD_SAFE_EDITOR_CONFIG_CACHE)
            .loader(editorConfigLoader)
            .applyIf(editorConfigDefaults != EMPTY_EDITOR_CONFIG_DEFAULTS) {
                defaultEditorConfigs(editorConfigDefaults.value)
            }.build()

    private fun editorConfigLoader(rules: Set<Rule>) =
        EditorConfigLoader
            .of(Version.CURRENT, propertyTypeRegistry(rules))

    private fun propertyTypeRegistry(rules: Set<Rule>) =
        PropertyTypeRegistry.builder()
            .defaults()
            .apply {
                rules
                    .filterIsInstance<UsesEditorConfigProperties>()
                    .flatMap(UsesEditorConfigProperties::editorConfigProperties)
                    .forEach { editorConfigProperty ->
                        type(editorConfigProperty.type)
                    }
            }
            .build()

    public companion object {
        /**
         * List of file extensions, editorconfig lookup will be performed.
         */
        internal val SUPPORTED_FILES = arrayOf(
            ".kt",
            ".kts",
        )

        private const val TAB_WIDTH_PROPERTY_NAME = "tab_width"
    }
}
