package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.editorconfig.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.core.ast.ElementType
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DisabledRulesTest {
    @Test
    fun `Given some code and a enabled standard rule resulting in a violation then the violation is reported`() {
        assertThat(
            ArrayList<LintError>().apply {
                KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider { NoVarRule("no-var") },
                    ),
                ).lint("var foo") { e -> add(e) }
            },
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-var", "Unexpected var, use val instead"),
            ),
        )
    }

    @Deprecated("To be removed when deprecated property 'disabled_rules` is removed")
    @ParameterizedTest(name = "RuleId: {0}, Disabled ruleId: {1}")
    @CsvSource(
        value = [
            "no-var,no-var",
            "no-var,standard:no-var",
            "standard:no-var,no-var",
            "standard:no-var,standard:no-var",
            "experimental:no-var,experimental:no-var",
            "custom:no-var,custom:no-var",
        ],
    )
    fun `Given a rule that is disabled via property 'disabled_rules' and some code then no violation is reported`(
        ruleId: String,
        disabledRuleId: String,
    ) {
        assertThat(
            ArrayList<LintError>().apply {
                KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider { NoVarRule(ruleId) },
                    ),
                    editorConfigOverride = EditorConfigOverride.from(DISABLED_RULES_PROPERTY to disabledRuleId),
                ).lint("var foo") { e -> add(e) }
            },
        ).isEmpty()
    }

    @Deprecated("To be removed when deprecated property 'ktlint_disabled_rules` is removed")
    @ParameterizedTest(name = "RuleId: {0}, Disabled ruleId: {1}")
    @CsvSource(
        value = [
            "no-var,no-var",
            "no-var,standard:no-var",
            "standard:no-var,no-var",
            "standard:no-var,standard:no-var",
            "experimental:no-var,experimental:no-var",
            "custom:no-var,custom:no-var",
        ],
    )
    fun `Given a rule that is disabled via property 'ktlint_disabled_rules' and some code then no violation is reported`(
        ruleId: String,
        disabledRuleId: String,
    ) {
        assertThat(
            ArrayList<LintError>().apply {
                KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider { NoVarRule(ruleId) },
                    ),
                    editorConfigOverride = EditorConfigOverride.from(KTLINT_DISABLED_RULES_PROPERTY to disabledRuleId),
                ).lint("var foo") { e -> add(e) }
            },
        ).isEmpty()
    }

    @ParameterizedTest(name = "RuleId: {0}, Disabled ruleId: {1}")
    @CsvSource(
        value = [
            "no-var,no-var",
            "no-var,standard:no-var",
            "standard:no-var,no-var",
            "standard:no-var,standard:no-var",
            "experimental:no-var,experimental:no-var",
            "custom:no-var,custom:no-var",
        ],
    )
    fun `Given a rule that is disabled via property 'ktlint_some-rule-id' and some code then no violation is reported`(
        ruleId: String,
        disabledRuleId: String,
    ) {
        assertThat(
            ArrayList<LintError>().apply {
                KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider { NoVarRule(ruleId) },
                    ),
                    editorConfigOverride = EditorConfigOverride.from(
                        createRuleExecutionEditorConfigProperty(disabledRuleId) to RuleExecution.disabled,
                    ),
                ).lint("var foo") { e -> add(e) }
            },
        ).isEmpty()
    }

    class NoVarRule(id: String) : Rule(id) {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ) {
            if (node.elementType == ElementType.VAR_KEYWORD) {
                emit(node.startOffset, "Unexpected var, use val instead", false)
            }
        }
    }
}
