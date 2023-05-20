package com.pinterest.ktlint.rule.engine.core.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class RuleKtTest {
    @Test
    fun `Given a rule with an unqualified rule id than the rule can not be instantiated`() {
        assertThatThrownBy { creatRule("some-unqualified-rule-id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Rule with id 'some-unqualified-rule-id' must match regexp '[a-z]+(-[a-z]+)*:[a-z]+(-[a-z]+)*'")
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule id: `{1}`")
    @ValueSource(
        strings = [
            "standard:rule-id",
            "custom:rule-id",
        ],
    )
    fun `Given a rule with a qualified rule id then return the rule id`(id: String) {
        val rule = creatRule(id)
        assertThat(rule.ruleId.value).isEqualTo(id)
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule set id: `{1}`")
    @CsvSource(
        value = [
            "standard:rule-id,standard",
            "custom:rule-id,custom",
        ],
    )
    fun `Given a qualified rule id then return the rule set id`(
        id: String,
        ruleSetId: String,
    ) {
        val rule = creatRule(id)
        assertThat(rule.ruleId.ruleSetId.value).isEqualTo(ruleSetId)
    }

    private fun creatRule(ruleId: String) =
        object : Rule(
            ruleId = RuleId(ruleId),
            about = About(),
        ) {}
}
