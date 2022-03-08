package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties.EditorConfigProperty
import org.ec4j.core.model.PropertyType

/**
 * Properties set in [EditorConfigOverride] override values which are loaded from the ".EditorConfig" file. This
 * [EditorConfigOverride] is only to be used in unit tests. From the perspective of the unit test as well as from the
 * perspective of the rule, it is transparent whether an editor config property was loaded from an ".editorconfig" file
 * or from an override.
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
         * Creates EditorConfig properties with value to be used in unit test so that the unit test does not need to
         * write an .editorconfig file.
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
         * Get the empty EditorConfig properties. As it does not contain any properties, all properties fall back on
         * their respective default value.
         */
        public val emptyEditorConfigOverride: EditorConfigOverride = EditorConfigOverride()
    }
}
