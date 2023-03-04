package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class RuleRunnerTest {
    @Test
    fun `Given a rule having a single RunAfterRule visitor modifier referring to the rule itself then throw an exception`() {
        assertThatIllegalStateException().isThrownBy {
            RuleRunner(
                RuleProvider {
                    object : Rule(
                        ruleId = SOME_RULE_ID,
                        about = About(),
                        visitorModifiers = setOf(
                            VisitorModifier.RunAfterRule(
                                ruleId = SOME_RULE_ID,
                                mode = VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                            ),
                        ),
                    ) {}
                },
            )
        }
    }

    @Test
    fun `Given a rule having multiple RunAfterRule visitor modifiers of which one refers to the rule itself then throw an exception`() {
        assertThatIllegalStateException().isThrownBy {
            RuleRunner(
                RuleProvider {
                    object : Rule(
                        ruleId = SOME_RULE_ID,
                        about = About(),
                        visitorModifiers = setOf(
                            VisitorModifier.RunAfterRule(
                                ruleId = SOME_OTHER_RULE_ID,
                                mode = VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                            ),
                            VisitorModifier.RunAfterRule(
                                ruleId = SOME_RULE_ID,
                                mode = VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                            ),
                        ),
                    ) {}
                },
            )
        }
    }

    private companion object {
        val SOME_RULE_ID = RuleId("test:some-rule-id")
        val SOME_OTHER_RULE_ID = RuleId("test:some-other-rule-id")
    }
}
