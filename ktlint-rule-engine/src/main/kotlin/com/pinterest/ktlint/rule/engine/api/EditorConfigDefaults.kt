package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.internal.EditorConfigDefaultsLoader
import com.pinterest.ktlint.rule.engine.internal.EditorConfigLoaderEc4j
import dev.drewhamilton.poko.Poko
import org.ec4j.core.model.EditorConfig
import org.ec4j.core.model.PropertyType
import java.nio.file.Path

/**
 * Wrapper around the ec4j [EditorConfig]. Only to be used to specify the default value of ec4j properties. Those default values will only
 * be used whenever a property is retrieved from the ec4j [EditorConfig] and the property has not been defined in the ".editorconfig" file.
 */
@Poko
public class EditorConfigDefaults(
    /**
     * The ec4j [EditorConfig] containing the default value of the ec4j properties. Those default values will only be used whenever a
     * property is retrieved from the ec4j [EditorConfig] and the property has not been defined in the ".editorconfig" file.
     */
    public val value: EditorConfig,
) {
    public companion object {
        /**
         * Loads properties from [path]. [path] may either locate a file (also allows specifying a file with a name other than
         * ".editorconfig") or a directory in which a file with name ".editorconfig" is expected to exist. Properties from all globs are
         * returned. If [path] is not valid then the [EMPTY_EDITOR_CONFIG_DEFAULTS] is returned.
         *
         * Properties having a custom [PropertyType] (e.g. a type not defined in the ec4j library) can not be parsed to the correct type
         * when the property type is missing and will by default be converted to type [String]. [PropertyType]'s can be easily extracted
         * with extension function `Collection<RuleProvider>.propertyTypes()`.
         *
         * The property "root" which denotes whether the parent directory is to be checked for the existence of a fallback
         * ".editorconfig" is ignored entirely.
         */
        public fun load(
            path: Path?,
            propertyTypes: Set<PropertyType<*>>,
        ): EditorConfigDefaults =
            EditorConfigDefaultsLoader(
                EditorConfigLoaderEc4j(propertyTypes),
            ).load(path)

        /**
         * Empty representation of [EditorConfigDefaults].
         */
        public val EMPTY_EDITOR_CONFIG_DEFAULTS: EditorConfigDefaults = EditorConfigDefaults(EditorConfig.builder().build())
    }
}
