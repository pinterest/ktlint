package com.pinterest.ktlint.rule.engine.core.api.editorconfig.ec4j

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType

/**
 * Creates an [org.ec4j.core.model.Property.Builder] for given [value].
 */
public fun <T> EditorConfigProperty<T>.toPropertyBuilderWithValue(value: String): Property.Builder =
    Property
        .builder()
        .type(type)
        .name(name)
        .value(value)

/**
 * Creates an [org.ec4j.core.model.Property] for given [value].
 */
public fun <T> EditorConfigProperty<T>.toPropertyWithValue(value: String): Property = toPropertyBuilderWithValue(value).build()

/**
 * Creates an [org.ec4j.core.model.Property.Builder] for given [value].
 */
public fun <T> EditorConfigProperty<T>.toPropertyBuilderWithValue(value: PropertyType.PropertyValue<*>): Property.Builder =
    Property
        .builder()
        .type(type)
        .name(name)
        .value(value)

/**
 * Creates an [org.ec4j.core.model.Property] for given [value].
 */
public fun <T> EditorConfigProperty<T>.toPropertyWithValue(value: PropertyType.PropertyValue<*>): Property =
    toPropertyBuilderWithValue(value).build()
