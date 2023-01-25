package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.FeatureInAlphaState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(FeatureInAlphaState::class)
class RuleKtTest {
    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule id: `{1}`")
    @CsvSource(
        value = [
            "rule-id,rule-id",
            "standard:rule-id,rule-id",
            "custom:rule-id,rule-id",
        ],
    )
    fun `Given a qualified rule id then return the rule id`(
        qualifiedRuleId: String,
        ruleId: String,
    ) {
        assertThat(
            qualifiedRuleId.ruleId(),
        ).isEqualTo(ruleId)
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule set id: `{1}`")
    @CsvSource(
        value = [
            "rule-id,standard",
            "standard:rule-id,standard",
            "custom:rule-id,custom",
        ],
    )
    fun `Given a qualified rule id then return the rule set id`(
        qualifiedRuleId: String,
        ruleSetId: String,
    ) {
        assertThat(
            qualifiedRuleId.ruleSetId(),
        ).isEqualTo(ruleSetId)
    }
}
