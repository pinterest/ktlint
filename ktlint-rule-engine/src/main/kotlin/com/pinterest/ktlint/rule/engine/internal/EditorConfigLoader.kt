package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults.Companion.EMPTY_EDITOR_CONFIG_DEFAULTS
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.END_OF_LINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.internal.ThreadSafeEditorConfigCache.Companion.THREAD_SAFE_EDITOR_CONFIG_CACHE
import mu.KotlinLogging
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.PropertyTypeRegistry
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.Version
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

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
     * as specified in the property definition [EditorConfigProperty] is used.
     *
     * Properties specified in [editorConfigOverride] take precedence above any other '.editorconfig' file on [filePath]
     * or default value.
     */
    internal fun load(
        filePath: Path?,
        rules: Set<Rule> = emptySet(),
        editorConfigDefaults: EditorConfigDefaults = EMPTY_EDITOR_CONFIG_DEFAULTS,
        editorConfigOverride: EditorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
        ignoreEditorConfigOnFileSystem: Boolean = false,
    ): EditorConfig {
        val editorConfigProperties: MutableMap<String, Property> =
            if (ignoreEditorConfigOnFileSystem) {
                // The editorConfigDefaults will only be taken into account whenever the ec4j library is queried to retrieve the editor
                // config for an extension at an existing path. This flag is supposed to be enabled while running unit tests which should
                // not be affected by a '.editorconfig' file which exists on the filesystem where the tests are executed. For now, it is
                // assumed that the file system does not contain such file in the root of the file system.
                createLoaderService(rules, editorConfigDefaults)
                    .queryProperties(Paths.get("/.kt").resource())
                    .properties
            } else {
                val normalizedFilePath = filePath ?: defaultFilePath()
                createLoaderService(rules, editorConfigDefaults)
                    .queryProperties(normalizedFilePath.resource())
                    .properties
            }
        return editorConfigProperties
            .also { properties ->
                if (properties[TAB_WIDTH_PROPERTY_NAME]?.sourceValue == properties[INDENT_SIZE_PROPERTY.name]?.sourceValue &&
                    editorConfigOverride.properties[INDENT_SIZE_PROPERTY] != null
                ) {
                    // The tab_width property can not be overridden via the editorConfigOverride. So if it has been
                    // set to the same value as the indent_size property then keep its value in sync with that
                    // property.
                    properties[TAB_WIDTH_PROPERTY_NAME] =
                        Property
                            .builder()
                            .name(TAB_WIDTH_PROPERTY_NAME)
                            .type(PropertyType.tab_width)
                            .value(editorConfigOverride.properties[INDENT_SIZE_PROPERTY]?.source)
                            .build()
                }
                editorConfigOverride
                    .properties
                    .forEach {
                        properties[it.key.name] = it.key.toPropertyWithValue(it.value)
                    }
            }.also { properties ->
                LOGGER.debug { properties.prettyPrint(filePath) }
            }.let { properties ->
                // Only add properties which are not related to rules but which are needed by the KtLint Rule Engine
                EditorConfig(properties).addPropertiesWithDefaultValueIfMissing(END_OF_LINE_PROPERTY)
            }
    }

    private fun MutableMap<String, Property>.prettyPrint(normalizedFilePath: Path?) =
        map { entry -> "${entry.key}: ${entry.value.sourceValue}" }
            .joinToString(
                prefix = "Effective editorconfig properties${
                    if (normalizedFilePath == null) {
                        ""
                    } else {
                        " for file '$normalizedFilePath'"
                    }
                }:\n\t",
                separator = "\n\t",
            )

    private fun Path?.resource() = Resource.Resources.ofPath(this, StandardCharsets.UTF_8)

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
    ) = ResourcePropertiesService
        .builder()
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
        PropertyTypeRegistry
            .builder()
            .defaults()
            .apply {
                rules
                    .flatMap(Rule::usesEditorConfigProperties)
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
