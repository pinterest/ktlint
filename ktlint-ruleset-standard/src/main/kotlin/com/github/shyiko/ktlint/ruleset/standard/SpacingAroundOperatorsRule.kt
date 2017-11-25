package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens.ANDAND
import org.jetbrains.kotlin.lexer.KtTokens.ARROW
import org.jetbrains.kotlin.lexer.KtTokens.DIV
import org.jetbrains.kotlin.lexer.KtTokens.DIVEQ
import org.jetbrains.kotlin.lexer.KtTokens.ELVIS
import org.jetbrains.kotlin.lexer.KtTokens.EQ
import org.jetbrains.kotlin.lexer.KtTokens.EQEQ
import org.jetbrains.kotlin.lexer.KtTokens.EQEQEQ
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQ
import org.jetbrains.kotlin.lexer.KtTokens.EXCLEQEQEQ
import org.jetbrains.kotlin.lexer.KtTokens.GT
import org.jetbrains.kotlin.lexer.KtTokens.GTEQ
import org.jetbrains.kotlin.lexer.KtTokens.LT
import org.jetbrains.kotlin.lexer.KtTokens.LTEQ
import org.jetbrains.kotlin.lexer.KtTokens.MINUS
import org.jetbrains.kotlin.lexer.KtTokens.MINUSEQ
import org.jetbrains.kotlin.lexer.KtTokens.MUL
import org.jetbrains.kotlin.lexer.KtTokens.MULTEQ
import org.jetbrains.kotlin.lexer.KtTokens.OROR
import org.jetbrains.kotlin.lexer.KtTokens.PERC
import org.jetbrains.kotlin.lexer.KtTokens.PERCEQ
import org.jetbrains.kotlin.lexer.KtTokens.PLUS
import org.jetbrains.kotlin.lexer.KtTokens.PLUSEQ
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class SpacingAroundOperatorsRule : Rule("op-spacing") {

    private val tokenSet = TokenSet.create(MUL, PLUS, MINUS, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ,
        EXCLEQ, ANDAND, OROR, ELVIS, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, ARROW)

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (tokenSet.contains(node.elementType) && node is LeafPsiElement &&
            !node.isPartOf(KtPrefixExpression::class) && // not unary
            !node.isPartOf(KtTypeArgumentList::class) && // C<T>
            !(node.elementType == MUL && node.isPartOf(KtValueArgument::class)) && // fn(*array)
            !node.isPartOf(KtImportDirective::class) && // import *
            !node.isPartOf(KtSuperExpression::class) // super<T>
        ) {
            if ((node.elementType == GT || node.elementType == LT) &&
                // fun <T>fn(): T {}
                node.getNonStrictParentOfType(KtTypeParameterList::class.java)?.parent?.node?.elementType !=
                    KtStubElementTypes.FUNCTION) {
                return
            }
            val spacingBefore = PsiTreeUtil.prevLeaf(node, true) is PsiWhiteSpace ||
                node.elementType == GT
            val spacingAfter = PsiTreeUtil.nextLeaf(node, true) is PsiWhiteSpace ||
                node.elementType == LT
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingAfter -> {
                    emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
            }
        }
    }
}
