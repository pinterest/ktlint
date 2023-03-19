package com.pinterest.ktlint.rule.engine.internal.rulefilter

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.junit.jupiter.api.Test

class RuleExecutionRuleFilterTest {
    @Test
    fun `Given that standard rule set is enabled explicitly then run all standard rules except those that are disabled explicitly`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { NormalRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD", RuleExecution.enabled),
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                    ),
            ).toRuleIds()

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
        )
    }

    @Test
    fun `Given that standard rule set is not enabled explicitly then run all standard rules except experimental and explicitly disabled rules`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                RuleProvider { NormalRule(STANDARD_RULE_D) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                    ),
            ).toRuleIds()

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_D,
        )
    }

    @Test
    fun `Given that standard rule set is disabled explicitly then only run standard rules that are enabled explicitly`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { NormalRule(STANDARD_RULE_B) },
                RuleProvider { NormalRule(STANDARD_RULE_C) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty("ktlint_$STANDARD", RuleExecution.disabled),
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_A, RuleExecution.enabled),
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.enabled),
                    ),
            ).toRuleIds()

        assertThat(actual).containsExactly(
            STANDARD_RULE_A,
            STANDARD_RULE_B,
        )
    }

    @Test
    fun `Given that the experimental rules are not disabled explicitly then only run rules that are enabled explicitly`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_C) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_B) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_C) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.enabled),
                        ktLintRuleExecutionEditorConfigProperty(CUSTOM_RULE_B, RuleExecution.enabled),
                    ),
            ).toRuleIds()

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            CUSTOM_RULE_B,
        )
    }

    @Test
    fun `Given that a experimental rules are disabled explicitly then only run rules that are enabled explicitly`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_C) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_B) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_C) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty("ktlint_experimental", RuleExecution.disabled),
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.enabled),
                        ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM", RuleExecution.disabled),
                        ktLintRuleExecutionEditorConfigProperty(CUSTOM_RULE_B, RuleExecution.enabled),
                    ),
            ).toRuleIds()

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            CUSTOM_RULE_B,
        )
    }

    @Test
    fun `Given that the experimental rules are enabled then only run rules that are not disabled explicitly`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { ExperimentalRule(STANDARD_RULE_B) },
                RuleProvider { ExperimentalRule(STANDARD_RULE_C) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_B) },
                RuleProvider { ExperimentalRule(CUSTOM_RULE_C) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty("ktlint_experimental", RuleExecution.enabled),
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_C, RuleExecution.disabled),
                        ktLintRuleExecutionEditorConfigProperty("ktlint_$CUSTOM", RuleExecution.enabled),
                        ktLintRuleExecutionEditorConfigProperty(CUSTOM_RULE_C, RuleExecution.disabled),
                    ),
            ).toRuleIds()

        assertThat(actual).containsExactly(
            STANDARD_RULE_B,
            CUSTOM_RULE_B,
        )
    }

    @Test
    fun `When some standard rules which are all disabled explicitly then return empty`() {
        val actual =
            runWithRuleExecutionRuleFilter(
                RuleProvider { NormalRule(STANDARD_RULE_A) },
                RuleProvider { NormalRule(STANDARD_RULE_B) },
                editorConfig =
                    EditorConfig(
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_A, RuleExecution.disabled),
                        ktLintRuleExecutionEditorConfigProperty(STANDARD_RULE_B, RuleExecution.disabled),
                    ),
            ).toRuleIds()

        assertThat(actual).isEmpty()
    }

    /**
     * Create a [RuleExecutionRuleFilter] for a given set of [RuleProvider]s and an [EditorConfig].
     */
    private fun runWithRuleExecutionRuleFilter(
        vararg ruleProviders: RuleProvider,
        editorConfig: EditorConfig,
    ): Set<RuleProvider> =
        RuleExecutionRuleFilter(
            editorConfig = editorConfig.addPropertiesWithDefaultValueIfMissing(CODE_STYLE_PROPERTY),
        ).filter(
            ruleProviders.toSet(),
        )

    private fun Set<RuleProvider>.toRuleIds() = map { it.ruleId }

    private fun ktLintRuleExecutionEditorConfigProperty(
        ktlintRuleExecutionPropertyName: String,
        ruleExecution: RuleExecution,
    ) = Property
        .builder()
        .type(RULE_EXECUTION_PROPERTY_TYPE)
        .name(ktlintRuleExecutionPropertyName)
        .value(ruleExecution.name)
        .build()

    private fun ktLintRuleExecutionEditorConfigProperty(
        ruleId: RuleId,
        ruleExecution: RuleExecution,
    ) = ktLintRuleExecutionEditorConfigProperty(ruleId.createRuleExecutionEditorConfigProperty().name, ruleExecution)

    private companion object {
        val STANDARD = RuleSetId.STANDARD.value
        const val CUSTOM = "custom"
        val STANDARD_RULE_A = RuleId("$STANDARD:rule-a")
        val STANDARD_RULE_B = RuleId("$STANDARD:rule-b")
        val STANDARD_RULE_C = RuleId("$STANDARD:rule-c")
        val STANDARD_RULE_D = RuleId("$STANDARD:rule-d")
        val CUSTOM_RULE_B = RuleId("$CUSTOM:rule-b")
        val CUSTOM_RULE_C = RuleId("$CUSTOM:rule-c")
    }

    private open class NormalRule(ruleId: RuleId) :
        Rule(
            ruleId = ruleId,
            about = About(),
        )

    private open class ExperimentalRule(ruleId: RuleId) :
        Rule(
            ruleId = ruleId,
            about = About(),
        ),
        Rule.Experimental
}
