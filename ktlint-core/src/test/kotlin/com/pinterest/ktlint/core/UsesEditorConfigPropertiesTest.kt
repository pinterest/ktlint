package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.MAX_LINE_LENGTH_PROPERTY
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
                INDENT_SIZE_PROPERTY,
                SOME_INTEGER_VALUE.toString(),
            )

            val actual = with(EditorConfigPropertiesTester(INDENT_SIZE_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(INDENT_SIZE_PROPERTY)
            }

            assertThat(actual).isEqualTo(SOME_INTEGER_VALUE)
        }

        @Test
        fun `Given that editor config property indent_size is set to value 'unset' then return -1 as value via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                INDENT_SIZE_PROPERTY,
                "unset",
            )

            val actual = with(EditorConfigPropertiesTester(INDENT_SIZE_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(INDENT_SIZE_PROPERTY)
            }

            assertThat(actual).isEqualTo(-1)
        }

        @Test
        fun `Issue 1485 - Given that editor config property indent_size is set to value 'tab' then return tabWidth as value via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                INDENT_SIZE_PROPERTY,
                "tab",
            )

            val actual = with(EditorConfigPropertiesTester(INDENT_SIZE_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(INDENT_SIZE_PROPERTY)
            }

            assertThat(actual).isEqualTo(IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth)
        }

        @Test
        fun `Given that editor config property indent_size is not set then return the default tabWidth as value via the getEditorConfigValue of the node`() {
            val actual = with(EditorConfigPropertiesTester(INDENT_SIZE_PROPERTY)) {
                emptyMap<String, Property>().getEditorConfigValue(INDENT_SIZE_PROPERTY)
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
                MAX_LINE_LENGTH_PROPERTY,
                SOME_INTEGER_VALUE.toString(),
            )

            val actual = with(EditorConfigPropertiesTester(MAX_LINE_LENGTH_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
            }

            assertThat(actual).isEqualTo(SOME_INTEGER_VALUE)
        }

        @Test
        fun `Given that editor config property max_line_length is set to value 'off' then return -1 via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                MAX_LINE_LENGTH_PROPERTY,
                "off",
            )

            val actual = with(EditorConfigPropertiesTester(MAX_LINE_LENGTH_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
            }

            assertThat(actual).isEqualTo(-1)
        }

        @Test
        fun `Given that editor config property max_line_length is set to value 'unset' for android then return 100 via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                MAX_LINE_LENGTH_PROPERTY,
                "unset",
            ).plus(ANDROID_CODE_STYLE)

            val actual = with(EditorConfigPropertiesTester(MAX_LINE_LENGTH_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
            }

            assertThat(actual).isEqualTo(100)
        }

        @Test
        fun `Given that editor config property max_line_length is set to value 'unset' for non-android then return -1 via the getEditorConfigValue of the node`() {
            val editorConfigProperties = createEditorConfigPropertiesFrom(
                MAX_LINE_LENGTH_PROPERTY,
                "unset",
            ).plus(OFFICIAL_CODE_STYLE)

            val actual = with(EditorConfigPropertiesTester(MAX_LINE_LENGTH_PROPERTY)) {
                editorConfigProperties.getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
            }

            assertThat(actual).isEqualTo(-1)
        }

        @Test
        fun `Given that editor config property max_line_length is not set for android then return 100 via the getEditorConfigValue of the node`() {
            val actual = with(EditorConfigPropertiesTester(MAX_LINE_LENGTH_PROPERTY)) {
                ANDROID_CODE_STYLE.getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
            }

            assertThat(actual).isEqualTo(100)
        }

        @Test
        fun `Given that editor config property max_line_length is not set for non-android then return -1 via the getEditorConfigValue of the node`() {
            val actual = with(EditorConfigPropertiesTester(MAX_LINE_LENGTH_PROPERTY)) {
                OFFICIAL_CODE_STYLE.getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
            }

            assertThat(actual).isEqualTo(-1)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `Given that editor config property disabled_rules is set and has spacing around the comma, then retrieve the list without those spaces'`() {
        val editorConfigProperties = createEditorConfigPropertiesFrom(
            DISABLED_RULES_PROPERTY,
            "$RULE_A, $RULE_B,$RULE_C , $RULE_D",
        )

        val actual = with(EditorConfigPropertiesTester(DISABLED_RULES_PROPERTY)) {
            editorConfigProperties.getEditorConfigValue(DISABLED_RULES_PROPERTY)
        }

        assertThat(actual).isEqualTo("$RULE_A,$RULE_B,$RULE_C,$RULE_D")
    }

    @Test
    fun `Given that editor config property ktlint_disabled_rules is set and has spacing around the comma, then retrieve the list without those spaces'`() {
        val editorConfigProperties = createEditorConfigPropertiesFrom(
            KTLINT_DISABLED_RULES_PROPERTY,
            "$RULE_A, $RULE_B,$RULE_C , $RULE_D",
        )

        val actual = with(EditorConfigPropertiesTester(KTLINT_DISABLED_RULES_PROPERTY)) {
            editorConfigProperties.getEditorConfigValue(KTLINT_DISABLED_RULES_PROPERTY)
        }

        assertThat(actual).isEqualTo("$RULE_A,$RULE_B,$RULE_C,$RULE_D")
    }

    class EditorConfigPropertiesTester<T>(
        editorConfigProperty: UsesEditorConfigProperties.EditorConfigProperty<T>,
    ) : UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(editorConfigProperty)
    }

    private companion object {
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val RULE_D = "rule-d"
        const val SOME_INTEGER_VALUE = 123
        val ANDROID_CODE_STYLE = createEditorConfigPropertiesFrom(
            DefaultEditorConfigProperties.CODE_STYLE_PROPERTY,
            DefaultEditorConfigProperties.CodeStyleValue.android.name.lowercase(),
        )
        val OFFICIAL_CODE_STYLE = createEditorConfigPropertiesFrom(
            DefaultEditorConfigProperties.CODE_STYLE_PROPERTY,
            DefaultEditorConfigProperties.CodeStyleValue.official.name.lowercase(),
        )

        private fun <T : Any> createEditorConfigPropertiesFrom(
            editorConfigProperty: UsesEditorConfigProperties.EditorConfigProperty<T>,
            value: String,
        ) =
            with(editorConfigProperty) {
                mapOf(
                    type.name to Property.builder()
                        .name(type.name)
                        .type(type)
                        .value(value)
                        .build(),
                )
            }
    }
}
