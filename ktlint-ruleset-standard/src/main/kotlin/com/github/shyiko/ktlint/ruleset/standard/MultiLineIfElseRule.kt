package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * TODO: if, for, when branch, do, while
 * @author yokotaso <yokotaso.t@gmail.com>
 */
class MultiLineIfElseRule : Rule("multiline-if-else") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtNodeTypes.THEN || node.elementType == KtNodeTypes.ELSE) {
            if (!node.treePrev.textContains('\n')) { // if (...) <statement>
                return
            }
            if (node.firstChildNode?.firstChildNode?.elementType != KtTokens.LBRACE) {
                emit(node.firstChildNode.startOffset, "Missing { ... }", true)
                if (autoCorrect) {
                    (node.firstChildNode.firstChildNode as TreeElement).rawInsertBeforeMe(LeafPsiElement(KtTokens.RBRACE, "{"))
                    (node.lastChildNode.lastChildNode as TreeElement).rawInsertAfterMe(LeafPsiElement(KtTokens.LBRACE, "}"))
                }
            }
        }
    }
}
