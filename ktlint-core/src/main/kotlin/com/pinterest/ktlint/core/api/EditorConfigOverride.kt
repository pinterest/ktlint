package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.api.UsesEditorConfigProperties.EditorConfigProperty
import org.ec4j.core.model.PropertyType

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
@FeatureInAlphaState
public class EditorConfigOverride {
    private val _properties = mutableMapOf<EditorConfigProperty<*>, PropertyType.PropertyValue<*>>()

    public val properties: Map<EditorConfigProperty<*>, PropertyType.PropertyValue<*>>
        get() = _properties.toMap()

    private fun add(property: EditorConfigProperty<*>, value: Any?) =
        _properties.put(property, property.type.parse(value?.toString()))

    public companion object {
        /**
         * Creates the [EditorConfigOverride] based on one or more property-value mappings.
         */
        public fun from(
            vararg properties: Pair<EditorConfigProperty<*>, *>
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
         * Get the empty [EditorConfigOverride]. As it does not contain any properties, all properties fall back on
         * their respective default values.
         */
        public val emptyEditorConfigOverride: EditorConfigOverride = EditorConfigOverride()
    }
}
