package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

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
        if (elementType === SHORT_STRING_TEMPLATE_ENTRY || elementType == KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY) {
            if (node.treePrev.elementType == KtTokens.OPEN_QUOTE && node.treeNext.elementType == KtTokens.CLOSING_QUOTE) {
                emit(node.treePrev.startOffset, "Redundant string template", true)
                val entryStart = node.psi.firstChild
                node.treeParent.treeParent.replaceChild(node.treeParent, entryStart.nextSibling.node)
            }
*/
        if (elementType == KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY) {
            val entryStart = node.psi.firstChild
            val dotQualifiedExpression = entryStart.nextSibling
            if (dotQualifiedExpression?.node?.elementType == KtStubElementTypes.DOT_QUALIFIED_EXPRESSION) {
                val callExpression = dotQualifiedExpression!!.lastChild
                val dot = callExpression.prevSibling
                if (dot?.node?.elementType == KtTokens.DOT &&
                    callExpression.text == "toString()" &&
                    dotQualifiedExpression.firstChild?.node?.elementType != KtNodeTypes.SUPER_EXPRESSION) {
                    emit(dot.node.startOffset, "Redundant \"toString()\" call in string template", true)
                    if (autoCorrect) {
                        node.removeChild(dot.node)
                        node.removeChild(callExpression.node)
                    }
                }
            }
            if (node.text.startsWith("${'$'}{") &&
                node.text.let { it.substring(2, it.length - 1) }.all { it.isPartOfIdentifier() } &&
                (node.treeNext.elementType == KtTokens.CLOSING_QUOTE ||
                    (node.psi.nextSibling.node.elementType == KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY &&
                        !node.psi.nextSibling.text[0].isPartOfIdentifier()))) {
                emit(node.treePrev.startOffset + 2, "Redundant curly braces", true)
                if (autoCorrect) {
                    // fixme: a proper way would be to downcast to SHORT_STRING_TEMPLATE_ENTRY
                    (node.psi.firstChild as LeafPsiElement).rawReplaceWithText("$") // entry start
                    (node.psi.lastChild as LeafPsiElement).rawReplaceWithText("") // entry end
                }
            }
        }
    }

    private fun Char.isPartOfIdentifier() = this == '_' || this.isLetterOrDigit()
}
