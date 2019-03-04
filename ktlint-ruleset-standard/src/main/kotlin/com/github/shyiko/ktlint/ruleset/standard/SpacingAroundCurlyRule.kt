package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.AT
import com.github.shyiko.ktlint.core.ast.ElementType.CLASS_BODY
import com.github.shyiko.ktlint.core.ast.ElementType.COLONCOLON
import com.github.shyiko.ktlint.core.ast.ElementType.COMMA
import com.github.shyiko.ktlint.core.ast.ElementType.DOT
import com.github.shyiko.ktlint.core.ast.ElementType.EXCLEXCL
import com.github.shyiko.ktlint.core.ast.ElementType.LBRACE
import com.github.shyiko.ktlint.core.ast.ElementType.LBRACKET
import com.github.shyiko.ktlint.core.ast.ElementType.LPAR
import com.github.shyiko.ktlint.core.ast.ElementType.RBRACE
import com.github.shyiko.ktlint.core.ast.ElementType.RPAR
import com.github.shyiko.ktlint.core.ast.ElementType.SAFE_ACCESS
import com.github.shyiko.ktlint.core.ast.ElementType.SEMICOLON
import com.github.shyiko.ktlint.core.ast.isPartOfString
import com.github.shyiko.ktlint.core.ast.nextLeaf
import com.github.shyiko.ktlint.core.ast.prevLeaf
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceAfterMe
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtLambdaExpression

class SpacingAroundCurlyRule : Rule("curly-spacing") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is LeafPsiElement && !node.isPartOfString()) {
            val prevLeaf = node.prevLeaf()
            val nextLeaf = node.nextLeaf()
            val spacingBefore: Boolean
            val spacingAfter: Boolean
            if (node.textMatches("{")) {
                spacingBefore =
                    prevLeaf is PsiWhiteSpace ||
                    prevLeaf?.elementType == AT ||
                    (
                        prevLeaf?.elementType == LPAR &&
                        (node.parent is KtLambdaExpression || node.parent.parent is KtLambdaExpression)
                    )
                spacingAfter = nextLeaf is PsiWhiteSpace || nextLeaf?.elementType == RBRACE
                if (prevLeaf is PsiWhiteSpace &&
                    !prevLeaf.textContains('\n') &&
                    prevLeaf.prevLeaf()?.let {
                        it.elementType == LPAR || it.elementType == AT
                    } == true) {
                    emit(node.startOffset, "Unexpected space before \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf.node.treeParent.removeChild(prevLeaf.node)
                    }
                }
                if (prevLeaf is PsiWhiteSpace &&
                    prevLeaf.textContains('\n') &&
                    (
                        prevLeaf.prevLeaf()?.let {
                            it.elementType == RPAR || KtTokens.KEYWORDS.contains(it.elementType)
                        } == true ||
                        node.treeParent.elementType == CLASS_BODY
                    )
                ) {
                    emit(node.startOffset, "Unexpected newline before \"${node.text}\"", true)
                    if (autoCorrect) {
                        (prevLeaf as LeafPsiElement).rawReplaceWithText(" ")
                    }
                }
            } else if (node.textMatches("}")) {
                spacingBefore = prevLeaf is PsiWhiteSpace || prevLeaf?.elementType == LBRACE
                spacingAfter = nextLeaf == null || nextLeaf is PsiWhiteSpace || shouldNotToBeSeparatedBySpace(nextLeaf)
                if (nextLeaf is PsiWhiteSpace && !nextLeaf.textContains('\n') &&
                        shouldNotToBeSeparatedBySpace(nextLeaf.nextLeaf())) {
                    emit(node.startOffset, "Unexpected space after \"${node.text}\"", true)
                    if (autoCorrect) {
                        nextLeaf.node.treeParent.removeChild(nextLeaf.node)
                    }
                }
            } else {
                return
            }
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

    fun shouldNotToBeSeparatedBySpace(leaf: ASTNode?): Boolean {
        val nextElementType = leaf?.elementType
        return (
            nextElementType == DOT ||
                nextElementType == COMMA ||
                nextElementType == RPAR ||
                nextElementType == SEMICOLON ||
                nextElementType == SAFE_ACCESS ||
                nextElementType == EXCLEXCL ||
                nextElementType == LBRACKET ||
                nextElementType == LPAR ||
                nextElementType == COLONCOLON
            )
    }
}
