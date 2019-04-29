package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Issue
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.EXCL
import com.pinterest.ktlint.core.ast.ElementType.EXCLEXCL
import com.pinterest.ktlint.core.ast.ElementType.MINUS
import com.pinterest.ktlint.core.ast.ElementType.MINUSMINUS
import com.pinterest.ktlint.core.ast.ElementType.PLUS
import com.pinterest.ktlint.core.ast.ElementType.PLUSPLUS
import com.pinterest.ktlint.core.ast.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Ensures there are no spaces around unary operators
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/reference/coding-conventions.html#horizontal-whitespace)
 */
class SpacingAroundUnaryOperatorsRule : Rule("unary-op-spacing") {

    private val tokenSet = TokenSet.create(PLUS, PLUSPLUS, MINUS, MINUSMINUS, EXCL, EXCLEXCL)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (Issue) -> Unit
    ) {
        if (node is LeafElement && tokenSet.contains(node.elementType)) {
            val unaryExpressionNode = node.parent({ it.elementType in listOf(POSTFIX_EXPRESSION, PREFIX_EXPRESSION) })
            when (unaryExpressionNode?.elementType) {
                PREFIX_EXPRESSION -> {
                    val nextLeaf = node.nextLeaf()
                    if (nextLeaf is PsiWhiteSpace) {
                        emit(Issue(node.startOffset + 1, "Unexpected spacing after \"${node.text}\"", true))
                        if (autoCorrect) {
                            nextLeaf.treeParent.removeChild(nextLeaf)
                        }
                    }
                }
                POSTFIX_EXPRESSION -> {
                    val prevLeaf = node.prevLeaf()
                    if (prevLeaf is PsiWhiteSpace) {
                        emit(Issue(node.startOffset - 1, "Unexpected spacing before \"${node.text}\"", true))
                        if (autoCorrect) {
                            prevLeaf.treeParent.removeChild(prevLeaf)
                        }
                    }
                }
            }
        }
    }
}
