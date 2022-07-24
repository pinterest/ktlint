package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
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
                        NormalRule(RULE_A)
                    ),
                    debug = true
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            RULE_A,
            RULE_B
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
                        NormalRule("$CUSTOM_RULE_SET_B:$RULE_A")
                    ),
                    debug = true
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
            "$CUSTOM_RULE_SET_B:$RULE_B"
        )
    }

    @Test
    fun `Root only rule is run before non-root-only rule`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        RootNodeOnlyRule(ROOT_NODE_ONLY_RULE),
                        NormalRule(NORMAL_RULE)
                    ),
                    debug = true
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            ROOT_NODE_ONLY_RULE,
            NORMAL_RULE
        )
    }

    @Test
    fun `Multiple root only rules in the same rule set are run in alphabetical order`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        RootNodeOnlyRule("$EXPERIMENTAL:$RULE_B"),
                        RootNodeOnlyRule("$EXPERIMENTAL:$RULE_A"),
                        RootNodeOnlyRule("$CUSTOM_RULE_SET_A:$RULE_B"),
                        RootNodeOnlyRule("$CUSTOM_RULE_SET_A:$RULE_A"),
                        RootNodeOnlyRule(RULE_B),
                        RootNodeOnlyRule(RULE_A),
                        RootNodeOnlyRule("$CUSTOM_RULE_SET_B:$RULE_B"),
                        RootNodeOnlyRule("$CUSTOM_RULE_SET_B:$RULE_A")
                    ),
                    debug = true
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
            "$CUSTOM_RULE_SET_B:$RULE_B"
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
                        NormalRule(RULE_B)
                    ),
                    debug = true
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            RULE_B,
            RULE_C,
            RULE_A
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
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_B:$RULE_A")
                    ),
                    debug = true
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
            "$CUSTOM_RULE_SET_B:$RULE_B"
        )
    }

    @Test
    fun `A run as late as possible rule on root node only runs after the rules not marked to run as late as possible`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        NormalRule(RULE_C),
                        RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_A),
                        NormalRule(RULE_B)
                    ),
                    debug = true
                )
                .map { it.ruleId }

        assertThat(actual).containsExactly(
            RULE_B,
            RULE_C,
            RULE_A
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
                        RunAsLateAsPossibleRule("$CUSTOM_RULE_SET_B:$RULE_A")
                    ),
                    debug = true
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
            "$CUSTOM_RULE_SET_B:$RULE_B"
        )
    }

    @ParameterizedTest(name = "Rule id: {0}, expected result: {1}")
    @ValueSource(
        strings = [
            RULE_A,
            "$CUSTOM_RULE_SET_A:$RULE_A"
        ]
    )
    fun `A rule annotated with run after rule can not refer to itself`(ruleId: String) {
        assertThatIllegalStateException().isThrownBy {
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            id = ruleId,
                            visitorModifier = VisitorModifier.RunAfterRule(ruleId)
                        ) {}
                    ),
                    debug = true
                )
        }.withMessage(
            "Rule with id '$ruleId' has a visitor modifier of type 'RunAfterRule' but it is not referring to another " +
                "rule but to the rule itself. A rule can not run after itself. This should be fixed by the maintainer " +
                "of the rule."
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the same rule set runs after that rule and override the alphabetical sort order`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            id = RULE_A,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_C)
                        ) {},
                        NormalRule(RULE_B),
                        object : R(
                            id = RULE_D,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                        ) {},
                        object : R(
                            id = RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                        ) {}
                    ),
                    debug = true
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_B",
            "$STANDARD:$RULE_C",
            "$STANDARD:$RULE_A",
            // Although RULE_D like RULE_C depends on RULE_B it still comes after RULE_A because that rules according to
            // the default sort order comes before rule D
            "$STANDARD:$RULE_D"
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
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                        ) {},
                        object : R(
                            id = RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                        ) {},
                        object : R(
                            id = "$EXPERIMENTAL:$RULE_A",
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_C)
                        ) {}
                    ),
                    debug = true
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_B",
            "$STANDARD:$RULE_C",
            "$STANDARD:$RULE_D",
            "$EXPERIMENTAL:$RULE_A"
        )
    }

    @Test
    fun `A rule annotated with run after rule which has to be loaded throws an exception in case that other rule is not loaded`() {
        assertThatIllegalStateException().isThrownBy {
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            id = RULE_A,
                            visitorModifier = VisitorModifier.RunAfterRule(
                                ruleId = "not-loaded-rule",
                                loadOnlyWhenOtherRuleIsLoaded = true
                            )
                        ) {}
                    ),
                    debug = true
                )
        }.withMessage("No runnable rules found. Please ensure that at least one is enabled.")
    }

    @Test
    fun `A rule annotated with run after rule of a rule which is not required to be loaded will be loaded when that other rule is not loaded`() {
        val actual =
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            id = RULE_A,
                            visitorModifier = VisitorModifier.RunAfterRule(
                                ruleId = "not-loaded-rule",
                                loadOnlyWhenOtherRuleIsLoaded = false
                            )
                        ) {}
                    ),
                    debug = true
                ).map { it.qualifiedRuleId }

        assertThat(actual).containsExactly(
            "$STANDARD:$RULE_A"
        )
    }

    @Test
    fun `Rules annotated with run after rule but cyclic depend on each others, no custom rule sets involved, throws an exception`() {
        assertThatIllegalStateException().isThrownBy {
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            id = RULE_A,
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                        ) {},
                        object : R(
                            id = RULE_B,
                            visitorModifier = VisitorModifier.RunAfterRule("$EXPERIMENTAL:$RULE_C")
                        ) {},
                        object : R(
                            id = "$EXPERIMENTAL:$RULE_C",
                            visitorModifier = VisitorModifier.RunAfterRule(RULE_A)
                        ) {}
                    ),
                    debug = true
                )
        }.withMessage(
            """
            Found cyclic dependencies between rules that should run after another rule:
              - Rule with id '$STANDARD:$RULE_A' should run after rule with id '$STANDARD:$RULE_B'
              - Rule with id '$STANDARD:$RULE_B' should run after rule with id '$EXPERIMENTAL:$RULE_C'
              - Rule with id '$EXPERIMENTAL:$RULE_C' should run after rule with id '$STANDARD:$RULE_A'
            """.trimIndent()
        )
    }

    @Test
    fun `Rules annotated with run after rule but cyclic depend on each others, custom rule sets involved, throws an exception`() {
        assertThatIllegalStateException().isThrownBy {
            RuleRunnerSorter()
                .getSortedRuleRunners(
                    ruleRunners = createRuleRunners(
                        object : R(
                            id = RULE_C,
                            visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_B:$RULE_B")
                        ) {},
                        object : R(
                            id = "$CUSTOM_RULE_SET_B:$RULE_B",
                            visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_A")
                        ) {},
                        object : R(
                            id = "$CUSTOM_RULE_SET_A:$RULE_A",
                            visitorModifier = VisitorModifier.RunAfterRule("$STANDARD:$RULE_C")
                        ) {}
                    ),
                    debug = true
                )
        }.withMessage(
            """
            Found cyclic dependencies between rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
              - Rule with id '$STANDARD:$RULE_C' should run after rule with id '$CUSTOM_RULE_SET_B:$RULE_B'
              - Rule with id '$CUSTOM_RULE_SET_A:$RULE_A' should run after rule with id '$STANDARD:$RULE_C'
              - Rule with id '$CUSTOM_RULE_SET_B:$RULE_B' should run after rule with id '$CUSTOM_RULE_SET_A:$RULE_A'
            """.trimIndent()
        )
    }

    private fun createRuleRunners(vararg rules: Rule): Set<RuleRunner> =
        rules
            .map {
                RuleRunner(
                    RuleProvider { it }
                )
            }.toSet()

    private companion object {
        const val STANDARD = "standard"
        const val EXPERIMENTAL = "experimental"
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val CUSTOM_RULE_SET_B = "custom-rule-set-b"
        const val NORMAL_RULE = "normal-rule"
        const val ROOT_NODE_ONLY_RULE = "root-node-only-rule"
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val RULE_D = "rule-d"
    }

    open class NormalRule(id: String) : R(id)

    class RootNodeOnlyRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunOnRootNodeOnly
        )
    )

    class RunAsLateAsPossibleRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible
        )
    )

    class RunAsLateAsPossibleOnRootNodeOnlyRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunOnRootNodeOnly,
            VisitorModifier.RunAsLateAsPossible
        )
    )

    open class R(
        id: String,
        visitorModifiers: Set<VisitorModifier> = emptySet()
    ) : Rule(id, visitorModifiers) {
        constructor(id: String, visitorModifier: VisitorModifier) : this(id, setOf(visitorModifier))

        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) {
            throw UnsupportedOperationException(
                "Rule should never be really invoked because that is not the aim of this unit test."
            )
        }
    }
}
