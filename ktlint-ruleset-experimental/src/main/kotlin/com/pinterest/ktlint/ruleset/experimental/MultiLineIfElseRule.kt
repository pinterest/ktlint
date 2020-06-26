package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression

/**
 * https://kotlinlang.org/docs/reference/coding-conventions.html#formatting-control-flow-statements
 *
 * TODO: if, for, when branch, do, while
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
        val bodyIndent = node.treePrev.text
        val rightBraceIndent = node.treeParent
            .prevLeaf { it is PsiWhiteSpace && it.textContains('\n') }?.text.orEmpty()
            .let { "\n${it.substringAfterLast("\n")}" }
        (node.treePrev as LeafPsiElement).rawReplaceWithText(" ")
        KtBlockExpression(null).apply {
            val previousChild = node.firstChildNode
            node.replaceChild(node.firstChildNode, this)
            addChild(LeafPsiElement(LBRACE, "{"))
            addChild(PsiWhiteSpaceImpl(bodyIndent))
            addChild(previousChild)
            addChild(PsiWhiteSpaceImpl(rightBraceIndent))
            addChild(LeafPsiElement(RBRACE, "}"))
        }

        // Make sure else starts on same line as newly inserted right brace
        if (node.elementType == THEN && node.treeNext?.treeNext?.elementType == ELSE_KEYWORD) {
            node.treeParent.replaceChild(node.treeNext, PsiWhiteSpaceImpl(" "))
        }
    }
}
