package com.pinterest.ktlint.core.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class QualifiedRuleIdKtTest {
    @ParameterizedTest(name = "Rule id: `{0}`, expected result: `{1}`")
    @CsvSource(
        value = [
            "rule-id,standard:rule-id",
            " rule-id,standard:rule-id",
            "rule-id ,standard:rule-id",
            "rule -id,standard:rule-id",
            "standard:rule-id,standard:rule-id",
            " standard:rule-id,standard:rule-id",
            "standard:rule-id ,standard:rule-id",
            "standard : rule- id,standard:rule-id",
            "custom-rule-set:rule-id,custom-rule-set:rule-id",
            " custom-rule-set:rule-id,custom-rule-set:rule-id",
            "custom-rule-set:rule-id ,custom-rule-set:rule-id",
            "custom - rule - set : rule - id ,custom-rule-set:rule-id",
        ],
        ignoreLeadingAndTrailingWhitespace = false,
        nullValues = [""],
    )
    fun `Given a rule id then return the qualified rule id`(
        ruleId: String,
        qualifiedRuleId: String,
    ) {
        assertThat(
            ruleId.toQualifiedRuleId(),
        ).isEqualTo(qualifiedRuleId)
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule id: `{1}`")
    @CsvSource(
        value = [
            "rule-id,rule-id",
            "standard:rule-id,rule-id",
            "experimental:rule-id,rule-id",
            "custom:rule-id,rule-id",
        ],
    )
    fun `Given a qualified rule id then return the rule id`(
        qualifiedRuleId: String,
        ruleId: String,
    ) {
        assertThat(
            ruleId(qualifiedRuleId),
        ).isEqualTo(ruleId)
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule set id: `{1}`")
    @CsvSource(
        value = [
            "rule-id,standard",
            "standard:rule-id,standard",
            "experimental:rule-id,experimental",
            "custom:rule-id,custom",
        ],
    )
    fun `Given a qualified rule id then return the rule set id`(
        qualifiedRuleId: String,
        ruleSetId: String,
    ) {
        assertThat(
            ruleSetId(qualifiedRuleId),
        ).isEqualTo(ruleSetId)
    }
}
