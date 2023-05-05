package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ec4j.toPropertyBuilderWithValue as ec4jToPropertyBuilderWithPropertyValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ec4j.toPropertyBuilderWithValue as ec4jToPropertyBuilderWithStringValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ec4j.toPropertyWithValue as ec4jToPropertyWithPropertyValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ec4j.toPropertyWithValue as ec4jToPropertyWithStringValue

/**
 * Creates an [org.ec4j.core.model.Property.Builder] for given [value].
 */
@Deprecated("Marked for removal in ktlint 0.50. Use function exposed by ktlint-rule-engine-core instead")
public fun <T> EditorConfigProperty<T>.toPropertyBuilderWithValue(value: String): Property.Builder =
    ec4jToPropertyBuilderWithStringValue(value)

/**
 * Creates an [org.ec4j.core.model.Property] for given [value].
 */
@Deprecated("Marked for removal in ktlint 0.50. Use function exposed by ktlint-rule-engine-core instead")
public fun <T> EditorConfigProperty<T>.toPropertyWithValue(value: String): Property = ec4jToPropertyWithStringValue(value)

/**
 * Creates an [org.ec4j.core.model.Property.Builder] for given [value].
 */
@Deprecated("Marked for removal in ktlint 0.50. Use function exposed by ktlint-rule-engine-core instead")
public fun <T> EditorConfigProperty<T>.toPropertyBuilderWithValue(value: PropertyType.PropertyValue<*>): Property.Builder =
    ec4jToPropertyBuilderWithPropertyValue(value)

/**
 * Creates an [org.ec4j.core.model.Property] for given [value].
 */
@Deprecated("Marked for removal in ktlint 0.50. Use function exposed by ktlint-rule-engine-core instead")
public fun <T> EditorConfigProperty<T>.toPropertyWithValue(value: PropertyType.PropertyValue<*>): Property =
    ec4jToPropertyWithPropertyValue(value)
