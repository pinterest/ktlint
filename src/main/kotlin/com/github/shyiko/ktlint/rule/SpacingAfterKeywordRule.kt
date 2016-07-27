package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens.CATCH_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.DO_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.ELSE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.FINALLY_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.FOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.IF_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.TRY_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.WHEN_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.WHILE_KEYWORD

class SpacingAfterKeywordRule : Rule {

    private val tokenSet = TokenSet.create(FOR_KEYWORD, IF_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
        TRY_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD, WHEN_KEYWORD)
    // todo: but not after fun(, get(, set(

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (tokenSet.contains(node.elementType) && node is LeafPsiElement &&
            PsiTreeUtil.nextLeaf(node) !is PsiWhiteSpace) {
            emit(RuleViolation(node.startOffset + node.text.length, "Missing spacing after \"${node.text}\"", correct))
            if (correct) {
                node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
            }
        }
    }
}
