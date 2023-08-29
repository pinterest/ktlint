package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CATCH_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DO_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FINALLY_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GET_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SET_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHILE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet.create
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtWhenEntry

@SinceKtlint("0.1", STABLE)
public class SpacingAroundKeywordRule : StandardRule("keyword-spacing") {
    private val noLFBeforeSet = create(ELSE_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD)
    private val tokenSet =
        create(
            FOR_KEYWORD, IF_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            TRY_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD, WHEN_KEYWORD,
        )

    private val keywordsWithoutSpaces = create(GET_KEYWORD, SET_KEYWORD)

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is LeafPsiElement) {
            if (tokenSet.contains(node.elementType) && node.parent !is KDocName && node.nextLeaf() !is PsiWhiteSpace) {
                emit(node.startOffset + node.text.length, "Missing spacing after \"${node.text}\"", true)
                if (autoCorrect) {
                    (node as ASTNode).upsertWhitespaceAfterMe(" ")
                }
            } else if (keywordsWithoutSpaces.contains(node.elementType) && node.nextLeaf() is PsiWhiteSpace) {
                val parent = node.parent
                val nextLeaf = node.nextLeaf()
                if (parent is KtPropertyAccessor && parent.hasBody() && nextLeaf != null) {
                    emit(node.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        nextLeaf.treeParent.removeChild(nextLeaf)
                    }
                }
            }
            if (noLFBeforeSet.contains(node.elementType)) {
                val prevLeaf = node.prevLeaf()
                val isElseKeyword = node.elementType == ELSE_KEYWORD
                if (
                    prevLeaf?.elementType == WHITE_SPACE &&
                    prevLeaf.textContains('\n') &&
                    (!isElseKeyword || node.parent !is KtWhenEntry)
                ) {
                    val rBrace = prevLeaf.prevLeaf()?.takeIf { it.elementType == RBRACE }
                    val parentOfRBrace = rBrace?.treeParent
                    if (
                        parentOfRBrace is KtBlockExpression &&
                        (!isElseKeyword || parentOfRBrace.treeParent?.treeParent == node.treeParent)
                    ) {
                        emit(node.startOffset, "Unexpected newline before \"${node.text}\"", true)
                        if (autoCorrect) {
                            (prevLeaf as LeafElement).rawReplaceWithText(" ")
                        }
                    }
                }
            }
        }
    }
}

public val SPACING_AROUND_KEYWORD_RULE_ID: RuleId = SpacingAroundKeywordRule().ruleId
