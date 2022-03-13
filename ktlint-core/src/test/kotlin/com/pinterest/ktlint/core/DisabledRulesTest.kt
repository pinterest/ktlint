package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.ElementType
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test

class DisabledRulesTest {
    @Test
    fun `Given some code and a enabled standard rule resulting in a violation then the violation is reported`() {
        assertThat(
            ArrayList<LintError>().apply {
                KtLint.lint(
                    KtLint.Params(
                        text = "var foo",
                        ruleSets = listOf(RuleSet("standard", NoVarRule())),
                        cb = { e, _ -> add(e) }
                    )
                )
            }
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-var", "Unexpected var, use val instead")
            )
        )
    }

    @Test
    fun `Given some code and a disabled standard rule then no violation is reported`() {
        assertThat(
            ArrayList<LintError>().apply {
                KtLint.lint(
                    KtLint.Params(
                        text = "var foo",
                        ruleSets = listOf(RuleSet("standard", NoVarRule())),
                        cb = { e, _ -> add(e) },
                        userData = mapOf(("disabled_rules" to "no-var"))
                    )
                )
            }
        ).isEmpty()
    }

    @Test
    fun `Given some code and a disabled experimental rule then no violation is reported`() {
        assertThat(
            ArrayList<LintError>().apply {
                KtLint.lint(
                    KtLint.Params(
                        text = "var foo",
                        ruleSets = listOf(RuleSet("experimental", NoVarRule())),
                        cb = { e, _ -> add(e) },
                        userData = mapOf(("disabled_rules" to "experimental:no-var"))
                    )
                )
            }
        ).isEmpty()
    }

    class NoVarRule : Rule("no-var") {
        override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) {
            if (node.elementType == ElementType.VAR_KEYWORD) {
                emit(node.startOffset, "Unexpected var, use val instead", false)
            }
        }
    }
}
