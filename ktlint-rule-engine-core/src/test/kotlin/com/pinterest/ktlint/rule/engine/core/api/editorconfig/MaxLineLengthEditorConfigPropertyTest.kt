package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class MaxLineLengthEditorConfigPropertyTest {
    @Nested
    inner class PropertyMapper {
        val maxLineLengthPropertyMapper = MAX_LINE_LENGTH_PROPERTY.propertyMapper!!

        @ParameterizedTest(name = "Code style: {0}, default value: {1}")
        @CsvSource(
            value = [
                "android, 100",
                "android_studio, 100",
                "intellij_idea, -1",
                "official, -1",
                "ktlint_official, 140",
            ],
        )
        fun `Given a null property then the property mapper returns the default value of the code style`(
            codeStyleValue: CodeStyleValue,
            expectedDefaultValue: Int,
        ) {
            val actual = maxLineLengthPropertyMapper(null, codeStyleValue)

            assertThat(actual).isEqualTo(expectedDefaultValue)
        }

        @ParameterizedTest(name = "Code style: {0}, default value: {1}")
        @CsvSource(
            value = [
                "android, 100",
                "android_studio, 100",
                "intellij_idea, -1",
                "official, -1",
                "ktlint_official, 140",
            ],
        )
        fun `Given a property which is unset then the property mapper returns the default value of the code style`(
            codeStyleValue: CodeStyleValue,
            expectedDefaultValue: Int,
        ) {
            val property = maxLineLengthProperty("unset")

            val actual = maxLineLengthPropertyMapper(property, codeStyleValue)

            assertThat(actual).isEqualTo(expectedDefaultValue)
        }

        @ParameterizedTest(name = "Code style: {0}")
        @EnumSource(CodeStyleValue::class)
        fun `Given a valid string value then the property mapper returns the integer value`(codeStyleValue: CodeStyleValue) {
            val someValue = 123
            val property = maxLineLengthProperty(someValue.toString())

            val actual = maxLineLengthPropertyMapper(property, codeStyleValue)

            assertThat(actual).isEqualTo(someValue)
        }

        @ParameterizedTest(name = "Code style: {0}")
        @EnumSource(CodeStyleValue::class)
        fun `Given the value 'off' then the property mapper returns -1 which is internally used to disable the max line length`(
            codeStyleValue: CodeStyleValue,
        ) {
            val property = maxLineLengthProperty("off")

            val actual = maxLineLengthPropertyMapper(property, codeStyleValue)

            assertThat(actual).isEqualTo(-1)
        }

        @ParameterizedTest(name = "Code style: {0}")
        @EnumSource(CodeStyleValue::class)
        fun `Given an invalid value then the property mapper returns -1 which is internally used to disable the max line length`(
            codeStyleValue: CodeStyleValue,
        ) {
            val property = maxLineLengthProperty("some-invalid-value")

            val actual = maxLineLengthPropertyMapper(property, codeStyleValue)

            assertThat(actual).isEqualTo(-1)
        }
    }

    @ParameterizedTest(name = "Input value: {0}, output value: {1}")
    @CsvSource(
        value = [
            "-1, off",
            "0, off",
            "1, 1",
        ],
    )
    fun `Given a property with an integer value than write that property`(
        inputValue: Int,
        expectedOutputValue: String,
    ) {
        val actual = MAX_LINE_LENGTH_PROPERTY.propertyWriter(inputValue)

        assertThat(actual).isEqualTo(expectedOutputValue)
    }

    private fun maxLineLengthProperty(value: String?): Property? =
        Property
            .builder()
            .name("max_line_length")
            .type(PropertyType.max_line_length)
            .value(value)
            .build()
}
