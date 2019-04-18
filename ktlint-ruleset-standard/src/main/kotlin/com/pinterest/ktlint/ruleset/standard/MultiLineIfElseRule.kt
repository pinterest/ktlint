package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.THEN
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeElement

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
        if (node.elementType == THEN || node.elementType == ELSE) {
            if (!node.treePrev.textContains('\n')) { // if (...) <statement>
                return
            }
            if (node.firstChildNode?.firstChildNode?.elementType != LBRACE) {
                emit(node.firstChildNode.startOffset, "Missing { ... }", true)
                if (autoCorrect) {
                    (node.firstChildNode.firstChildNode as TreeElement).rawInsertBeforeMe(LeafPsiElement(RBRACE, "{"))
                    (node.lastChildNode.lastChildNode as TreeElement).rawInsertAfterMe(LeafPsiElement(LBRACE, "}"))
                }
            }
        }
    }
}
