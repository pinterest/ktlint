package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.KtTokens

class IfThenBlockWithCurlyBraceRule : Rule("if-then-without-curly-brace-rule") {
    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {


        if (node.elementType == KtNodeTypes.THEN) {
            assert(node.treeParent.elementType == KtNodeTypes.IF)
            // Check Whether Block Element start with `{`
            if (node.firstChildNode.firstChildNode.elementType != KtTokens.LBRACE) {
                emit(node.firstChildNode.startOffset, "if-then block should start with `{`", true)
                if (autoCorrect) {
                    (node.firstChildNode.firstChildNode as TreeElement).rawInsertBeforeMe(LeafPsiElement(KtTokens.RBRACE, "{"))
                }
            }
            // Check Whether Block Element end with `}`
            if (node.lastChildNode.lastChildNode.elementType != KtTokens.RBRACE) {
                emit(node.lastChildNode.lastChildNode.startOffset, "if-then block should end with `}`", true)
                if (autoCorrect) {
                    (node.lastChildNode.lastChildNode as TreeElement).rawInsertAfterMe(LeafPsiElement(KtTokens.LBRACE, "}"))
                }
            }
        }

    }

}
