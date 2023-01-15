package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.editorconfig.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.core.internal.RuleRunner
import com.pinterest.ktlint.core.internal.VisitorProvider
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VisitorProviderTest {
    @Nested
    inner class `Given a visitor provider and some rules disabled via the 'disabled_rules' property` {
        @Test
        fun `A run-as-late-as-possible-rule runs later than normal rules`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { RunAsLateAsPossibleRule(RULE_B) },
                RuleProvider { NormalRule(RULE_C) },
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(RULE_C),
                Visit(RULE_B),
            )
        }

        @Test
        fun `Disabled rules in any type of rule set and experimental rules are not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) },
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_B") },
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET) },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_C") },
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A) },
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(CUSTOM_RULE_SET_A, RULE_C),
            )
        }

        @Test
        fun `When no enabled rules are found for the root node, the visit function on the root node is not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) },
            )

            assertThat(actual).isEmpty()
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
                            runOnlyWhenOtherRuleIsEnabled = true,
                        ),
                    ) {}
                },
            )

            assertThat(actual).isEmpty()
        }

        /**
         * Create a visitor provider. It returns a list of visits that the provider made after it was invoked. The tests
         * of the visitor provider should only focus on whether the visit provider has invoked the correct rules in the
         * correct order. Note that the testProvider does not invoke the real visit method of the rule.
         */
        private fun testVisitorProvider(vararg ruleProviders: RuleProvider): List<Visit> {
            return VisitorProvider(
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
                    ktlintDisabledRulesEditorConfigProperties(
                        SOME_DISABLED_RULE_IN_STANDARD_RULE_SET,
                        SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET,
                        SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A,
                    ),
                ).invoke { _, fqRuleId ->
                    visits.add(Visit(fqRuleId))
                }
                visits
            }
        }

        private fun ktlintDisabledRulesEditorConfigProperties(vararg ruleIds: String) =
            with(DISABLED_RULES_PROPERTY) {
                mapOf(
                    name to
                        Property.builder()
                            .name(name)
                            .type(type)
                            .value(ruleIds.joinToString(separator = ","))
                            .build(),
                )
            }
    }

    @Nested
    inner class `Given a visitor provider and some rules disabled via the 'ktlint_disabled_rules' property` {
        @Test
        fun `A run-as-late-as-possible-rule runs later than normal rules`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { RunAsLateAsPossibleRule(RULE_B) },
                RuleProvider { NormalRule(RULE_C) },
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(RULE_C),
                Visit(RULE_B),
            )
        }

        @Test
        fun `Disabled rules in any type of rule set and experimental rules are not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) },
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_B") },
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET) },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_C") },
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A) },
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(CUSTOM_RULE_SET_A, RULE_C),
            )
        }

        @Test
        fun `When no enabled rules are found for the root node, the visit function on the root node is not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET) },
            )

            assertThat(actual).isEmpty()
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
                            runOnlyWhenOtherRuleIsEnabled = true,
                        ),
                    ) {}
                },
            )

            assertThat(actual).isEmpty()
        }

        /**
         * Create a visitor provider. It returns a list of visits that the provider made after it was invoked. The tests
         * of the visitor provider should only focus on whether the visit provider has invoked the correct rules in the
         * correct order. Note that the testProvider does not invoke the real visit method of the rule.
         */
        private fun testVisitorProvider(vararg ruleProviders: RuleProvider): List<Visit> {
            return VisitorProvider(
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
                    ktlintDisabledRulesEditorConfigProperties(
                        SOME_DISABLED_RULE_IN_STANDARD_RULE_SET,
                        SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET,
                        SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A,
                    ),
                ).invoke { _, fqRuleId ->
                    visits.add(Visit(fqRuleId))
                }
                visits
            }
        }

        private fun ktlintDisabledRulesEditorConfigProperties(vararg ruleIds: String) =
            with(KTLINT_DISABLED_RULES_PROPERTY) {
                mapOf(
                    name to
                        Property.builder()
                            .name(name)
                            .type(type)
                            .value(ruleIds.joinToString(separator = ","))
                            .build(),
                )
            }
    }

    @Nested
    inner class `Given a visitor provider and some rules disabled via the 'ktlint_rule-id' properties` {
        @Test
        fun `A run-as-late-as-possible-rule runs later than normal rules`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { RunAsLateAsPossibleRule(RULE_B) },
                RuleProvider { NormalRule(RULE_C) },
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(RULE_C),
                Visit(RULE_B),
            )
        }

        @Test
        fun `Given that standard rule set is enabled explicitly then run all standard rules except those that are disabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { NormalRule("$STANDARD:$RULE_B") },
                RuleProvider { NormalRule("$STANDARD:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD:$RULE_C", RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(RULE_B),
            )
        }

        @Test
        fun `Given that standard rule set is not enabled explicitly then run all standard rules except those that are disabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { NormalRule("$STANDARD:$RULE_B") },
                RuleProvider { NormalRule("$STANDARD:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD:$RULE_C", RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(RULE_B),
            )
        }

        @Test
        fun `Given that standard rule set is disabled explicitly then only run standard rules that are enabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { NormalRule("$STANDARD:$RULE_B") },
                RuleProvider { NormalRule("$STANDARD:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD:$RULE_A", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD:$RULE_B", RuleExecution.enabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(RULE_A),
                Visit(RULE_B),
            )
        }

        @Test
        fun `Given that the experimental rule set is not enabled explicitly then only run experimental rules that are enabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_B") },
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$EXPERIMENTAL:$RULE_B", RuleExecution.enabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(EXPERIMENTAL, RULE_B),
            )
        }

        @Test
        fun `Given that the custom rule set is not enabled explicitly then run all rules that are not disabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_B") },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM_RULE_SET_A:$RULE_C", RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(CUSTOM_RULE_SET_A, RULE_B),
            )
        }

        @Test
        fun `Given that a non-standard rule set is disabled explicitly then only run rules that are enabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_B") },
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_C") },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_B") },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$EXPERIMENTAL", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$EXPERIMENTAL:$RULE_B", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM_RULE_SET_A", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM_RULE_SET_A:$RULE_B", RuleExecution.enabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(EXPERIMENTAL, RULE_B),
                Visit(CUSTOM_RULE_SET_A, RULE_B),
            )
        }

        @Test
        fun `Given that a non-standard rule set is enabled explicitly then only run rules that are not disabled explicitly`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_B") },
                RuleProvider { NormalRule("$EXPERIMENTAL:$RULE_C") },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_B") },
                RuleProvider { NormalRule("$CUSTOM_RULE_SET_A:$RULE_C") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$EXPERIMENTAL", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$EXPERIMENTAL:$RULE_C", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM_RULE_SET_A", RuleExecution.enabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM_RULE_SET_A:$RULE_C", RuleExecution.disabled),
                ),
            )

            assertThat(actual).containsExactly(
                Visit(EXPERIMENTAL, RULE_B),
                Visit(CUSTOM_RULE_SET_A, RULE_B),
            )
        }

        @Test
        fun `When no enabled rules are found for the root node, the visit function on the root node is not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_A) },
                RuleProvider { NormalRule("$STANDARD:$RULE_B") },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_${STANDARD}_$RULE_A", RuleExecution.disabled),
                    ktLintRuleExecutionEditorConfigProperty("ktlint_${STANDARD}_$RULE_B", RuleExecution.disabled),
                ),
            )

            assertThat(actual).isEmpty()
        }

        @Test
        fun `When no runnable rules are found for the root node, the visit function on the root node is not executed`() {
            val actual = testVisitorProvider(
                RuleProvider { NormalRule(RULE_C) },
                RuleProvider {
                    object : R(
                        id = RULE_A,
                        visitorModifier = VisitorModifier.RunAfterRule(
                            ruleId = RULE_C,
                            runOnlyWhenOtherRuleIsEnabled = true,
                        ),
                    ) {}
                },
                RuleProvider {
                    object : R(
                        id = RULE_B,
                        visitorModifier = VisitorModifier.RunAfterRule(
                            ruleId = RULE_C,
                            runOnlyWhenOtherRuleIsEnabled = true,
                        ),
                    ) {}
                },
                editorConfigProperties = mapOf(
                    ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD:$RULE_C", RuleExecution.disabled),
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
        ): List<Visit> = VisitorProvider(
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
            ).invoke { _, fqRuleId ->
                visits.add(Visit(fqRuleId))
            }
            visits
        }

        private fun ktLintRuleExecutionEditorConfigProperty(
            ktlintRuleExecutionPropertyName: String,
            ruleExecution: RuleExecution,
        ): Pair<String, Property> =
            ktlintRuleExecutionPropertyName
                .replace(":", "_")
                .let { propertyName ->
                    propertyName to Property.builder()
                        .name(propertyName)
                        .type(RULE_EXECUTION_PROPERTY_TYPE)
                        .value(ruleExecution.name)
                        .build()
                }
    }

    private companion object {
        const val STANDARD = "standard"
        const val EXPERIMENTAL = "experimental"
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val SOME_DISABLED_RULE_IN_STANDARD_RULE_SET = "some-disabled-rule-in-standard-rule-set"
        const val SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET = "$EXPERIMENTAL:some-disabled-rule-in-experimental-rule-set"
        const val SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A = "$CUSTOM_RULE_SET_A:some-disabled-rule-in-custom-rule-set"
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

    private data class Visit(val shortenedQualifiedRuleId: String) {
        constructor(
            ruleSetId: String,
            ruleId: String,
        ) : this(
            shortenedQualifiedRuleId = "$ruleSetId:$ruleId",
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
