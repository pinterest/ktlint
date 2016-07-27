package com.github.shyiko.ktlint.rule

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
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtValueArgument

class SpacingAroundOperatorsRule : Rule {

    private val tokenSet = TokenSet.create(MUL, PLUS, MINUS, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ,
        EXCLEQ, ANDAND, OROR, ELVIS, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, ARROW)

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (tokenSet.contains(node.elementType) && node is LeafPsiElement &&
            !node.isPartOf(KtPrefixExpression::class) && // not unary
            !node.isPartOf(KtTypeParameterList::class) && // fun <T>fn(): T {}
            !node.isPartOf(KtTypeArgumentList::class) && // C<T>
            !node.isPartOf(KtValueArgument::class) && // fn(*array)
            !node.isPartOf(KtImportDirective::class) // import *
        ) {
            val spacingBefore = PsiTreeUtil.prevLeaf(node, true) is PsiWhiteSpace
            val spacingAfter = PsiTreeUtil.nextLeaf(node, true) is PsiWhiteSpace
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(RuleViolation(node.startOffset,
                        "Missing spacing around \"${node.text}\"", correct))
                    if (correct) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingBefore -> {
                    emit(RuleViolation(node.startOffset,
                        "Missing spacing before \"${node.text}\"", correct))
                    if (correct) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingAfter -> {
                    emit(RuleViolation(node.startOffset + 1,
                        "Missing spacing after \"${node.text}\"", correct))
                    if (correct) {
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
            }
        }
    }

}
