package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.disabledRulesProperty
import com.pinterest.ktlint.core.internal.VisitorProvider
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test

class VisitorProviderTest {
    @Test
    fun `A root only rule only visits the FILE node only`() {
        val actual = testVisitorProvider(
            RuleProvider { RootNodeOnlyRule(ROOT_NODE_ONLY_RULE) }
        )

        assertThat(actual).containsExactly(
            Visit(ROOT_NODE_ONLY_RULE)
        )
    }

    @Test
    fun `A run-as-late-as-possible-rule runs later than normal rules`() {
        val actual = testVisitorProvider(
            RuleProvider { NormalRule(RULE_A) },
            RuleProvider { RunAsLateAsPossibleRule(RULE_B) },
            RuleProvider { NormalRule(RULE_C) }
        )

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(RULE_C),
            Visit(RULE_B)
        )
    }

    @Test
    fun `A run as late as possible on root node only rule visits the root node only`() {
        val actual = testVisitorProvider(
            RuleProvider { RunAsLateAsPossibleOnRootNodeOnlyRule(RUN_AS_LATE_AS_POSSIBLE_RULE) }
        )

        assertThat(actual).containsExactly(
            Visit(RUN_AS_LATE_AS_POSSIBLE_RULE)
        )
    }

    @Test
    fun `Disabled rules in any type of rule set are not executed`() {
        val actual = testVisitorProvider(
            RuleProvider { NormalRule(RULE_A) },
            RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) },
            RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_B") },
            RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET) },
            RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_C") },
            RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A) }
        )

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(EXPERIMENTAL, RULE_B),
            Visit(CUSTOM_RULE_SET_A, RULE_C)
        )
    }

    @Test
    fun `When no enabled rules are found for the root node, the visit function on the root node is not executed`() {
        val actual = testVisitorProvider(
            RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) }
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `When no runnable rules are found for the root node, the visit function on the root node is not executed`() {
        val actual = testVisitorProvider(
            RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) },
            RuleProvider {
                object : R(
                    id = RULE_A,
                    visitorModifier = VisitorModifier.RunAfterRule(
                        ruleId = SOME_DISABLED_RULE_IN_STANDARD_RULE_SET,
                        runOnlyWhenOtherRuleIsEnabled = true
                    )
                ) {}
            }
        )

        assertThat(actual).isNull()
    }

    /**
     * Create a visitor provider. It returns a list of visits that the provider made after it was invoked. The tests of
     * the visitor provider should only focus on whether the visit provider has invoked the correct rules in the correct
     * order. Note that the testProvider does not invoke the real visit method of the rule.
     */
    private fun testVisitorProvider(vararg ruleProviders: RuleProvider): MutableList<Visit>? {
        return VisitorProvider(
            params = KtLint.ExperimentalParams(
                text = "",
                cb = { _, _ -> },
                ruleProviders = ruleProviders.toSet(),
                // Enable debug mode as it is helpful when a test fails
                debug = true
            ),
            // Creates a new VisitorProviderFactory for each unit test to prevent that tests for the exact same set of
            // ruleIds are influencing each other.
            recreateRuleSorter = true
        ).run {
            var visits: MutableList<Visit>? = null
            visitor(
                disabledRulesEditorConfigProperties(
                    SOME_DISABLED_RULE_IN_STANDARD_RULE_SET,
                    SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET,
                    SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A
                )
            ).invoke { _, fqRuleId ->
                if (visits == null) {
                    visits = mutableListOf()
                }
                visits?.add(Visit(fqRuleId))
            }
            visits
        }
    }

    private fun disabledRulesEditorConfigProperties(vararg ruleIds: String) =
        with(disabledRulesProperty) {
            mapOf(
                type.name to
                    Property.builder()
                        .name(type.name)
                        .type(type)
                        .value(ruleIds.joinToString(separator = ","))
                        .build()
            )
        }

    private companion object {
        const val STANDARD = "standard"
        const val EXPERIMENTAL = "experimental"
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val ROOT_NODE_ONLY_RULE = "root-node-only-rule"
        const val RUN_AS_LATE_AS_POSSIBLE_RULE = "run-as-late-as-possible-rule"
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val SOME_DISABLED_RULE_IN_STANDARD_RULE_SET = "some-disabled-rule-in-standard-rule-set"
        const val SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET = "$EXPERIMENTAL:some-disabled-rule-in-experimental-rule-set"
        const val SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A = "$CUSTOM_RULE_SET_A:some-disabled-rule-in-custom-rule-set"
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

    private data class Visit(val shortenedQualifiedRuleId: String) {
        constructor(
            ruleSetId: String,
            ruleId: String
        ) : this(
            shortenedQualifiedRuleId = "$ruleSetId:$ruleId"
        ) {
            require(!ruleSetId.contains(':')) {
                "rule set id may not contain the ':' character"
            }
            require(!ruleId.contains(':')) {
                "rule id may not contain the ':' character"
            }
        }

        init {
            require(!shortenedQualifiedRuleId.startsWith(STANDARD)) {
                "the shortened qualified rule id may not start with '$STANDARD'"
            }
        }
    }
}
