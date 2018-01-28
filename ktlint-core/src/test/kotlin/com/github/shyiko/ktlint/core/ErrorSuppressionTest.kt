package com.github.shyiko.ktlint.core

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtImportDirective
import org.testng.annotations.Test
import java.util.ArrayList

class ErrorSuppressionTest {

    @Test
    fun testErrorSuppression() {
        class NoWildcardImportsRule : Rule("no-wildcard-imports") {
            override fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String, corrected: Boolean) -> Unit
            ) {
                if (node is LeafPsiElement && node.textMatches("*") &&
                        PsiTreeUtil.getNonStrictParentOfType(node, KtImportDirective::class.java) != null) {
                    emit(node.startOffset, "Wildcard import", false)
                }
            }
        }
        fun lint(text: String) =
            ArrayList<LintError>().apply {
                KtLint.lint(text, listOf(RuleSet("standard", NoWildcardImportsRule()))) { e -> add(e) }
            }
        assertThat(lint(
            """
            import a.* // ktlint-disable
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 10, "no-wildcard-imports", "Wildcard import")
        ))
        assertThat(lint(
            """
            import a.* // ktlint-disable no-wildcard-imports
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 10, "no-wildcard-imports", "Wildcard import")
        ))
        assertThat(lint(
            """
            /* ktlint-disable */
            import a.*
            import a.*
            /* ktlint-enable */
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(5, 10, "no-wildcard-imports", "Wildcard import")
        ))
        assertThat(lint(
            """
            /* ktlint-disable no-wildcard-imports */
            import a.*
            import a.*
            /* ktlint-enable no-wildcard-imports */
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(5, 10, "no-wildcard-imports", "Wildcard import")
        ))
    }
}
