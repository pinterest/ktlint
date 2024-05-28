package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.ruleset.standard.rules.FUNCTION_SIGNATURE_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule
import com.pinterest.ktlint.ruleset.standard.rules.INDENTATION_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.IndentationRule
import com.pinterest.ktlint.ruleset.standard.rules.TRAILING_COMMA_ON_CALL_SITE_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnCallSiteRule
import com.pinterest.ktlint.ruleset.standard.rules.WRAPPING_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.WrappingRule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RuleProviderSorterTest {
    @Test
    fun `Multiple normal rules in the same rule set are run in alphabetical order`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            NormalRule(STANDARD_RULE_B),
                            NormalRule(STANDARD_RULE_A),
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
        )
    }

    @Test
    fun `Multiple normal rules in different rule sets are run in alphabetical order but grouped in order standard, experimental and custom`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            ExperimentalRule(STANDARD_RULE_B),
                            ExperimentalRule(STANDARD_RULE_A),
                            NormalRule(CUSTOM_RULE_SET_A_RULE_B),
                            NormalRule(CUSTOM_RULE_SET_A_RULE_A),
                            NormalRule(STANDARD_RULE_D),
                            NormalRule(STANDARD_RULE_C),
                            NormalRule(CUSTOM_RULE_SET_B_RULE_B),
                            NormalRule(CUSTOM_RULE_SET_B_RULE_A),
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_D,
            // Rules from custom rule sets are all grouped together
            CUSTOM_RULE_SET_A_RULE_A,
            CUSTOM_RULE_SET_A_RULE_B,
            CUSTOM_RULE_SET_B_RULE_A,
            CUSTOM_RULE_SET_B_RULE_B,
        )
    }

    @Test
    fun `A run as late as possible rule runs after the rules not marked to run as late as possible`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            NormalRule(STANDARD_RULE_C),
                            RunAsLateAsPossibleRule(STANDARD_RULE_A),
                            NormalRule(STANDARD_RULE_B),
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_A,
        )
    }

    @Test
    fun `Multiple run as late as possible rules in the same rule set are sorted alphabetically`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            RunAsLateAsPossibleExperimentalRule(STANDARD_RULE_B),
                            RunAsLateAsPossibleExperimentalRule(STANDARD_RULE_A),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_A_RULE_B),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_A_RULE_A),
                            RunAsLateAsPossibleRule(STANDARD_RULE_D),
                            RunAsLateAsPossibleRule(STANDARD_RULE_C),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_B_RULE_B),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_B_RULE_A),
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_D,
            // Rules from custom rule sets are all grouped together
            CUSTOM_RULE_SET_A_RULE_A,
            CUSTOM_RULE_SET_A_RULE_B,
            CUSTOM_RULE_SET_B_RULE_A,
            CUSTOM_RULE_SET_B_RULE_B,
        )
    }

    @Test
    fun `Multiple run as late as possible on root node only rules in the same rule set are sorted alphabetically`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            RunAsLateAsPossibleExperimentalRule(STANDARD_RULE_B),
                            RunAsLateAsPossibleExperimentalRule(STANDARD_RULE_A),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_A_RULE_B),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_A_RULE_A),
                            RunAsLateAsPossibleRule(STANDARD_RULE_D),
                            RunAsLateAsPossibleRule(STANDARD_RULE_C),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_B_RULE_B),
                            RunAsLateAsPossibleRule(CUSTOM_RULE_SET_B_RULE_A),
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_D,
            // Rules from custom rule sets are all grouped together
            CUSTOM_RULE_SET_A_RULE_A,
            CUSTOM_RULE_SET_A_RULE_B,
            CUSTOM_RULE_SET_B_RULE_A,
            CUSTOM_RULE_SET_B_RULE_B,
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the same rule set runs after that rule and override the alphabetical sort order`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_C,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            NormalRule(STANDARD_RULE_B),
                            object : R(
                                ruleId = STANDARD_RULE_D,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_B,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object : R(
                                ruleId = STANDARD_RULE_C,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_B,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_A,
            STANDARD_RULE_D,
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the different rule set runs after that rule and override the alphabetical sort order`() {
        val actual =
            RuleProviderSorter()
                .getSortedRuleProviders(
                    ruleProviders =
                        createRuleProviders(
                            NormalRule(STANDARD_RULE_B),
                            object : R(
                                ruleId = STANDARD_RULE_D,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_B,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object : R(
                                ruleId = STANDARD_RULE_C,
                                visitorModifier =
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_B,
                                        mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                    ),
                            ) {},
                            object :
                                R(
                                    ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                    visitorModifier =
                                        VisitorModifier.RunAfterRule(
                                            ruleId = STANDARD_RULE_C,
                                            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                        ),
                                ),
                                Rule.Experimental {},
                        ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_D,
            CUSTOM_RULE_SET_A_RULE_A,
        )
    }

    @Nested
    inner class `Given the IndentationRule, TrailingCommaOnCallSiteRule, WrappingRule and FunctionSignatureRule` {
        @Test
        fun `Given that the experimental FunctionSignatureRule is not included in the rules to be sorted`() {
            val actual =
                RuleProviderSorter()
                    .getSortedRuleProviders(
                        ruleProviders =
                            createRuleProviders(
                                IndentationRule(),
                                TrailingCommaOnCallSiteRule(),
                                WrappingRule(),
                            ),
                    ).map { it.ruleId }

            assertThat(actual).containsExactly(
                WRAPPING_RULE_ID,
                TRAILING_COMMA_ON_CALL_SITE_RULE_ID,
                INDENTATION_RULE_ID,
            )
        }

        @Test
        fun `Given that the experimental FunctionSignatureRule is included in the rules to be sorted`() {
            val actual =
                RuleProviderSorter()
                    .getSortedRuleProviders(
                        ruleProviders =
                            createRuleProviders(
                                IndentationRule(),
                                TrailingCommaOnCallSiteRule(),
                                WrappingRule(),
                                FunctionSignatureRule(),
                            ),
                    ).map { it.ruleId }

            assertThat(actual).containsExactly(
                WRAPPING_RULE_ID,
                FUNCTION_SIGNATURE_RULE_ID,
                TRAILING_COMMA_ON_CALL_SITE_RULE_ID,
                INDENTATION_RULE_ID,
            )
        }
    }

    @Nested
    inner class `Given a rule with a RunAfterRule visitor modifier on a rule which is not required to be loaded then run the rule` {
        @Test
        fun `Given a rule with a single RunAfterRule visitor modifier`() {
            val actual =
                RuleProviderSorter()
                    .getSortedRuleProviders(
                        ruleProviders =
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
                    ).map { it.ruleId }

            assertThat(actual).containsExactly(
                STANDARD_RULE_A,
            )
        }

        @Test
        fun `Given a rule with a multiple RunAfterRule visitor modifiers`() {
            val actual =
                RuleProviderSorter()
                    .getSortedRuleProviders(
                        ruleProviders =
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
                    ).map { it.ruleId }

            assertThat(actual).containsExactly(
                STANDARD_RULE_A,
                STANDARD_RULE_B,
            )
        }
    }

    @Nested
    inner class `Rule has no visitor modifier referring to self` {
        @Test
        fun `Given a rule having a single RunAfterRule visitor modifier which refers to the rule itself then throw an exception`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RuleProviderSorter()
                        .getSortedRuleProviders(
                            ruleProviders =
                                createRuleProviders(
                                    object : R(
                                        ruleId = STANDARD_RULE_A,
                                        visitorModifiers =
                                            setOf(
                                                VisitorModifier.RunAfterRule(
                                                    ruleId = STANDARD_RULE_A,
                                                    mode = ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED,
                                                ),
                                            ),
                                    ) {},
                                ),
                        ).map { it.ruleId }
                }.withMessage(
                    "Rule with id '${STANDARD_RULE_A.value}' has a visitor modifier of type 'RunAfterRule' which may not refer to the " +
                        "rule itself.",
                )
        }

        @Test
        fun `Given a rule having multiple RunAfterRule visitor modifiers of which one refers to the rule itself then throw an exception`() {
            assertThatIllegalStateException()
                .isThrownBy {
                    RuleProviderSorter()
                        .getSortedRuleProviders(
                            ruleProviders =
                                createRuleProviders(
                                    object : R(
                                        ruleId = STANDARD_RULE_A,
                                        visitorModifiers =
                                            setOf(
                                                VisitorModifier.RunAfterRule(
                                                    ruleId = STANDARD_RULE_A,
                                                    mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                                ),
                                                VisitorModifier.RunAfterRule(
                                                    ruleId = STANDARD_RULE_B,
                                                    mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                                ),
                                            ),
                                    ) {},
                                ),
                        ).map { it.ruleId }
                }.withMessage(
                    "Rule with id '${STANDARD_RULE_A.value}' has a visitor modifier of type 'RunAfterRule' which may not refer to the " +
                        "rule itself.",
                )
        }
    }

    private fun createRuleProviders(vararg rules: Rule): Set<RuleProvider> =
        rules
            .map {
                RuleProvider { it }
            }.toSet()

    private companion object {
        const val RULE_A = "rule-a"
        val STANDARD = RuleSetId.STANDARD.value
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val CUSTOM_RULE_SET_B = "custom-rule-set-b"
        val STANDARD_RULE_A = RuleId("$STANDARD:$RULE_A")
        val STANDARD_RULE_B = RuleId("$STANDARD:rule-b")
        val STANDARD_RULE_C = RuleId("$STANDARD:rule-c")
        val STANDARD_RULE_D = RuleId("$STANDARD:rule-d")
        val CUSTOM_RULE_SET_A_RULE_A = RuleId("$CUSTOM_RULE_SET_A:rule-a")
        val CUSTOM_RULE_SET_A_RULE_B = RuleId("$CUSTOM_RULE_SET_A:rule-b")
        val CUSTOM_RULE_SET_B_RULE_A = RuleId("$CUSTOM_RULE_SET_B:rule-a")
        val CUSTOM_RULE_SET_B_RULE_B = RuleId("$CUSTOM_RULE_SET_B:rule-b")
    }

    private open class NormalRule(
        ruleId: RuleId,
    ) : R(ruleId)

    private open class ExperimentalRule(
        ruleId: RuleId,
    ) : R(ruleId),
        Rule.Experimental

    private class RunAsLateAsPossibleRule(
        ruleId: RuleId,
    ) : R(
            ruleId = ruleId,
            visitorModifiers =
                setOf(
                    VisitorModifier.RunAsLateAsPossible,
                ),
        )

    private class RunAsLateAsPossibleExperimentalRule(
        ruleId: RuleId,
    ) : R(
            ruleId = ruleId,
            visitorModifiers =
                setOf(
                    VisitorModifier.RunAsLateAsPossible,
                ),
        ),
        Rule.Experimental

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
}
