package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.internal.rules.KTLINT_SUPPRESSION_RULE_ID
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test

class InternalRuleProvidersFilterTest {
    @Test
    fun `Given a ktlint rule engine then add the ktlint suppression rule provider`() {
        val ktLintRuleEngine =
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider {
                            object : R(ruleId = STANDARD_RULE_A) {}
                        },
                    ),
            )
        val actual =
            InternalRuleProvidersFilter(ktLintRuleEngine)
                .filter(ktLintRuleEngine.ruleProviders)
                .toRuleId()

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            KTLINT_SUPPRESSION_RULE_ID,
        )
    }

    private companion object {
        const val RULE_A = "rule-a"
        val STANDARD = RuleSetId.STANDARD.value
        val STANDARD_RULE_A = RuleId("$STANDARD:$RULE_A")
    }

    private open class R(
        ruleId: RuleId,
        visitorModifiers: Set<VisitorModifier> = emptySet(),
    ) : Rule(
            ruleId = ruleId,
            about = About(),
            visitorModifiers,
        ),
        RuleAutocorrectApproveHandler {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        ): Unit =
            throw UnsupportedOperationException(
                "Rule should never be really invoked because that is not the aim of this unit test.",
            )
    }

    private fun Set<RuleProvider>.toRuleId() = map { it.ruleId }
}
