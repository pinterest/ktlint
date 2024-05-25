package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RunAfterRuleFilterTest {
    @Nested
    inner class `Given a rule with a RunAfterRule visitor modifier for a rule which is required to be loaded then throw an exception` {
        @Test
        fun `Given that the rule contains a single visitor modifier`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RunAfterRuleFilter()
                        .filter(
                            createRuleProviders(
                                createRule(STANDARD_RULE_A, ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED, STANDARD_RULE_B),
                                createRule(EXPERIMENTAL_RULE_C, ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED, EXPERIMENTAL_RULE_B),
                            ),
                        )
                }.withMessage(
                    """
                    Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to add additional rule sets before creating an issue.
                      - Rule with id '$STANDARD_RULE_A' requires rule with id '$STANDARD_RULE_B' to be loaded
                      - Rule with id '$EXPERIMENTAL_RULE_C' requires rule with id '$EXPERIMENTAL_RULE_B' to be loaded
                    """.trimIndent(),
                )
        }

        @Test
        fun `Given that the rule contains multiple visitor modifiers`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RunAfterRuleFilter()
                        .filter(
                            createRuleProviders(
                                object : R(
                                    ruleId = STANDARD_RULE_A,
                                    visitorModifiers =
                                        setOf(
                                            VisitorModifier.RunAfterRule(
                                                ruleId = STANDARD_RULE_B,
                                                mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                            ),
                                            VisitorModifier.RunAfterRule(
                                                ruleId = STANDARD_RULE_C,
                                                mode = ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED,
                                            ),
                                        ),
                                ) {},
                                object :
                                    R(
                                        ruleId = EXPERIMENTAL_RULE_B,
                                        visitorModifiers =
                                            setOf(
                                                VisitorModifier.RunAfterRule(
                                                    ruleId = EXPERIMENTAL_RULE_C,
                                                    mode = ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED,
                                                ),
                                            ),
                                    ),
                                    Rule.Experimental {},
                            ),
                        )
                }.withMessage(
                    """
                    Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to add additional rule sets before creating an issue.
                      - Rule with id '$STANDARD_RULE_A' requires rule with id '$STANDARD_RULE_C' to be loaded
                      - Rule with id '$EXPERIMENTAL_RULE_B' requires rule with id '$EXPERIMENTAL_RULE_C' to be loaded
                    """.trimIndent(),
                )
        }
    }

    @Nested
    inner class `Given a rule with a RunAfterRule visitor modifier on a rule which is not required to be loaded then run the rule` {
        @Test
        fun `Given a rule with a single RunAfterRule visitor modifier`() {
            val actual =
                RunAfterRuleFilter()
                    .filter(
                        createRuleProviders(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = RuleId("test:not-loaded-rule"),
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                        ),
                    ).toRuleId()

            assertThat(actual).containsExactly(
                STANDARD_RULE_A,
            )
        }

        @Test
        fun `Given a rule with a multiple RunAfterRule visitor modifiers`() {
            val actual =
                RunAfterRuleFilter()
                    .filter(
                        createRuleProviders(
                            object : R(STANDARD_RULE_A) {},
                            object : R(
                                ruleId = STANDARD_RULE_B,
                                visitorModifiers =
                                    setOf(
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_A,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                        VisitorModifier.RunAfterRule(
                                            ruleId = RuleId("test:not-loaded-rule"),
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                    ),
                            ) {},
                        ),
                    ).toRuleId()

            assertThat(actual).containsExactly(
                STANDARD_RULE_A,
                STANDARD_RULE_B,
            )
        }
    }

    @Nested
    inner class `Given some rules that have a cyclic dependency and no custom rules sets involved` {
        @Test
        fun `Given some rules, including an experimental rule, having only a single RunAfterRule visitor modifier then throw an exception`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RunAfterRuleFilter().filter(
                        createRuleProviders(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_B,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object : R(
                                ruleId = STANDARD_RULE_B,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_C,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object :
                                R(
                                    ruleId = STANDARD_RULE_C,
                                    visitorModifier =
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_A,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                ),
                                Rule.Experimental {},
                        ),
                    )
                }.withMessage(
                    """
                    Found cyclic dependencies between required rules that should run after another rule:
                      - Rule with id '${STANDARD_RULE_A.value}' should run after rule(s) with id '${STANDARD_RULE_B.value}'
                      - Rule with id '${STANDARD_RULE_B.value}' should run after rule(s) with id '${STANDARD_RULE_C.value}'
                      - Rule with id '${STANDARD_RULE_C.value}' should run after rule(s) with id '${STANDARD_RULE_A.value}'
                    """.trimIndent(),
                )
        }

        @Test
        fun `Given a rule with multiple RunAfterRule visitor modifier is part of a cyclic dependency then throw an exception`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RunAfterRuleFilter().filter(
                        createRuleProviders(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifiers =
                                    setOf(
                                        VisitorModifier.RunAfterRule(
                                            ruleId = CUSTOM_RULE_SET_A_RULE_B,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_B,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                    ),
                            ) {},
                            object : R(
                                ruleId = STANDARD_RULE_B,
                                visitorModifiers =
                                    setOf(
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_C,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                        VisitorModifier.RunAfterRule(
                                            ruleId = CUSTOM_RULE_SET_A_RULE_C,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                    ),
                            ) {},
                            object :
                                R(
                                    ruleId = CUSTOM_RULE_SET_A_RULE_C,
                                    visitorModifier =
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_A,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                ),
                                Rule.Experimental {},
                        ),
                    )
                }.withMessage(
                    """
                    Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [custom-rule-set-a] before creating an issue in the KtLint project. Dependencies:
                      - Rule with id '${CUSTOM_RULE_SET_A_RULE_C.value}' should run after rule(s) with id '${STANDARD_RULE_A.value}'
                      - Rule with id '${STANDARD_RULE_A.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_A_RULE_B.value}, ${STANDARD_RULE_B.value}'
                      - Rule with id '${STANDARD_RULE_B.value}' should run after rule(s) with id '${STANDARD_RULE_C.value}, ${CUSTOM_RULE_SET_A_RULE_C.value}'
                    """.trimIndent(),
                )
        }
    }

    @Nested
    inner class `Given some rules that have a cyclic dependency and custom rules sets involved` {
        @Test
        fun `Given some rules having only a single RunAfterRule visitor modifier then throw an exception`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RunAfterRuleFilter().filter(
                        createRuleProviders(
                            object : R(
                                ruleId = STANDARD_RULE_C,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = CUSTOM_RULE_SET_B_RULE_B,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_B_RULE_B,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_C,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                        ),
                    )
                }.withMessage(
                    """
                    Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
                      - Rule with id '${CUSTOM_RULE_SET_A_RULE_A.value}' should run after rule(s) with id '${STANDARD_RULE_C.value}'
                      - Rule with id '${CUSTOM_RULE_SET_B_RULE_B.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_A_RULE_A.value}'
                      - Rule with id '${STANDARD_RULE_C.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_B_RULE_B.value}'
                    """.trimIndent(),
                )
        }

        @Test
        fun `Given a rule with multiple RunAfterRule visitor modifier is part of a cyclic dependency then throw an exception`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RunAfterRuleFilter().filter(
                        createRuleProviders(
                            object : R(
                                ruleId = STANDARD_RULE_C,
                                visitorModifiers =
                                    setOf(
                                        VisitorModifier.RunAfterRule(
                                            ruleId = CUSTOM_RULE_SET_B_RULE_B,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_B,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                    ),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_B_RULE_B,
                                visitorModifiers =
                                    setOf(
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_A,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                        VisitorModifier.RunAfterRule(
                                            ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                    ),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_C,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                        ),
                    )
                }.withMessage(
                    """
                    Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
                      - Rule with id '${CUSTOM_RULE_SET_A_RULE_A.value}' should run after rule(s) with id '${STANDARD_RULE_C.value}'
                      - Rule with id '${CUSTOM_RULE_SET_B_RULE_B.value}' should run after rule(s) with id '${STANDARD_RULE_A.value}, ${CUSTOM_RULE_SET_A_RULE_A.value}'
                      - Rule with id '${STANDARD_RULE_C.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_B_RULE_B.value}, ${STANDARD_RULE_B.value}'
                    """.trimIndent(),
                )
        }
    }

    private companion object {
        const val RULE_A = "rule-a"
        val STANDARD = RuleSetId.STANDARD.value
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val CUSTOM_RULE_SET_B = "custom-rule-set-b"
        val STANDARD_RULE_A = RuleId("$STANDARD:$RULE_A")
        val STANDARD_RULE_B = RuleId("$STANDARD:rule-b")
        val STANDARD_RULE_C = RuleId("$STANDARD:rule-c")
        val EXPERIMENTAL_RULE_B = RuleId("$STANDARD:rule-b")
        val EXPERIMENTAL_RULE_C = RuleId("$STANDARD:rule-c")
        val CUSTOM_RULE_SET_A_RULE_A = RuleId("$CUSTOM_RULE_SET_A:rule-a")
        val CUSTOM_RULE_SET_A_RULE_B = RuleId("$CUSTOM_RULE_SET_A:rule-b")
        val CUSTOM_RULE_SET_A_RULE_C = RuleId("$CUSTOM_RULE_SET_A:rule-c")
        val CUSTOM_RULE_SET_B_RULE_B = RuleId("$CUSTOM_RULE_SET_B:rule-b")
    }

    private fun createRule(
        beforeRule: RuleId,
        mode: Rule.VisitorModifier.RunAfterRule.Mode,
        afterRule: RuleId,
    ): Rule =
        object : R(
            ruleId = beforeRule,
            visitorModifier =
                VisitorModifier.RunAfterRule(
                    ruleId = afterRule,
                    mode = mode,
                ),
        ) {}

    private open class R(
        ruleId: RuleId,
        visitorModifiers: Set<VisitorModifier> = emptySet(),
    ) : Rule(
            ruleId = ruleId,
            about = About(),
            visitorModifiers,
        ),
        RuleAutocorrectApproveHandler {
        constructor(ruleId: RuleId, visitorModifier: VisitorModifier) : this(ruleId, setOf(visitorModifier))

        override fun beforeVisitChildNodes(
            node: ASTNode,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        ): Unit =
            throw UnsupportedOperationException(
                "Rule should never be really invoked because that is not the aim of this unit test.",
            )
    }

    private fun createRuleProviders(vararg rules: Rule) =
        rules
            .map {
                RuleProvider { it }
            }.toSet()

    private fun Set<RuleProvider>.toRuleId() = map { it.ruleId }
}
