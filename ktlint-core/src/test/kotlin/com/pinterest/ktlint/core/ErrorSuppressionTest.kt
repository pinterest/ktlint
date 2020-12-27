package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.isPartOf
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.junit.Test

class ErrorSuppressionTest {

    class NoWildcardImportsRule : Rule("no-wildcard-imports") {
        override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, corrected: Boolean) -> Unit
        ) {
            if (node is LeafPsiElement && node.textMatches("*") && node.isPartOf(ElementType.IMPORT_DIRECTIVE)) {
                emit(node.startOffset, "Wildcard import", false)
            }
        }
    }

    fun lint(text: String) =
        ArrayList<LintError>().apply {
            KtLint.lint(
                KtLint.Params(
                    text = text,
                    ruleSets = listOf(RuleSet("standard", NoWildcardImportsRule())),
                    cb = { e, _ -> add(e) }
                )
            )
        }

    @Test
    fun testErrorSuppressionDisableAllOnPackage() {
        assertThat(
            lint(
                """
                package com.pinterest.ktlint // ktlint-disable

                import a.* // Should not trigger an error due to ktlin-disable directive on package

                /* ktlint-enable */
                import b.* // will trigger an error
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(6, 10, "no-wildcard-imports", "Wildcard import")
            )
        )
    }

    @Test
    fun testErrorSuppressionDisableRuleOnPackage() {
        assertThat(
            lint(
                """
                package com.pinterest.ktlint // ktlint-disable no-wildcard-imports

                import a.* // Should not trigger an error due to ktlin-disable directive on package

                /* ktlint-enable no-wildcard-imports */
                import b.* // will trigger an error
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(6, 10, "no-wildcard-imports", "Wildcard import")
            )
        )
    }

    @Test
    fun testErrorSuppressionDisableAllOnImport() {
        assertThat(
            lint(
                """
                import a.* // ktlint-disable
                import a.* // will trigger an error
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 10, "no-wildcard-imports", "Wildcard import")
            )
        )
    }

    @Test
    fun testErrorSuppressionDisableRuleOnImport() {
        assertThat(
            lint(
                """
                import a.* // ktlint-disable no-wildcard-imports
                import a.* // will trigger an error
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 10, "no-wildcard-imports", "Wildcard import")
            )
        )
    }

    @Test
    fun testErrorSuppressionDisableAllInBlock() {
        assertThat(
            lint(
                """
                /* ktlint-disable */
                import a.*
                import a.*
                /* ktlint-enable */
                import a.* // will trigger an error
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(5, 10, "no-wildcard-imports", "Wildcard import")
            )
        )
    }

    @Test
    fun testErrorSuppressionDisableRuleInBlock() {
        assertThat(
            lint(
                """
                /* ktlint-disable no-wildcard-imports */
                import a.*
                import a.*
                /* ktlint-enable no-wildcard-imports */
                import a.* // will trigger an error
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(5, 10, "no-wildcard-imports", "Wildcard import")
            )
        )
    }
}
