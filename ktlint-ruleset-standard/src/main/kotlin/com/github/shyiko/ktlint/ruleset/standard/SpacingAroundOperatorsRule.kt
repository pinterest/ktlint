package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.ANDAND
import com.github.shyiko.ktlint.core.ast.ElementType.ARROW
import com.github.shyiko.ktlint.core.ast.ElementType.DIV
import com.github.shyiko.ktlint.core.ast.ElementType.DIVEQ
import com.github.shyiko.ktlint.core.ast.ElementType.ELVIS
import com.github.shyiko.ktlint.core.ast.ElementType.EQ
import com.github.shyiko.ktlint.core.ast.ElementType.EQEQ
import com.github.shyiko.ktlint.core.ast.ElementType.EQEQEQ
import com.github.shyiko.ktlint.core.ast.ElementType.EXCLEQ
import com.github.shyiko.ktlint.core.ast.ElementType.EXCLEQEQEQ
import com.github.shyiko.ktlint.core.ast.ElementType.FUN
import com.github.shyiko.ktlint.core.ast.ElementType.GT
import com.github.shyiko.ktlint.core.ast.ElementType.GTEQ
import com.github.shyiko.ktlint.core.ast.ElementType.LT
import com.github.shyiko.ktlint.core.ast.ElementType.LTEQ
import com.github.shyiko.ktlint.core.ast.ElementType.MINUS
import com.github.shyiko.ktlint.core.ast.ElementType.MINUSEQ
import com.github.shyiko.ktlint.core.ast.ElementType.MUL
import com.github.shyiko.ktlint.core.ast.ElementType.MULTEQ
import com.github.shyiko.ktlint.core.ast.ElementType.OROR
import com.github.shyiko.ktlint.core.ast.ElementType.PERC
import com.github.shyiko.ktlint.core.ast.ElementType.PERCEQ
import com.github.shyiko.ktlint.core.ast.ElementType.PLUS
import com.github.shyiko.ktlint.core.ast.ElementType.PLUSEQ
import com.github.shyiko.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.WHITE_SPACE
import com.github.shyiko.ktlint.core.ast.isPartOf
import com.github.shyiko.ktlint.core.ast.nextLeaf
import com.github.shyiko.ktlint.core.ast.parent
import com.github.shyiko.ktlint.core.ast.prevLeaf
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceAfterMe
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtValueArgument

class SpacingAroundOperatorsRule : Rule("op-spacing") {

    private val tokenSet = TokenSet.create(
        MUL, PLUS, MINUS, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ,
        EXCLEQ, ANDAND, OROR, ELVIS, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, ARROW
    )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (tokenSet.contains(node.elementType) &&
            node is LeafElement &&
            !node.isPartOf(KtPrefixExpression::class) && // not unary
            !node.isPartOf(KtTypeArgumentList::class) && // C<T>
            !(node.elementType == MUL && node.isPartOf(KtValueArgument::class)) && // fn(*array)
            !node.isPartOf(KtImportDirective::class) && // import *
            !node.isPartOf(KtSuperExpression::class) // super<T>
        ) {
            if (node.elementType == LT || node.elementType == GT) {
                val list = node.parent(TYPE_PARAMETER_LIST)
                if (list != null) {
                    if (node.elementType == LT) {
                        // ensure no space after < in <T>
                        val nextLeaf = node.nextLeaf()
                        if (nextLeaf?.elementType == WHITE_SPACE && !nextLeaf.textContains('\n')) {
                            emit(node.startOffset + 1, "Unexpected spacing after \"${node.text}\"", true)
                            if (autoCorrect) {
                                nextLeaf.treeParent.removeChild(nextLeaf)
                            }
                        }
                    } else {
                        // ensure no space before > in <T>
                        val prevLeaf = node.prevLeaf()
                        if (prevLeaf?.elementType == WHITE_SPACE && !prevLeaf.textContains('\n')) {
                            emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                            if (autoCorrect) {
                                prevLeaf.treeParent.removeChild(prevLeaf)
                            }
                        }
                    }
                    if (node.parent(TYPE_PARAMETER_LIST)?.treeParent?.elementType != FUN) {
                        return // FIXME: only `fun <T>fn(): T {}` is checked
                    }
                }
            }
            val spacingBefore = node.prevLeaf() is PsiWhiteSpace || node.elementType == GT
            val spacingAfter = node.nextLeaf() is PsiWhiteSpace || node.elementType == LT
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceBeforeMe(" ")
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
                !spacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceBeforeMe(" ")
                    }
                }
                !spacingAfter -> {
                    emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.upsertWhitespaceAfterMe(" ")
                    }
                }
            }
        }
    }
}
