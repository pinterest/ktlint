package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.RuleId
import com.pinterest.ktlint.ruleset.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.ruleset.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.ruleset.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VisitorProviderTest {
    @Nested
    inner class `Given a visitor provider and some rules disabled via the 'ktlint_rule-id' properties` {
        @Test
        fun `A run-as-late-as-possible-rule runs later than normal rules`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { RunAsLateAsPossibleRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_A),
                Visit(STANDARD_RULE_C),
                Visit(STANDARD_RULE_B),
            )
        }

        @Test
        fun `Given that standard rule set is enabled explicitly then run all standard rules except those that are disabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { NormalRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_A),
                Visit(STANDARD_RULE_B),
            )
        }

        @Test
        fun `Given that standard rule set is not enabled explicitly then run all standard rules except experimental and explicitly disabled rules`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                RuleProvider { NormalRule(STANDARD_RULE_D) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_A),
                Visit(STANDARD_RULE_D),
            )
        }

        @Test
        fun `Given that standard rule set is disabled explicitly then only run standard rules that are enabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { NormalRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_A, RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.enabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_A),
                Visit(STANDARD_RULE_B),
            )
        }

        @Test
        fun `Given that the experimental rules are not disabled explicitly then only run rules that are enabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_C) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_B) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_C) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty(CUSTOM_RULE_B, RuleExecution.enabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_B),
                Visit(CUSTOM_RULE_B),
            )
        }

        @Test
        fun `Given that a experimental rules are disabled explicitly then only run rules that are enabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_C) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_B) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_C) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_experimental", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty(CUSTOM_RULE_B, RuleExecution.enabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_B),
                Visit(CUSTOM_RULE_B),
            )
        }

        @Test
        fun `Given that the experimental rules are enabled then only run rules that are not disabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_C) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_B) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_C) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_experimental", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty(CUSTOM_RULE_C, RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(STANDARD_RULE_B),
                Visit(CUSTOM_RULE_B),
            )
        }

        @Test
        fun `When no enabled rules are found for the root node, the visit function on the root node is not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { NormalRule(STANDARD_RULE_B) },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_A, RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.disabled),
                ),
            )

            assertThat(actual).isEmpty()
        }

        @Test
        fun `When no runnable rules are found for the root node, the visit function on the root node is not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                RuleProvider {
                    object : R(
                        ruleId = STANDARD_RULE_A,
                        visitorModifiers = setOf(
                            VisitorModifier.RunAfterRule(
                                ruleId = STANDARD_RULE_C,
                                runOnlyWhenOtherRuleIsEnabled = true,
                            )
                        ),
                    ) {}
                },
                RuleProvider {
                    object : R(
                        ruleId = STANDARD_RULE_B,
                        visitorModifiers = setOf(
                            VisitorModifier.RunAfterRule(
                                ruleId = STANDARD_RULE_C,
                                runOnlyWhenOtherRuleIsEnabled = true,
                            )
                        ),
                    ) {}
                },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                ),
            )

            assertThat(actual).isEmpty()
        }

        /**
         * Create a visitor provider. It returns a list of visits that the provider made after it was invoked. The tests
         * of the visitor provider should only focus on whether the visit provider has invoked the correct rules in the
         * correct order. Note that the testProvider does not invoke the real visit method of the rule.
         */
        private fun testVisitorProvider(
            vararg ruleProviders: RuleProvider,
            editorConfigProperties: Map<String, Property> = emptyMap(),
        ): List<Visit> =
            VisitorProvider(
                // Creates a new VisitorProviderFactory for each unit test to prevent that tests for the exact same set of
                // ruleIds are influencing each other.
                ruleProviders
                    .map { RuleRunner(it) }
                    .distinctBy { it.ruleId }
                    .toSet(),
                recreateRuleSorter = true,
            ).run {
                val visits = mutableListOf<Visit>()
                visitor(
                    editorConfigProperties,
                ).invoke { rule, _ ->
                    visits.add(Visit(rule.ruleId))
                }
                visits
            }

        private fun ktLintRuleExecutionEditorConfigProperty(
            ktlintRuleExecutionPropertyName: String,
            ruleExecution: RuleExecution,
        ): Pair<String, Property> =
            ktlintRuleExecutionPropertyName
                .let { propertyName ->
                    propertyName to Property.builder()
                        .name(propertyName)
                        .type(RULE_EXECUTION_PROPERTY_TYPE)
                        .value(ruleExecution.name)
                        .build()
                }

        private fun ktLintRuleExecutionEditorConfigProperty(
            ruleId: RuleId,
            ruleExecution: RuleExecution,
        ): Pair<String, Property> =
            ruleId
                .createRuleExecutionEditorConfigProperty()
                .let { property ->
                    property.name to Property.builder()
                        .name(property.name)
                        .type(RULE_EXECUTION_PROPERTY_TYPE)
                        .value(ruleExecution.name)
                        .build()
                }
    }

    private companion object {
        const val STANDARD = "standard"
        const val CUSTOM = "custom"
        val STANDARD_RULE_A = RuleId("$STANDARD:rule-a")
        val STANDARD_RULE_B = RuleId("$STANDARD:rule-b")
        val STANDARD_RULE_C = RuleId("$STANDARD:rule-c")
        val STANDARD_RULE_D = RuleId("$STANDARD:rule-d")
        val CUSTOM_RULE_B = RuleId("$CUSTOM:rule-b")
        val CUSTOM_RULE_C = RuleId("$CUSTOM:rule-c")
    }

    private open class NormalRule(ruleId: RuleId) : R(ruleId)

    private open class ExperimentalRule(ruleId: RuleId) : R(ruleId), Rule.Experimental

    private class RunAsLateAsPossibleRule(ruleId: RuleId) : R(
        ruleId = ruleId,
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible,
        ),
    )

    private open class R(
        ruleId: RuleId,
        visitorModifiers: Set<VisitorModifier> = emptySet(),
    ) : Rule(ruleId, visitorModifiers) {
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

    private data class Visit(val ruleId: RuleId)
}
