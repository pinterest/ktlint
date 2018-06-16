package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.KtTokens

class IfElseMultiLineBlockWithCurlBraceRule : Rule("if-else-multiline-block-with-curly-brace-rule") {
    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {


        if (node.elementType == KtNodeTypes.THEN
            || node.elementType == KtNodeTypes.ELSE
        ) {
            assert(node.treeParent?.elementType == KtNodeTypes.IF)
            // skip one line if(true) <statement>
            if (!node.treePrev.textContains('\n')) {
                return
            }

            // Check Whether Block Element start with `{`
            if (node.firstChildNode?.firstChildNode?.elementType != KtTokens.LBRACE) {
                emit(node.firstChildNode.startOffset, "if-else block with multiline should start with `{`", true)
                if (autoCorrect) {
                    (node.firstChildNode.firstChildNode as TreeElement).rawInsertBeforeMe(LeafPsiElement(KtTokens.RBRACE, "{"))
                }
            }
            // Check Whether Block Element end with `}`
            if (node.lastChildNode?.lastChildNode?.elementType != KtTokens.RBRACE) {
                emit(node.lastChildNode.lastChildNode.startOffset, "if-else block with multiline should end with `}`", true)
                if (autoCorrect) {
                    (node.lastChildNode.lastChildNode as TreeElement).rawInsertAfterMe(LeafPsiElement(KtTokens.LBRACE, "}"))
                }
            }
        }
    }

}
