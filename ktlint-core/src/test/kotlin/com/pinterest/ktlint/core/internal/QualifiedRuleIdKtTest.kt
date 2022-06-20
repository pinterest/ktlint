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
            "custom - rule - set : rule - id ,custom-rule-set:rule-id"
        ],
        ignoreLeadingAndTrailingWhitespace = false,
        nullValues = [""]
    )
    fun `Given a rule id then return the qualified rule id`(
        ruleId: String,
        qualifiedRuleId: String
    ) {
        assertThat(
            ruleId.toQualifiedRuleId()
        ).isEqualTo(qualifiedRuleId)
    }

    @ParameterizedTest(name = "Rule set id: `{0}`, rule id: `{1}`, expected result: `{2}`")
    @CsvSource(
        value = [
            ",rule-id,standard:rule-id",
            ", rule-id,standard:rule-id",
            ",rule-id ,standard:rule-id",
            ",rule -id,standard:rule-id",
            "standard,rule-id,standard:rule-id",
            " standard,rule-id,standard:rule-id",
            "standard ,rule-id,standard:rule-id",
            "standard, rule-id,standard:rule-id",
            "standard,rule - id,standard:rule-id",
            "standard,rule-id ,standard:rule-id",
            "custom-rule-set,rule-id,custom-rule-set:rule-id",
            " custom-rule-set,rule-id,custom-rule-set:rule-id",
            "custom-rule-set ,rule-id,custom-rule-set:rule-id",
            "custom- rule -set,rule-id,custom-rule-set:rule-id",
            "custom-rule-set, rule-id,custom-rule-set:rule-id",
            "custom-rule-set,rule - id,custom-rule-set:rule-id",
            "custom-rule-set,rule-id ,custom-rule-set:rule-id"
        ],
        ignoreLeadingAndTrailingWhitespace = false,
        nullValues = [""]
    )
    fun `Given a RuleReference then return the qualified rule id`(
        rulesetId: String?,
        ruleId: String,
        qualifiedRuleId: String
    ) {
        assertThat(
            toRuleReference(rulesetId.orEmpty(), ruleId).toQualifiedRuleId()
        ).isEqualTo(qualifiedRuleId)
    }

    @ParameterizedTest(name = "Rule set id: `{0}`, rule id: `{1}`, expected result: `{2}`")
    @CsvSource(
        value = [
            ",rule-id,rule-id",
            ", rule-id,rule-id",
            ",rule-id ,rule-id",
            ",rule -id,rule-id",
            "standard,rule-id,rule-id",
            " standard,rule-id,rule-id",
            "standard ,rule-id,rule-id",
            "standard, rule-id,rule-id",
            "standard,rule - id,rule-id",
            "standard,rule-id ,rule-id",
            "custom-rule-set,rule-id,custom-rule-set:rule-id",
            " custom-rule-set,rule-id,custom-rule-set:rule-id",
            "custom-rule-set ,rule-id,custom-rule-set:rule-id",
            "custom- rule -set,rule-id,custom-rule-set:rule-id",
            "custom-rule-set, rule-id,custom-rule-set:rule-id",
            "custom-rule-set,rule - id,custom-rule-set:rule-id",
            "custom-rule-set,rule-id ,custom-rule-set:rule-id"
        ],
        ignoreLeadingAndTrailingWhitespace = false,
        nullValues = [""]
    )
    fun `Given a RuleReference then return the shortened qualified rule id`(
        rulesetId: String?,
        ruleId: String,
        shortenedQualifiedRuleId: String
    ) {
        assertThat(
            toRuleReference(rulesetId.orEmpty(), ruleId).toShortenedQualifiedRuleId()
        ).isEqualTo(shortenedQualifiedRuleId)
    }

    private fun toRuleReference(rulesetId: String, ruleId: String) =
        RuleReference(
            ruleId = ruleId,
            ruleSetId = rulesetId,
            runOnRootNodeOnly = false,
            runAsLateAsPossible = false,
            runAfterRule = null
        )
}
