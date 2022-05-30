package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * https://kotlinlang.org/docs/reference/coding-conventions.html#formatting-control-flow-statements
 */
class MultiLineIfElseRule : Rule("multiline-if-else") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == THEN || node.elementType == ELSE) {
            if (!node.treePrev.textContains('\n')) { // if (...) <statement>
                return
            }

            if (node.firstChildNode?.firstChildNode?.elementType != LBRACE) {
                emit(node.firstChildNode.startOffset, "Missing { ... }", true)
                if (autoCorrect) {
                    autocorrect(node)
                }
            }
        }
    }

    private fun autocorrect(node: ASTNode) {
        val prevLeaves =
            node.leaves(forward = false).takeWhile { it.elementType !in listOf(RPAR, ELSE_KEYWORD) }.toList().reversed()
        val nextLeaves =
            node.leaves(forward = true).takeWhile { it.isWhiteSpaceWithoutNewline() || it.isPartOfComment() }.toList()
        val rightBraceIndent = node.treeParent
            .prevLeaf { it is PsiWhiteSpace && it.textContains('\n') }?.text.orEmpty()
            .let { "\n${it.substringAfterLast("\n")}" }

        (node.treePrev as LeafPsiElement).rawReplaceWithText(" ")
        KtBlockExpression(null).apply {
            val previousChild = node.firstChildNode
            node.replaceChild(node.firstChildNode, this)
            addChild(LeafPsiElement(LBRACE, "{"))
            prevLeaves.forEach(::addChild)
            addChild(previousChild)
            nextLeaves.forEach(::addChild)
            addChild(PsiWhiteSpaceImpl(rightBraceIndent))
            addChild(LeafPsiElement(RBRACE, "}"))
        }

        // Make sure else starts on same line as newly inserted right brace
        if (node.elementType == THEN && node.treeNext?.treeNext?.elementType == ELSE_KEYWORD) {
            node.treeParent.replaceChild(node.treeNext, PsiWhiteSpaceImpl(" "))
        }
    }
}
