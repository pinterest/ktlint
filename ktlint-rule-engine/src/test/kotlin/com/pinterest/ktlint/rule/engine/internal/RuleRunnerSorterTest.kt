package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.RuleId
import com.pinterest.ktlint.ruleset.core.api.RuleProvider
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RuleRunnerSorterTest {
    @Test
    fun `Multiple normal rules in the same rule set are run in alphabetical order`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule(STANDARD_RULE_B),
                        NormalRule(STANDARD_RULE_A),
                    ),
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
        )
    }

    @Test
    fun `Multiple normal rules in different rule sets are run in alphabetical order but grouped in order standard, experimental and custom`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
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
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule(STANDARD_RULE_C),
                        RunAsLateAsPossibleRule(STANDARD_RULE_A),
                        NormalRule(STANDARD_RULE_B),
                    ),
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_A,
        )
    }

    @Test
    fun `Multiple run as late as possible rules in the same rule set are sorted alphabetically`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
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
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
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

    @Nested
    inner class `Given a rule having a RunAfterRule visitor modifier then this modifier can not refer to the rule itself` {
        @ParameterizedTest(name = "Rule id: {0}")
        @ValueSource(
            strings = [
                "$STANDARD:$RULE_A",
                "$CUSTOM_RULE_SET_A:$RULE_A",
            ],
        )
        fun `Given a rule with a single RunAfterRule modifier`(ruleId: String) {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = RuleId(ruleId),
                                visitorModifier = VisitorModifier.RunAfterRule(RuleId(ruleId)),
                            ) {},
                        ),
                    )
            }.withMessage(
                "Rule with id '$ruleId' has a visitor modifier of type 'RunAfterRule' but it is not referring to another " +
                    "rule but to the rule itself. A rule can not run after itself. This should be fixed by the maintainer " +
                    "of the rule.",
            )
        }

        @ParameterizedTest(name = "Rule id: {0}")
        @ValueSource(
            strings = [
                "$STANDARD:$RULE_A",
                "$CUSTOM_RULE_SET_A:$RULE_A",
            ],
        )
        fun `A rule having multiple RunAfterRule visitor modifiers then none of those modifiers may refer to the rule itself`(
            ruleId: String,
        ) {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = RuleId(ruleId),
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(RuleId("custom:some-other-rule-id")),
                                    VisitorModifier.RunAfterRule(RuleId(ruleId)),
                                ),
                            ) {},
                        ),
                    )
            }.withMessage(
                "Rule with id '$ruleId' has a visitor modifier of type 'RunAfterRule' but it is not referring to another " +
                    "rule but to the rule itself. A rule can not run after itself. This should be fixed by the maintainer " +
                    "of the rule.",
            )
        }
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the same rule set runs after that rule and override the alphabetical sort order`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            ruleId = STANDARD_RULE_A,
                            visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_C),
                        ) {},
                        NormalRule(STANDARD_RULE_B),
                        object : R(
                            ruleId = STANDARD_RULE_D,
                            visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                        ) {},
                        object : R(
                            ruleId = STANDARD_RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                        ) {},
                    ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_D,
            // RULE_D is ordered before RULE_A because rules are evaluated in order of the initial sorting (A, B, C, D). In the first
            // iteration of the rules, RULE_A is blocked because rule C is not yet added. RULE_B, RULE_C and RULE_D can be added during the
            // first iteration as the rules are not blocked when they are evaluated. In the second iteration, RULE_A can be added as well.
            STANDARD_RULE_A,
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the different rule set runs after that rule and override the alphabetical sort order`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule(STANDARD_RULE_B),
                        object : R(
                            ruleId = STANDARD_RULE_D,
                            visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                        ) {},
                        object : R(
                            ruleId = STANDARD_RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                        ) {},
                        object :
                            R(
                                ruleId = EXPERIMENTAL_RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_C),
                            ),
                            Rule.Experimental {},
                    ),
                ).map { it.ruleId }

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            STANDARD_RULE_C,
            STANDARD_RULE_D,
            EXPERIMENTAL_RULE_A,
        )
    }

    @Nested
    inner class `Given the IndentationRule, TrailingCommaOnCallSiteRule, WrappingRule and FunctionSignatureRule` {
        @Test
        fun `Given that the experimental FunctionSignatureRule is not included in the rules to be sorted`() {
            val actual =
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
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
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
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
    inner class `Given a rule with a RunAfterRule visitor modifier for a rule which is required to be loaded then throw an exception` {
        @Test
        fun `Given that the rule contains a single visitor modifier`() {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(
                                    ruleId = STANDARD_RULE_B,
                                    loadOnlyWhenOtherRuleIsLoaded = true,
                                ),
                            ) {},
                            object :
                                R(
                                    ruleId = EXPERIMENTAL_RULE_C,
                                    visitorModifier = VisitorModifier.RunAfterRule(
                                        ruleId = EXPERIMENTAL_RULE_B,
                                        loadOnlyWhenOtherRuleIsLoaded = true,
                                    ),
                                ),
                                Rule.Experimental {},
                        ),
                    )
            }.withMessage(
                """
                Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to additional rule sets before creating an issue.
                  - Rule with id '$STANDARD_RULE_A' requires rule with id '$STANDARD_RULE_B' to be loaded
                  - Rule with id '$EXPERIMENTAL_RULE_C' requires rule with id '$EXPERIMENTAL_RULE_B' to be loaded
                """.trimIndent(),
            )
        }

        @Test
        fun `Given that the rule contains multiple visitor modifiers`() {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                                    VisitorModifier.RunAfterRule(
                                        ruleId = STANDARD_RULE_C,
                                        loadOnlyWhenOtherRuleIsLoaded = true,
                                    ),
                                ),
                            ) {},
                            object :
                                R(
                                    ruleId = EXPERIMENTAL_RULE_B,
                                    visitorModifiers = setOf(
                                        VisitorModifier.RunAfterRule(
                                            ruleId = EXPERIMENTAL_RULE_C,
                                            loadOnlyWhenOtherRuleIsLoaded = true,
                                        ),
                                    ),
                                ),
                                Rule.Experimental {},
                        ),
                    )
            }.withMessage(
                """
                Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to additional rule sets before creating an issue.
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
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(
                                    ruleId = RuleId("test:not-loaded-rule"),
                                    loadOnlyWhenOtherRuleIsLoaded = false,
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
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(STANDARD_RULE_A) {},
                            object : R(
                                ruleId = STANDARD_RULE_B,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(STANDARD_RULE_A),
                                    VisitorModifier.RunAfterRule(
                                        ruleId = RuleId("test:not-loaded-rule"),
                                        loadOnlyWhenOtherRuleIsLoaded = false,
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
    inner class `Given some rules that have a cyclic dependency and no custom rules sets involved` {
        @Test
        fun `Given some rules, including an experimental rule, having only a single RunAfterRule visitor modifier then throw an exception`() {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                            ) {},
                            object : R(
                                ruleId = STANDARD_RULE_B,
                                visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_C),
                            ) {},
                            object :
                                R(
                                    ruleId = STANDARD_RULE_C,
                                    visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_A),
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
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_A,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(CUSTOM_RULE_SET_A_RULE_B),
                                    VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                                ),
                            ) {},
                            object : R(
                                ruleId = STANDARD_RULE_B,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(STANDARD_RULE_C),
                                    VisitorModifier.RunAfterRule(CUSTOM_RULE_SET_A_RULE_C),
                                ),
                            ) {},
                            object :
                                R(
                                    ruleId = CUSTOM_RULE_SET_A_RULE_C,
                                    visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_A),
                                ),
                                Rule.Experimental {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [custom-rule-set-a] before creating an issue in the KtLint project. Dependencies:
                  - Rule with id '${STANDARD_RULE_A.value}' should run after rule(s) with id '${STANDARD_RULE_B.value}'
                  - Rule with id '${STANDARD_RULE_B.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_A_RULE_C.value}'
                  - Rule with id '${CUSTOM_RULE_SET_A_RULE_C.value}' should run after rule(s) with id '${STANDARD_RULE_A.value}'
                """.trimIndent(),
            )
        }
    }

    @Nested
    inner class `Given some rules that have a cyclic dependency and custom rules sets involved` {
        @Test
        fun `Given some rules having only a single RunAfterRule visitor modifier then throw an exception`() {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_C,
                                visitorModifier = VisitorModifier.RunAfterRule(CUSTOM_RULE_SET_B_RULE_B),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_B_RULE_B,
                                visitorModifier = VisitorModifier.RunAfterRule(CUSTOM_RULE_SET_A_RULE_A),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_C),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
                  - Rule with id '${STANDARD_RULE_C.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_B_RULE_B.value}'
                  - Rule with id '${CUSTOM_RULE_SET_A_RULE_A.value}' should run after rule(s) with id '${STANDARD_RULE_C.value}'
                  - Rule with id '${CUSTOM_RULE_SET_B_RULE_B.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_A_RULE_A.value}'
                """.trimIndent(),
            )
        }

        @Test
        fun `Given a rule with multiple RunAfterRule visitor modifier is part of a cyclic dependency then throw an exception`() {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                ruleId = STANDARD_RULE_C,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(CUSTOM_RULE_SET_B_RULE_B),
                                    VisitorModifier.RunAfterRule(STANDARD_RULE_B),
                                ),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_B_RULE_B,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(STANDARD_RULE_A),
                                    VisitorModifier.RunAfterRule(CUSTOM_RULE_SET_A_RULE_A),
                                ),
                            ) {},
                            object : R(
                                ruleId = CUSTOM_RULE_SET_A_RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(STANDARD_RULE_C),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
                  - Rule with id '${STANDARD_RULE_C.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_B_RULE_B.value}'
                  - Rule with id '${CUSTOM_RULE_SET_A_RULE_A.value}' should run after rule(s) with id '${STANDARD_RULE_C.value}'
                  - Rule with id '${CUSTOM_RULE_SET_B_RULE_B.value}' should run after rule(s) with id '${CUSTOM_RULE_SET_A_RULE_A.value}'
                """.trimIndent(),
            )
        }
    }

    private fun createRuleRunners(vararg rules: Rule): Set<RuleRunner> =
        rules
            .map {
                RuleRunner(
                    RuleProvider { it },
                )
            }.toSet()

    private companion object {
        const val RULE_A = "rule-a"
        const val STANDARD = "standard"
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val CUSTOM_RULE_SET_B = "custom-rule-set-b"
        val STANDARD_RULE_A = RuleId("$STANDARD:$RULE_A")
        val STANDARD_RULE_B = RuleId("$STANDARD:rule-b")
        val STANDARD_RULE_C = RuleId("$STANDARD:rule-c")
        val STANDARD_RULE_D = RuleId("$STANDARD:rule-d")
        val EXPERIMENTAL_RULE_A = RuleId("$STANDARD:rule-a")
        val EXPERIMENTAL_RULE_B = RuleId("$STANDARD:rule-b")
        val EXPERIMENTAL_RULE_C = RuleId("$STANDARD:rule-c")
        val CUSTOM_RULE_SET_A_RULE_A = RuleId("$CUSTOM_RULE_SET_A:rule-a")
        val CUSTOM_RULE_SET_A_RULE_B = RuleId("$CUSTOM_RULE_SET_A:rule-b")
        val CUSTOM_RULE_SET_A_RULE_C = RuleId("$CUSTOM_RULE_SET_A:rule-c")
        val CUSTOM_RULE_SET_B_RULE_A = RuleId("$CUSTOM_RULE_SET_B:rule-a")
        val CUSTOM_RULE_SET_B_RULE_B = RuleId("$CUSTOM_RULE_SET_B:rule-b")
    }

    private open class NormalRule(ruleId: RuleId) : R(ruleId)

    private open class ExperimentalRule(ruleId: RuleId) : R(ruleId), Rule.Experimental

    private class RunAsLateAsPossibleRule(ruleId: RuleId) : R(
        ruleId = ruleId,
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible,
        ),
    )

    private class RunAsLateAsPossibleExperimentalRule(ruleId: RuleId) :
        R(
            ruleId = ruleId,
            visitorModifiers = setOf(
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
        visitorModifiers
    ) {
        constructor(ruleId: RuleId, visitorModifier: VisitorModifier) : this(ruleId, setOf(visitorModifier))
        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ) {
            throw UnsupportedOperationException(
                "Rule should never be really invoked because that is not the aim of this unit test.",
            )
        }
    }
}
