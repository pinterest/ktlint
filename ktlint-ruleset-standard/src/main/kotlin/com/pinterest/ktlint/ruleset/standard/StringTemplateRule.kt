package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_EXPRESSION
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression

class StringTemplateRule : Rule("string-template") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val elementType = node.elementType
        // code below is commented out because (setting aside potentially dangerous replaceChild part)
        // `val v: String = "$elementType"` would be rewritten to `val v: String = elementType.toString()` and it's not
        // immediately clear which is better
/*
        if (elementType === SHORT_STRING_TEMPLATE_ENTRY || elementType == LONG_STRING_TEMPLATE_ENTRY) {
            if (node.treePrev.elementType == OPEN_QUOTE && node.treeNext.elementType == CLOSING_QUOTE) {
                emit(node.treePrev.startOffset, "Redundant string template", true)
                val entryStart = node.psi.firstChild
                node.treeParent.treeParent.replaceChild(node.treeParent, entryStart.nextSibling.node)
            }
*/
        if (elementType == LONG_STRING_TEMPLATE_ENTRY) {
            var entryExpression = (node.psi as? KtBlockStringTemplateEntry)?.expression
            val entryStart = node.firstChildNode
            val dotQualifiedExpression = entryStart.treeNext
            if (dotQualifiedExpression?.elementType == DOT_QUALIFIED_EXPRESSION) {
                val callExpression = dotQualifiedExpression.lastChildNode
                val dot = callExpression.treePrev
                if (dot?.elementType == DOT &&
                    callExpression.text == "toString()" &&
                    dotQualifiedExpression.firstChildNode?.elementType != SUPER_EXPRESSION
                ) {
                    emit(dot.startOffset, "Redundant \"toString()\" call in string template", true)
                    if (autoCorrect) {
                        entryExpression = (entryExpression as? KtDotQualifiedExpression)?.receiverExpression
                        node.removeChild(dot)
                        node.removeChild(callExpression)
                    }
                }
            }
            if (node.text.startsWith("${'$'}{") &&
                node.text.let { it.substring(2, it.length - 1) }.all { it.isPartOfIdentifier() } &&
                (entryExpression is KtNameReferenceExpression || entryExpression is KtThisExpression) &&
                node.treeNext.let { nextSibling ->
                    nextSibling.elementType == CLOSING_QUOTE ||
                        (
                            nextSibling.elementType == LITERAL_STRING_TEMPLATE_ENTRY &&
                                !nextSibling.text[0].isPartOfIdentifier()
                            )
                }
            ) {
                emit(node.treePrev.startOffset + 2, "Redundant curly braces", true)
                if (autoCorrect) {
                    // fixme: a proper way would be to downcast to SHORT_STRING_TEMPLATE_ENTRY
                    (node.firstChildNode as LeafPsiElement).rawReplaceWithText("$") // entry start
                    (node.lastChildNode as LeafPsiElement).rawReplaceWithText("") // entry end
                }
            }
        }
    }

    private fun Char.isPartOfIdentifier() = this == '_' || this.isLetterOrDigit()
}
