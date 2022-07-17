package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ANDAND
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.DIV
import com.pinterest.ktlint.core.ast.ElementType.DIVEQ
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.EQEQ
import com.pinterest.ktlint.core.ast.ElementType.EQEQEQ
import com.pinterest.ktlint.core.ast.ElementType.EXCLEQ
import com.pinterest.ktlint.core.ast.ElementType.EXCLEQEQEQ
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.GTEQ
import com.pinterest.ktlint.core.ast.ElementType.LT
import com.pinterest.ktlint.core.ast.ElementType.LTEQ
import com.pinterest.ktlint.core.ast.ElementType.MINUS
import com.pinterest.ktlint.core.ast.ElementType.MINUSEQ
import com.pinterest.ktlint.core.ast.ElementType.MUL
import com.pinterest.ktlint.core.ast.ElementType.MULTEQ
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.OROR
import com.pinterest.ktlint.core.ast.ElementType.PERC
import com.pinterest.ktlint.core.ast.ElementType.PERCEQ
import com.pinterest.ktlint.core.ast.ElementType.PLUS
import com.pinterest.ktlint.core.ast.ElementType.PLUSEQ
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPrefixExpression

class SpacingAroundOperatorsRule : Rule("op-spacing") {

    private val tokenSet = TokenSet.create(
        MUL, PLUS, MINUS, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ,
        EXCLEQ, ANDAND, OROR, ELVIS, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, ARROW
    )

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (tokenSet.contains(node.elementType) &&
            node is LeafElement &&
            !node.isPartOf(KtPrefixExpression::class) && // not unary
            !(node.elementType == MUL && node.treeParent.elementType == VALUE_ARGUMENT) && // fn(*array)
            !node.isPartOf(KtImportDirective::class) // import *
        ) {
            if ((node.elementType == LT || node.elementType == GT || node.elementType == MUL) &&
                node.treeParent.elementType != OPERATION_REFERENCE
            ) {
                // Do not format parameter types like:
                //   <T> fun foo(...)
                //   class Foo<T> { ... }
                //   Foo<*>
                return
            }
            val spacingBefore = node.prevLeaf() is PsiWhiteSpace
            val spacingAfter = node.nextLeaf() is PsiWhiteSpace
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
