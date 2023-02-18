package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class IndentSizeEditorConfigPropertyTest {
    val indentSizePropertyMapper = INDENT_SIZE_PROPERTY.propertyMapper!!

    @ParameterizedTest(name = "Code style: {0}, default value: {1}")
    @EnumSource(CodeStyleValue::class)
    fun `Given a null property then the property mapper returns 4 which is set as the default value`(codeStyleValue: CodeStyleValue) {
        val actual = indentSizePropertyMapper(null, codeStyleValue)

        assertThat(actual).isEqualTo(4)
    }

    @ParameterizedTest(name = "Code style: {0}, default value: {1}")
    @EnumSource(CodeStyleValue::class)
    fun `Given a property which is unset then the property mapper returns -1 as default value`(codeStyleValue: CodeStyleValue) {
        val property = indentSizeProperty("unset")

        val actual = indentSizePropertyMapper(property, codeStyleValue)

        assertThat(actual).isEqualTo(-1)
    }

    @ParameterizedTest(name = "Code style: {0}")
    @EnumSource(CodeStyleValue::class)
    fun `Given a valid string value then the property mapper returns the integer value`(codeStyleValue: CodeStyleValue) {
        val someValue = 123
        val property = indentSizeProperty(someValue.toString())

        val actual = indentSizePropertyMapper(property, codeStyleValue)

        assertThat(actual).isEqualTo(someValue)
    }

    @ParameterizedTest(name = "Code style: {0}")
    @EnumSource(CodeStyleValue::class)
    fun `Given value 'tab' then the property mapper returns 4 which is set as the default value`(codeStyleValue: CodeStyleValue) {
        val property = indentSizeProperty("tab")

        val actual = indentSizePropertyMapper(property, codeStyleValue)

        assertThat(actual).isEqualTo(4)
    }

    private fun indentSizeProperty(value: String?): Property? =
        Property
            .builder()
            .name("indent_size")
            .type(PropertyType.indent_size)
            .value(value)
            .build()
}
