package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleSetExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleExecutionPropertyName
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ktLintRuleSetExecutionPropertyName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EditorConfigPropertyRegistryTest {
    @Nested
    inner class `Given an editor config property without rule providers` {
        val editorConfigPropertyRegistry = EditorConfigPropertyRegistry(emptySet())

        @Test
        fun `Given a property name defined in the ktlint rule engine core module then return that property`() {
            val actual = editorConfigPropertyRegistry.find(INDENT_SIZE_PROPERTY.name)

            assertThat(actual).isEqualTo(INDENT_SIZE_PROPERTY)
        }

        @Test
        fun `Given a property name starting with 'ktlint_', and for which the suffix is a valid rule id then return the rule execution property for that rule id`() {
            val actual = editorConfigPropertyRegistry.find(SOME_RULE_ID.ktLintRuleExecutionPropertyName())

            assertThat(actual).isEqualTo(SOME_RULE_ID.createRuleExecutionEditorConfigProperty())
        }

        @Test
        fun `Given a property name starting with 'ktlint_', and for which the suffix is not valid rule id, but the suffix is a valid rule set id then return the rule execution property for that rule set id`() {
            val actual = editorConfigPropertyRegistry.find(SOME_RULE_SET_ID.ktLintRuleSetExecutionPropertyName())

            assertThat(actual).isEqualTo(SOME_RULE_SET_ID.createRuleSetExecutionEditorConfigProperty())
        }

        @Test
        fun `Given a property name that can not be found by name in the editor config property registry then throw an exception`() {
            assertThatExceptionOfType(EditorConfigPropertyNotFoundException::class.java)
                .isThrownBy { editorConfigPropertyRegistry.find("some-unknown-property-name") }
        }
    }

    @Test
    fun `Given a property name defined in a rule provided to the editor config property registry then return that property`() {
        val editorConfigPropertyRegistry =
            EditorConfigPropertyRegistry(
                setOf(
                    RuleProvider { SomeTestRule() },
                ),
            )

        val actual = editorConfigPropertyRegistry.find(SomeTestRule.SOME_PROPERTY_NAME)

        assertThat(actual).isEqualTo(SomeTestRule.SOME_PROPERTY)
    }

    private companion object {
        val SOME_RULE_ID = RuleId("some-ruleset:some-rule-name")
        val SOME_RULE_SET_ID = RuleSetId("some-ruleset")
    }
}

private class SomeTestRule :
    Rule(
        ruleId = RuleId("test:some-test-rule"),
        about = About(),
        usesEditorConfigProperties = setOf(SOME_PROPERTY),
    ) {
    companion object {
        const val SOME_PROPERTY_NAME = "some_property_name"
        val SOME_PROPERTY: EditorConfigProperty<Int> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        SOME_PROPERTY_NAME,
                        "some description",
                        PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                        setOf("1", "2", "3"),
                    ),
                defaultValue = 1,
            )
    }
}
