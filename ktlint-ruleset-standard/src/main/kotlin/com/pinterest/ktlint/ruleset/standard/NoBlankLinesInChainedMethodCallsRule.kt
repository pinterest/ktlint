package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NoBlankLinesInChainedMethodCallsRule : Rule("no-blank-lines-in-chained-method-calls") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val isBlankLine = node is PsiWhiteSpace && node.getText().contains("\n\n")
        if (isBlankLine && node.treeParent.elementType == DOT_QUALIFIED_EXPRESSION) {
            emit(node.startOffset + 1, "Needless blank line(s)", true)

            if (autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText("\n" + node.getText().split("\n\n")[1])
            }
        }
    }
}
