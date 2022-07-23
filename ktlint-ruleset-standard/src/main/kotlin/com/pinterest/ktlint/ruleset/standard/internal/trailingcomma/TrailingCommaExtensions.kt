package com.pinterest.ktlint.ruleset.standard.internal.trailingcomma

import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.containsLineBreakInRange
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

internal fun ASTNode.reportAndCorrectTrailingCommaNodeBefore(
    inspectNode: ASTNode,
    isTrailingCommaAllowed: Boolean,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
) {
    val prevLeaf = inspectNode.prevLeaf()
    val trailingCommaNode = prevLeaf.findPreviousTrailingCommaNodeOrNull()
    val trailingCommaState = when {
        isMultiline(psi) -> if (trailingCommaNode != null) TrailingCommaState.EXISTS else TrailingCommaState.MISSING
        else -> if (trailingCommaNode != null) TrailingCommaState.REDUNDANT else TrailingCommaState.NOT_EXISTS
    }
    when (trailingCommaState) {
        TrailingCommaState.EXISTS -> if (!isTrailingCommaAllowed) {
            emit(
                trailingCommaNode!!.startOffset,
                "Unnecessary trailing comma before \"${inspectNode.text}\"",
                true
            )
            if (autoCorrect) {
                this.removeChild(trailingCommaNode)
            }
        }
        TrailingCommaState.MISSING -> if (isTrailingCommaAllowed) {
            val addNewLineBeforeArrowInWhenEntry = addNewLineBeforeArrowInWhen()
            val prevNode = inspectNode.prevCodeLeaf()!!
            if (addNewLineBeforeArrowInWhenEntry) {
                emit(
                    prevNode.startOffset + prevNode.textLength,
                    "Missing trailing comma and newline before \"${inspectNode.text}\"",
                    true
                )
            } else {
                emit(
                    prevNode.startOffset + prevNode.textLength,
                    "Missing trailing comma before \"${inspectNode.text}\"",
                    true
                )
            }
            if (autoCorrect) {
                if (addNewLineBeforeArrowInWhenEntry) {
                    val parentIndent = (prevNode.psi.parent.prevLeaf() as? PsiWhiteSpace)?.text ?: "\n"
                    val leafBeforeArrow = (psi as KtWhenEntry).arrow?.prevLeaf()
                    if (leafBeforeArrow != null && leafBeforeArrow is PsiWhiteSpace) {
                        val newLine = KtPsiFactory(prevNode.psi).createWhiteSpace(parentIndent)
                        leafBeforeArrow.replace(newLine)
                    } else {
                        val newLine = KtPsiFactory(prevNode.psi).createWhiteSpace(parentIndent)
                        prevNode.psi.parent.addAfter(newLine, prevNode.psi)
                    }
                }
                val comma = KtPsiFactory(prevNode.psi).createComma()
                prevNode.psi.parent.addAfter(comma, prevNode.psi)
            }
        }
        TrailingCommaState.REDUNDANT -> {
            emit(
                trailingCommaNode!!.startOffset,
                "Unnecessary trailing comma before \"${inspectNode.text}\"",
                true
            )
            if (autoCorrect) {
                this.removeChild(trailingCommaNode)
            }
        }
        TrailingCommaState.NOT_EXISTS -> Unit
    }
}

private fun isMultiline(element: PsiElement): Boolean = when {
    element.parent is KtFunctionLiteral -> isMultiline(element.parent)
    element is KtFunctionLiteral -> containsLineBreakInRange(element.valueParameterList!!, element.arrow!!)
    element is KtWhenEntry -> containsLineBreakInRange(element.firstChild, element.arrow!!)
    element is KtDestructuringDeclaration -> containsLineBreakInRange(element.lPar!!, element.rPar!!)
    element is KtValueArgumentList && element.children.size == 1 && element.anyDescendantOfType<KtCollectionLiteralExpression>() -> {
        // special handling for collection literal
        // @Annotation([
        //    "something",
        // ])
        val lastChild = element.collectDescendantsOfType<KtCollectionLiteralExpression>().last()
        containsLineBreakInLeafsRange(lastChild.rightBracket!!, element.rightParenthesis!!)
    }
    else -> element.textContains('\n')
}

private fun ASTNode.addNewLineBeforeArrowInWhen() =
    if (psi is KtWhenEntry) {
        val leafBeforeArrow = (psi as KtWhenEntry).arrow?.prevLeaf()
        !(leafBeforeArrow is PsiWhiteSpace && leafBeforeArrow.textContains('\n'))
    } else {
        false
    }

private fun ASTNode?.findPreviousTrailingCommaNodeOrNull(): ASTNode? {
    var node = this
    while (node?.isIgnorable() == true) {
        node = node.prevLeaf()
    }
    return if (node?.elementType == ElementType.COMMA) {
        node
    } else {
        null
    }
}

private fun containsLineBreakInLeafsRange(from: PsiElement, to: PsiElement): Boolean {
    var leaf: PsiElement? = from
    while (leaf != null && !leaf.isEquivalentTo(to)) {
        if (leaf.textContains('\n')) {
            return true
        }
        leaf = leaf.nextLeaf(skipEmptyElements = false)
    }
    return leaf?.textContains('\n') ?: false
}

private fun ASTNode.isIgnorable(): Boolean =
    elementType == ElementType.WHITE_SPACE ||
        elementType == ElementType.EOL_COMMENT ||
        elementType == ElementType.BLOCK_COMMENT
