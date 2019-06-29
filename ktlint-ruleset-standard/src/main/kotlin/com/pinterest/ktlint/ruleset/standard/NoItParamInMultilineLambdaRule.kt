package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class NoItParamInMultilineLambdaRule : Rule("no-it-in-multiline-lambda") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == IDENTIFIER && node.text == "it") {
            val block = node.parent(BLOCK)
            if (block != null && block.textContains('\n') && !isValidItParam(node, block)) {
                emit(node.startOffset, "Multiline lambda must explicitly name \"it\" parameter", false)
            }
        }
    }

    private fun isValidItParam(node: ASTNode, block: ASTNode): Boolean {
        val containsItInProperty = block.findChildByType(PROPERTY)?.text?.split("=")?.first()?.split(" ")?.contains("it")
            ?: false
        val parentElement = (node as LeafPsiElement).parent
        val partOfDotQualifiedExpression = parentElement is KtNameReferenceExpression &&
            parentElement.parent is KtDotQualifiedExpression &&
            (parentElement.parent as KtDotQualifiedExpression).elementType == DOT_QUALIFIED_EXPRESSION
        val isACallExpression = parentElement?.parent is KtCallExpression

        return containsItInProperty || isACallExpression || (containsItInProperty && partOfDotQualifiedExpression)
    }
}
