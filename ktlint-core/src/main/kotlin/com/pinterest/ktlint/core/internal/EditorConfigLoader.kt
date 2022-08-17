package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigDefaults.Companion.emptyEditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.emptyEditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.internal.ThreadSafeEditorConfigCache.Companion.threadSafeEditorConfigCache
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

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Loader for `.editorconfig` properties for files on [fileSystem].
 */
public class EditorConfigLoader(
    private val fileSystem: FileSystem = FileSystems.getDefault(),
) {
    /**
     * DEPRECATION NOTICE:
     * This method is removed from the public API in KtLint 0.48.0. Please raise an issue if you have a use case as
     * consumer of this API. If you currently rely on this method, then migrate to the replacement method which offers
     * a more consistent interface and allows to use both defaults and overrides of the '.editorconfig' files which are
     * found on the [filePath].
     *
     * Loads applicable properties from `.editorconfig`s for given file.
     *
     * @param filePath path to file that would be checked.
     * @param isStdIn indicates that checked content comes from input. Setting this to `true` overrides [filePath] and
     * uses `.kt` pattern to load properties from current folder `.editorconfig` files.
     * @param alternativeEditorConfig alternative to current [filePath] location where `.editorconfig` files should be
     * looked up
     * @param rules set of [Rule]s linting the file
     * @param editorConfigOverride map of values to add/replace values that were loaded from `.editorconfig` files
     * @param debug pass `true` to enable some additional debug output
     *
     * @return all possible loaded properties applicable to given file.
     * In case file extensions is not one of [SUPPORTED_FILES] or [filePath] is `null` method will immediately return
     * empty map.
     */
    @Deprecated(
        message = "Marked for removal in ktlint 0.48.0. See kdoc or changelog for more information",
        replaceWith = ReplaceWith("loadForFile(filePath)"),
    )
    public fun loadPropertiesForFile(
        filePath: Path?,
        isStdIn: Boolean = false,
        alternativeEditorConfig: Path? = null,
        rules: Set<Rule>,
        editorConfigOverride: EditorConfigOverride = emptyEditorConfigOverride,
        debug: Boolean = false,
    ): EditorConfigProperties {
        if (!isStdIn && filePath.isNullOrNotSupported()) {
            return editorConfigOverride
                .properties
                .map { (editorConfigProperty, propertyValue) ->
                    editorConfigProperty.type.name to property(editorConfigProperty, propertyValue)
                }.toMap()
        }

        val normalizedFilePath = when {
            alternativeEditorConfig != null -> {
                val editorconfigFilePath =
                    if (isStdIn) {
                        "stdin${SUPPORTED_FILES.first()}"
                    } else {
                        filePath!!.last()
                    }
                alternativeEditorConfig
                    .toAbsolutePath()
                    .resolve("$editorconfigFilePath")
            }
            isStdIn -> defaultFilePath()
            else -> filePath
        }

        return createLoaderService(rules, emptyEditorConfigDefaults)
            .queryProperties(normalizedFilePath.resource())
            .properties
            .also { loaded ->
                editorConfigOverride
                    .properties
                    .forEach {
                        loaded[it.key.type.name] = property(it.key, it.value)
                    }
            }.also { editorConfigProperties ->
                logger.trace { editorConfigProperties.prettyPrint(normalizedFilePath) }
            }
    }

    /**
     * DEPRECATION NOTICE:
     * This method is removed from the public API in KtLint 0.48.0. Please raise an issue if you have a use case as
     * consumer of this API. In case you migrate from the old method to this method, then just let us know in which
     * case this method will be kept available in the public API.
     *
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
    @Deprecated("Marked for removal from the public API in KtLint 0.48. See KDoc or changelog for more information")
    public fun load(
        filePath: Path?,
        rules: Set<Rule> = emptySet(),
        editorConfigDefaults: EditorConfigDefaults = emptyEditorConfigDefaults,
        editorConfigOverride: EditorConfigOverride = emptyEditorConfigOverride,
    ): EditorConfigProperties {
        if (filePath.isNullOrNotSupported()) {
            return editorConfigOverride
                .properties
                .map { (editorConfigProperty, propertyValue) ->
                    editorConfigProperty.type.name to property(editorConfigProperty, propertyValue)
                }.toMap()
        }

        // TODO: Move to class init once method load PropertiesForFiles has been removed.
        require(rules.isNotEmpty()) {
            "Set of rules for which the properties have to be loaded may not be empty."
        }

        val normalizedFilePath = filePath ?: defaultFilePath()

        return createLoaderService(rules, editorConfigDefaults)
            .queryProperties(normalizedFilePath.resource())
            .properties
            .also { loaded ->
                editorConfigOverride
                    .properties
                    .forEach {
                        loaded[it.key.type.name] = property(it.key, it.value)
                    }
            }.also { editorConfigProperties ->
                logger.trace { editorConfigProperties.prettyPrint(normalizedFilePath) }
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
        property: UsesEditorConfigProperties.EditorConfigProperty<*>,
        value: PropertyType.PropertyValue<*>,
    ) = Property
        .builder()
        .name(property.type.name)
        .type(property.type)
        .value(value)
        .build()

    private fun defaultFilePath() =
        fileSystem
            .getPath(".")
            .toAbsolutePath()
            .resolve(SUPPORTED_FILES.first())

    private fun Path?.isNullOrNotSupported() =
        this == null || this.isNotSupported()

    private fun Path.isNotSupported() =
        SUPPORTED_FILES
            .none {
                this.toString().endsWith(it)
            }

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
            .cache(threadSafeEditorConfigCache)
            .loader(editorConfigLoader)
            .applyIf(editorConfigDefaults != emptyEditorConfigDefaults) {
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

    /**
     * Trims used in-memory cache.
     */
    @Deprecated(
        message = "Marked for removal in KtLint 0.48.0",
        replaceWith = ReplaceWith("KtLint.trimMemory()"),
    )
    public fun trimMemory() {
        threadSafeEditorConfigCache.clear()
    }

    public companion object {
        /**
         * List of file extensions, editorconfig lookup will be performed.
         */
        internal val SUPPORTED_FILES = arrayOf(
            ".kt",
            ".kts",
        )

        /**
         * Converts loaded [EditorConfigProperties] values into string representation.
         *
         * @return map of key as string and value as string property representation
         */
        @Deprecated(message = "Marked for removal of public API in KtLint 0.48")
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
