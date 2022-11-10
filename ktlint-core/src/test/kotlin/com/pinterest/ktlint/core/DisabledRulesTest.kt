package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.KTLINT_DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.DeprecatedEditorConfigPropertyException
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.ast.ElementType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DisabledRulesTest {
    @Test
    fun `Given some code and a enabled standard rule resulting in a violation then the violation is reported`() {
        assertThat(
            ArrayList<LintError>().apply {
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        text = "var foo",
                        ruleProviders = setOf(
                            RuleProvider { NoVarRule("no-var") },
                        ),
                        cb = { e, _ -> add(e) },
                    ),
                )
            },
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-var", "Unexpected var, use val instead"),
            ),
        )
    }

    @Nested
    @Deprecated("To be removed when deprecated disabledRulesProperty is removed")
    inner class DisabledRulesProperty {
        @Test
        fun `Given some code and a disabled standard rule then no violation is reported`() {
            assertThatThrownBy {
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        text = "var foo",
                        ruleProviders = setOf(
                            RuleProvider { NoVarRule("no-var") },
                        ),
                        cb = { _, _ -> },
                        editorConfigOverride = EditorConfigOverride.from(DISABLED_RULES_PROPERTY to "no-var"),
                    ),
                )
            }.isInstanceOf(DeprecatedEditorConfigPropertyException::class.java)
                .hasMessage("Property 'disabled_rules' is disallowed: Rename property 'disabled_rules' to 'ktlint_disabled_rules' in all '.editorconfig' files.")
        }
    }

    @Nested
    inner class KtlintDisabledRulesProperty {
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
        fun `Given some code and a disabled standard rule then no violation is reported`(
            ruleId: String,
            disabledRuleId: String,
        ) {
            assertThat(
                ArrayList<LintError>().apply {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            text = "var foo",
                            ruleProviders = setOf(
                                RuleProvider { NoVarRule(ruleId) },
                            ),
                            cb = { e, _ -> add(e) },
                            editorConfigOverride = EditorConfigOverride.from(KTLINT_DISABLED_RULES_PROPERTY to disabledRuleId),
                        ),
                    )
                },
            ).isEmpty()
        }

        @Test
        fun `Given some code and a disabled standard rule then no violation is reported`() {
            assertThat(
                ArrayList<LintError>().apply {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            text = "var foo",
                            ruleProviders = setOf(
                                RuleProvider { NoVarRule("no-var") },
                            ),
                            cb = { e, _ -> add(e) },
                            editorConfigOverride = EditorConfigOverride.from(KTLINT_DISABLED_RULES_PROPERTY to "no-var"),
                        ),
                    )
                },
            ).isEmpty()
        }

        @Test
        fun `Given some code and a disabled experimental rule then no violation is reported`() {
            assertThat(
                ArrayList<LintError>().apply {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            text = "var foo",
                            ruleProviders = setOf(
                                RuleProvider { NoVarRule("experimental:no-var") },
                            ),
                            cb = { e, _ -> add(e) },
                            editorConfigOverride = EditorConfigOverride.from(KTLINT_DISABLED_RULES_PROPERTY to "experimental:no-var"),
                        ),
                    )
                },
            ).isEmpty()
        }
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
