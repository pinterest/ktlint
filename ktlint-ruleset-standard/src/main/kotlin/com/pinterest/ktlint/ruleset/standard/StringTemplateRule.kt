package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_TEMPLATE_ENTRY_END
import com.pinterest.ktlint.core.ast.ElementType.LONG_TEMPLATE_ENTRY_START
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtThisExpression

public class StringTemplateRule : Rule("string-template") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
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
            if (entryExpression is KtDotQualifiedExpression) {
                val receiver = entryExpression.receiverExpression
                if (entryExpression.selectorExpression?.text == "toString()" && receiver !is KtSuperExpression) {
                    emit(
                        entryExpression.operationTokenNode.startOffset,
                        "Redundant \"toString()\" call in string template",
                        true,
                    )
                    if (autoCorrect) {
                        entryExpression.replace(receiver)
                        entryExpression = receiver
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
                    val leftCurlyBraceNode = node.findChildByType(LONG_TEMPLATE_ENTRY_START)
                    val rightCurlyBraceNode = node.findChildByType(LONG_TEMPLATE_ENTRY_END)
                    if (leftCurlyBraceNode != null && rightCurlyBraceNode != null) {
                        node.removeChild(leftCurlyBraceNode)
                        node.removeChild(rightCurlyBraceNode)
                        val remainingNode = node.firstChildNode
                        val newNode = if (remainingNode.elementType == DOT_QUALIFIED_EXPRESSION) {
                            LeafPsiElement(REGULAR_STRING_PART, "\$${remainingNode.text}")
                        } else {
                            LeafPsiElement(remainingNode.elementType, "\$${remainingNode.text}")
                        }
                        node.replaceChild(node.firstChildNode, newNode)
                    }
                }
            }
        }
    }

    private fun Char.isPartOfIdentifier() = this == '_' || this.isLetterOrDigit()
}
