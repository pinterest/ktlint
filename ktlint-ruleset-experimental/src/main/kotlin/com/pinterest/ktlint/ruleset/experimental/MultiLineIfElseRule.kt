package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.THEN
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

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
                // Disabled until we autocorrect with proper formatting
                /*if (autoCorrect) {
                    (node.firstChildNode.firstChildNode as TreeElement).rawInsertBeforeMe(LeafPsiElement(RBRACE, "{"))
                    (node.lastChildNode.lastChildNode as TreeElement).rawInsertAfterMe(LeafPsiElement(LBRACE, "}"))
                }*/
            }
        }
    }
}
