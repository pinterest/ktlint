package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType.PropertyValue

/**
 * The [EditorConfigOverride] allows to add or replace properties which are loaded from the ".editorconfig" file. It
 * serves two purposes.
 *
 * Firstly, the [EditorConfigOverride] can be used by API consumers to run a rule with values which are not actually
 * save to the ".editorconfig" file. When doing so, this should be clearly communicated to their consumers who will
 * expect the settings in that file to be respected.
 *
 * Secondly, the [EditorConfigOverride] is used in unit tests, to test a rule with distinct values of a property without
 * having to access an ".editorconfig" file from physical storage. This also improves readability of the tests.
 */
public class EditorConfigOverride {
    private val _properties = mutableMapOf<EditorConfigProperty<*>, PropertyValue<*>>()

    /**
     * Gets a safe copy of the [EditorConfigProperty] set.
     */
    public val properties: Map<EditorConfigProperty<*>, PropertyValue<*>>
        get() = _properties.toMap()

    private fun add(property: EditorConfigProperty<*>, value: Any?) =
        _properties.put(
            property,
            if (value is PropertyValue<*>) {
                value
            } else {
                property.type.parse(value?.toString())
            },
        )

    public companion object {
        /**
         * Creates the [EditorConfigOverride] based on one or more property-value mappings.
         */
        public fun from(
            vararg properties: Pair<EditorConfigProperty<*>, *>,
        ): EditorConfigOverride {
            require(properties.isNotEmpty()) {
                "Can not create an EditorConfigOverride without properties. Use 'emptyEditorConfigOverride' instead."
            }
            return EditorConfigOverride()
                .apply {
                    properties.forEach {
                        add(it.first, it.second)
                    }
                }
        }

        /**
         * Creates a copy of [EditorConfigOverride] plus given properties. Given properties overwrite the property in
         * the original [EditorConfigOverride] silently.
         */
        public fun EditorConfigOverride.plus(
            vararg properties: Pair<EditorConfigProperty<*>, *>,
        ): EditorConfigOverride {
            require(properties.isNotEmpty()) {
                "Can not add EditorConfigOverride without properties."
            }
            return EditorConfigOverride()
                .apply {
                    this@plus._properties.forEach {
                        add(it.key, it.value)
                    }
                    properties.forEach {
                        add(it.first, it.second)
                    }
                }
        }

        /**
         * Get the empty [EditorConfigOverride]. As it does not contain any properties, all properties fall back on
         * their respective default values.
         */
        public val EMPTY_EDITOR_CONFIG_OVERRIDE: EditorConfigOverride = EditorConfigOverride()

        @Deprecated(
            message = "Marked for removal in KtLint 0.49",
            replaceWith = ReplaceWith("EMPTY_EDITOR_CONFIG_OVERRIDE"),
        )
        @Suppress("ktlint:experimental:property-naming")
        public val emptyEditorConfigOverride: EditorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE
    }
}
