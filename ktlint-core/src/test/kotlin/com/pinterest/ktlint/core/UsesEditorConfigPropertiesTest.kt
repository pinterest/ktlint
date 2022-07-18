package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UsesEditorConfigPropertiesTest {
    @Nested
    inner class IndentSizeProperty {
        @Test
        fun `Given that editor config property indent_size is set to an integer value then return that integer value via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                indentSizeProperty,
                SOME_INTEGER_VALUE.toString()
            )

            val actual = with(EditorConfigPropertiesTester(indentSizeProperty)) {
                editorConfigProperties.getEditorConfigValue(indentSizeProperty)
            }

            assertThat(actual).isEqualTo(SOME_INTEGER_VALUE)
        }

        @Test
        fun `Given that editor config property indent_size is set to value 'unset' then return -1 as value via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                indentSizeProperty,
                "unset"
            )

            val actual = with(EditorConfigPropertiesTester(indentSizeProperty)) {
                editorConfigProperties.getEditorConfigValue(indentSizeProperty)
            }

            assertThat(actual).isEqualTo(-1)
        }

        @Test
        fun `Issue 1485 - Given that editor config property indent_size is set to value 'tab' then return tabWidth as value via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                indentSizeProperty,
                "tab"
            )

            val actual = with(EditorConfigPropertiesTester(indentSizeProperty)) {
                editorConfigProperties.getEditorConfigValue(indentSizeProperty)
            }

            assertThat(actual).isEqualTo(IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth)
        }

        @Test
        fun `Given that editor config property indent_size is not set then return the default tabWidth as value via the getEditorConfigValue of the node`() {
            val actual = with(EditorConfigPropertiesTester(indentSizeProperty)) {
                emptyMap<String, Property>().getEditorConfigValue(indentSizeProperty)
            }

            assertThat(actual).isEqualTo(IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth)

            assertThat(actual).isEqualTo(IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth)
        }
    }

    @Nested
    inner class MaxLineLengthProperty {
        @Test
        fun `Given that editor config property max_line_length is set to an integer value then return that integer value via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                maxLineLengthProperty,
                SOME_INTEGER_VALUE.toString()
            )

            val actual = with(EditorConfigPropertiesTester(maxLineLengthProperty)) {
                editorConfigProperties.getEditorConfigValue(maxLineLengthProperty)
            }

            assertThat(actual).isEqualTo(SOME_INTEGER_VALUE)
        }

        @Test
        fun `Given that editor config property max_line_length is set to value 'off' then return -1 via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                maxLineLengthProperty,
                "off"
            )

            val actual = with(EditorConfigPropertiesTester(maxLineLengthProperty)) {
                editorConfigProperties.getEditorConfigValue(maxLineLengthProperty)
            }

            assertThat(actual).isEqualTo(-1)
        }

        @Test
        fun `Given that editor config property max_line_length is set to value 'unset' for android then return 100 via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                maxLineLengthProperty,
                "unset"
            ).plus(ANDROID_CODE_STYLE)

            val actual = with(EditorConfigPropertiesTester(maxLineLengthProperty)) {
                editorConfigProperties.getEditorConfigValue(maxLineLengthProperty)
            }

            assertThat(actual).isEqualTo(100)
        }

        @Test
        fun `Given that editor config property max_line_length is set to value 'unset' for non-android then return -1 via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                maxLineLengthProperty,
                "unset"
            ).plus(OFFICIAL_CODE_STYLE)

            val actual = with(EditorConfigPropertiesTester(maxLineLengthProperty)) {
                editorConfigProperties.getEditorConfigValue(maxLineLengthProperty)
            }

            assertThat(actual).isEqualTo(-1)
        }

        @Test
        fun `Given that editor config property max_line_length is not set for android then return 100 via the getEditorConfigValue of the node`() {
            val actual = with(EditorConfigPropertiesTester(maxLineLengthProperty)) {
                ANDROID_CODE_STYLE.getEditorConfigValue(maxLineLengthProperty)
            }

            assertThat(actual).isEqualTo(100)
        }

        @Test
        fun `Given that editor config property max_line_length is not set for non-android then return -1 via the getEditorConfigValue of the node`() {
            val actual = with(EditorConfigPropertiesTester(maxLineLengthProperty)) {
                OFFICIAL_CODE_STYLE.getEditorConfigValue(maxLineLengthProperty)
            }

            assertThat(actual).isEqualTo(-1)
        }
    }

    class EditorConfigPropertiesTester<T>(
        editorConfigProperty: UsesEditorConfigProperties.EditorConfigProperty<T>
    ) : UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(editorConfigProperty)
    }

    private companion object {
        const val SOME_INTEGER_VALUE = 123
        val ANDROID_CODE_STYLE = createEditorConfigPropertiesFrom(
            DefaultEditorConfigProperties.codeStyleSetProperty,
            DefaultEditorConfigProperties.CodeStyleValue.android.name.lowercase()
        )
        val OFFICIAL_CODE_STYLE = createEditorConfigPropertiesFrom(
            DefaultEditorConfigProperties.codeStyleSetProperty,
            DefaultEditorConfigProperties.CodeStyleValue.official.name.lowercase()
        )

        private fun <T : Any> createEditorConfigPropertiesFrom(
            editorConfigProperty: UsesEditorConfigProperties.EditorConfigProperty<T>,
            value: String
        ) =
            with(editorConfigProperty) {
                mapOf(
                    type.name to Property.builder()
                        .name(type.name)
                        .type(type)
                        .value(value)
                        .build()
                )
            }
    }
}
