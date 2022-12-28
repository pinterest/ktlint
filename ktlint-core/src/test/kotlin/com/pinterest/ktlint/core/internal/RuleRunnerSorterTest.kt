package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
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
                        NormalRule(RULE_B),
                        NormalRule(RULE_A),
                    ),
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            RULE_A,
            RULE_B,
        )
    }

    @Test
    fun `Multiple normal rules in different rule sets are run in alphabetical order but grouped in order standard, experimental and custom`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule("$EXPERIMENTAL:$RULE_B"),
                        NormalRule("$EXPERIMENTAL:$RULE_A"),
                        NormalRule("$CUSTOM_RULE_SET_A:$RULE_B"),
                        NormalRule("$CUSTOM_RULE_SET_A:$RULE_A"),
                        NormalRule(RULE_B),
                        NormalRule(RULE_A),
                        NormalRule("$CUSTOM_RULE_SET_B:$RULE_B"),
                        NormalRule("$CUSTOM_RULE_SET_B:$RULE_A"),
                    ),
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_A",
            "$STANDARD:$RULE_B",
            "$EXPERIMENTAL:$RULE_A",
            "$EXPERIMENTAL:$RULE_B",
            // Rules from custom rule sets are all grouped together
            "$CUSTOM_RULE_SET_A:$RULE_A",
            "$CUSTOM_RULE_SET_A:$RULE_B",
            "$CUSTOM_RULE_SET_B:$RULE_A",
            "$CUSTOM_RULE_SET_B:$RULE_B",
        )
    }

    @Test
    fun `A run as late as possible rule runs after the rules not marked to run as late as possible`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule(RULE_C),
                        RunAsLateAsPossibleRule(RULE_A),
                        NormalRule(RULE_B),
                    ),
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            RULE_B,
            RULE_C,
            RULE_A,
        )
    }

    @Test
    fun `Multiple run as late as possible rules in the same rule set are sorted alphabetically`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        RunAsLateAsPossibleRule("$EXPERIMENTAL:$RULE_B"),
                        RunAsLateAsPossibleRule("$EXPERIMENTAL:$RULE_A"),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_A:$RULE_B"),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_A:$RULE_A"),
                        RunAsLateAsPossibleRule(RULE_B),
                        RunAsLateAsPossibleRule(RULE_A),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_B:$RULE_B"),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_B:$RULE_A"),
                    ),
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_A",
            "$STANDARD:$RULE_B",
            "$EXPERIMENTAL:$RULE_A",
            "$EXPERIMENTAL:$RULE_B",
            // Rules from custom rule sets are all grouped together
            "$CUSTOM_RULE_SET_A:$RULE_A",
            "$CUSTOM_RULE_SET_A:$RULE_B",
            "$CUSTOM_RULE_SET_B:$RULE_A",
            "$CUSTOM_RULE_SET_B:$RULE_B",
        )
    }

    @Test
    fun `Multiple run as late as possible on root node only rules in the same rule set are sorted alphabetically`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        RunAsLateAsPossibleRule("$EXPERIMENTAL:$RULE_B"),
                        RunAsLateAsPossibleRule("$EXPERIMENTAL:$RULE_A"),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_A:$RULE_B"),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_A:$RULE_A"),
                        RunAsLateAsPossibleRule(RULE_B),
                        RunAsLateAsPossibleRule(RULE_A),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_B:$RULE_B"),
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_B:$RULE_A"),
                    ),
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_A",
            "$STANDARD:$RULE_B",
            "$EXPERIMENTAL:$RULE_A",
            "$EXPERIMENTAL:$RULE_B",
            // Rules from custom rule sets are all grouped together
            "$CUSTOM_RULE_SET_A:$RULE_A",
            "$CUSTOM_RULE_SET_A:$RULE_B",
            "$CUSTOM_RULE_SET_B:$RULE_A",
            "$CUSTOM_RULE_SET_B:$RULE_B",
        )
    }

    @Nested
    inner class `Given a rule having a RunAfterRule visitor modifier then this modifier can not refer to the rule itself` {
        @ParameterizedTest(name = "Rule id: {0}")
        @ValueSource(
            strings = [
                RULE_A,
                "$CUSTOM_RULE_SET_A:$RULE_A",
            ],
        )
        fun `Given a rule with a single RunAfterRule modifier`(ruleId: String) {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                id = ruleId,
                                visitorModifier = VisitorModifier.RunAfterRule(ruleId),
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
                RULE_A,
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
                                id = ruleId,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule("some-other-rule-id"),
                                    VisitorModifier.RunAfterRule(ruleId),
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
                            id = RULE_A,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_C),
                        ) {},
                        NormalRule(RULE_B),
                        object : R(
                            id = RULE_D,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B),
                        ) {},
                        object : R(
                            id = RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B),
                        ) {},
                    ),
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_B",
            "$STANDARD:$RULE_C",
            "$STANDARD:$RULE_D",
            // RULE_D is ordered before RULE_A because rules are evaluated in order of the initial sorting (A, B, C, D). In the first
            // iteration of the rules, RULE_A is blocked because rule C is not yet added. RULE_B, RULE_C and RULE_D can be added during the
            // first iteration as the rules are not blocked when they are evaluated. In the second iteration, RULE_A can be added as well.
            "$STANDARD:$RULE_A",
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the different rule set runs after that rule and override the alphabetical sort order`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule(RULE_B),
                        object : R(
                            id = RULE_D,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B),
                        ) {},
                        object : R(
                            id = RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B),
                        ) {},
                        object : R(
                            id = "$EXPERIMENTAL:$RULE_A",
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_C),
                        ) {},
                    ),
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_B",
            "$STANDARD:$RULE_C",
            "$STANDARD:$RULE_D",
            "$EXPERIMENTAL:$RULE_A",
        )
    }

    @Nested
    inner class `Given the IndentationRule, TrailingCommaOnCallSiteRule, WrappingRule and FunctionSignatureRule` {
        // Rule definitions below are extracted from the corresponding rules form the Standard and Experimental rule
        // sets of ktlint
        private val indentationRule =
            object : Rule(
                id = "indent",
                visitorModifiers = setOf(
                    VisitorModifier.RunAsLateAsPossible,
                    VisitorModifier.RunAfterRule(
                        ruleId = "experimental:function-signature",
                        loadOnlyWhenOtherRuleIsLoaded = false,
                        runOnlyWhenOtherRuleIsEnabled = false,
                    ),
                    VisitorModifier.RunAfterRule(
                        ruleId = "trailing-comma-on-call-site",
                        loadOnlyWhenOtherRuleIsLoaded = false,
                        runOnlyWhenOtherRuleIsEnabled = false,
                    ),
                    VisitorModifier.RunAfterRule(
                        ruleId = "trailing-comma-on-declaration-site",
                        loadOnlyWhenOtherRuleIsLoaded = false,
                        runOnlyWhenOtherRuleIsEnabled = false,
                    ),
                ),
            ) {}
        private val trailingCommaOnCallSiteRule =
            object : Rule(
                id = "trailing-comma-on-call-site",
                visitorModifiers = setOf(
                    VisitorModifier.RunAfterRule(
                        ruleId = "standard:wrapping",
                        loadOnlyWhenOtherRuleIsLoaded = true,
                        runOnlyWhenOtherRuleIsEnabled = true,
                    ),
                    VisitorModifier.RunAsLateAsPossible,
                ),
            ) {}
        private val wrappingRule = object : Rule("wrapping") {}
        private val functionSignatureRule =
            object : Rule(
                id = "experimental:function-signature",
                visitorModifiers = setOf(
                    // Run after wrapping and spacing rules
                    VisitorModifier.RunAsLateAsPossible,
                ),
            ) {}

        @Test
        fun `Given that the experimental FunctionSignatureRule is not included in the rules to be sorted`() {
            val actual =
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            indentationRule,
                            trailingCommaOnCallSiteRule,
                            wrappingRule,
                        ),
                    ).map { it.qualifiedRuleId }

            assertThat(actual).containsExactly(
                "standard:wrapping",
                "standard:trailing-comma-on-call-site",
                "standard:indent",
            )
        }

        @Test
        fun `Given that the experimental FunctionSignatureRule is included in the rules to be sorted`() {
            val actual =
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            indentationRule,
                            trailingCommaOnCallSiteRule,
                            wrappingRule,
                            functionSignatureRule,
                        ),
                    ).map { it.qualifiedRuleId }

            assertThat(actual).containsExactly(
                "standard:wrapping",
                "standard:trailing-comma-on-call-site",
                "experimental:function-signature",
                "standard:indent",
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
                                id = RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(
                                    ruleId = RULE_B,
                                    loadOnlyWhenOtherRuleIsLoaded = true,
                                ),
                            ) {},
                            object : R(
                                id = "$EXPERIMENTAL:$RULE_C",
                                visitorModifier = VisitorModifier.RunAfterRule(
                                    ruleId = "$EXPERIMENTAL:$RULE_B",
                                    loadOnlyWhenOtherRuleIsLoaded = true,
                                ),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to additional rule sets before creating an issue.
                  - Rule with id '$STANDARD:$RULE_A' requires rule with id '$STANDARD:$RULE_B' to be loaded
                  - Rule with id '$EXPERIMENTAL:$RULE_C' requires rule with id '$EXPERIMENTAL:$RULE_B' to be loaded
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
                                id = RULE_A,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(RULE_B),
                                    VisitorModifier.RunAfterRule(
                                        ruleId = RULE_C,
                                        loadOnlyWhenOtherRuleIsLoaded = true,
                                    ),
                                ),
                            ) {},
                            object : R(
                                id = "$EXPERIMENTAL:$RULE_B",
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(
                                        ruleId = "$EXPERIMENTAL:$RULE_C",
                                        loadOnlyWhenOtherRuleIsLoaded = true,
                                    ),
                                ),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Skipping rule(s) which are depending on a rule which is not loaded. Please check if you need to additional rule sets before creating an issue.
                  - Rule with id '$STANDARD:$RULE_A' requires rule with id '$STANDARD:$RULE_C' to be loaded
                  - Rule with id '$EXPERIMENTAL:$RULE_B' requires rule with id '$EXPERIMENTAL:$RULE_C' to be loaded
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
                                id = RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(
                                    ruleId = "not-loaded-rule",
                                    loadOnlyWhenOtherRuleIsLoaded = false,
                                ),
                            ) {},
                        ),
                    ).map { it.qualifiedRuleId }

            assertThat(actual).containsExactly(
                "$STANDARD:$RULE_A",
            )
        }

        @Test
        fun `Given a rule with a multiple RunAfterRule visitor modifiers`() {
            val actual =
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(RULE_A) {},
                            object : R(
                                id = RULE_B,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(RULE_A),
                                    VisitorModifier.RunAfterRule(
                                        ruleId = "not-loaded-rule",
                                        loadOnlyWhenOtherRuleIsLoaded = false,
                                    ),
                                ),
                            ) {},
                        ),
                    ).map { it.qualifiedRuleId }

            assertThat(actual).containsExactly(
                "$STANDARD:$RULE_A",
                "$STANDARD:$RULE_B",
            )
        }
    }

    @Nested
    inner class `Given some rules that have a cyclic dependency and no custom rules sets involved` {
        @Test
        fun `Given some rules having only a single RunAfterRule visitor modifier then throw an exception`() {
            assertThatIllegalStateException().isThrownBy {
                RuleRunnerSorter()
                    .getSortedRuleRunners(
                        ruleRunners = createRuleRunners(
                            object : R(
                                id = RULE_A,
                                visitorModifier = VisitorModifier.RunAfterRule(RULE_B),
                            ) {},
                            object : R(
                                id = RULE_B,
                                visitorModifier = VisitorModifier.RunAfterRule("$EXPERIMENTAL:$RULE_C"),
                            ) {},
                            object : R(
                                id = "$EXPERIMENTAL:$RULE_C",
                                visitorModifier = VisitorModifier.RunAfterRule(RULE_A),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule:
                  - Rule with id '$STANDARD:$RULE_A' should run after rule(s) with id '$STANDARD:$RULE_B'
                  - Rule with id '$STANDARD:$RULE_B' should run after rule(s) with id '$EXPERIMENTAL:$RULE_C'
                  - Rule with id '$EXPERIMENTAL:$RULE_C' should run after rule(s) with id '$STANDARD:$RULE_A'
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
                                id = RULE_A,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_B"),
                                    VisitorModifier.RunAfterRule(RULE_B),
                                ),
                            ) {},
                            object : R(
                                id = RULE_B,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(RULE_C),
                                    VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_C"),
                                ),
                            ) {},
                            object : R(
                                id = "$CUSTOM_RULE_SET_A:$RULE_C",
                                visitorModifier = VisitorModifier.RunAfterRule(RULE_A),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [custom-rule-set-a] before creating an issue in the KtLint project. Dependencies:
                  - Rule with id '$STANDARD:$RULE_A' should run after rule(s) with id '$STANDARD:$RULE_B'
                  - Rule with id '$STANDARD:$RULE_B' should run after rule(s) with id '$CUSTOM_RULE_SET_A:$RULE_C'
                  - Rule with id '$CUSTOM_RULE_SET_A:$RULE_C' should run after rule(s) with id '$STANDARD:$RULE_A'
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
                                id = RULE_C,
                                visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_B:$RULE_B"),
                            ) {},
                            object : R(
                                id = "$CUSTOM_RULE_SET_B:$RULE_B",
                                visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_A"),
                            ) {},
                            object : R(
                                id = "$CUSTOM_RULE_SET_A:$RULE_A",
                                visitorModifier = VisitorModifier.RunAfterRule("$STANDARD:$RULE_C"),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
                  - Rule with id '$STANDARD:$RULE_C' should run after rule(s) with id '$CUSTOM_RULE_SET_B:$RULE_B'
                  - Rule with id '$CUSTOM_RULE_SET_A:$RULE_A' should run after rule(s) with id '$STANDARD:$RULE_C'
                  - Rule with id '$CUSTOM_RULE_SET_B:$RULE_B' should run after rule(s) with id '$CUSTOM_RULE_SET_A:$RULE_A'
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
                                id = RULE_C,
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_B:$RULE_B"),
                                    VisitorModifier.RunAfterRule(RULE_B),
                                ),
                            ) {},
                            object : R(
                                id = "$CUSTOM_RULE_SET_B:$RULE_B",
                                visitorModifiers = setOf(
                                    VisitorModifier.RunAfterRule(RULE_A),
                                    VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_A"),
                                ),
                            ) {},
                            object : R(
                                id = "$CUSTOM_RULE_SET_A:$RULE_A",
                                visitorModifier = VisitorModifier.RunAfterRule("$STANDARD:$RULE_C"),
                            ) {},
                        ),
                    )
            }.withMessage(
                """
                Found cyclic dependencies between required rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
                  - Rule with id '$STANDARD:$RULE_C' should run after rule(s) with id '$CUSTOM_RULE_SET_B:$RULE_B'
                  - Rule with id '$CUSTOM_RULE_SET_A:$RULE_A' should run after rule(s) with id '$STANDARD:$RULE_C'
                  - Rule with id '$CUSTOM_RULE_SET_B:$RULE_B' should run after rule(s) with id '$CUSTOM_RULE_SET_A:$RULE_A'
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
        const val STANDARD = "standard"
        const val EXPERIMENTAL = "experimental"
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val CUSTOM_RULE_SET_B = "custom-rule-set-b"
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val RULE_D = "rule-d"
    }

    open class NormalRule(id: String) : R(id)

    class RunAsLateAsPossibleRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible,
        ),
    )

    open class R(
        id: String,
        visitorModifiers: Set<VisitorModifier> = emptySet(),
    ) : Rule(id, visitorModifiers) {
        constructor(id: String, visitorModifier: VisitorModifier) : this(id, setOf(visitorModifier))

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
