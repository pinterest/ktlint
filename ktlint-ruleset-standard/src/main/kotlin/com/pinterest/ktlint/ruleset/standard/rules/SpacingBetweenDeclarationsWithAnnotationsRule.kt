package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35106
 */
@SinceKtlint("0.37", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class SpacingBetweenDeclarationsWithAnnotationsRule : StandardRule("spacing-between-declarations-with-annotations") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.psi is KtDeclaration && node.isAnnotated()) {
            visitDeclaration(node, emit, autoCorrect)
        }
    }

    private fun visitDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .psi
            ?.getPrevSiblingIgnoringWhitespaceAndComments(withItself = false)
            ?.takeIf { it is KtDeclaration }
            ?.takeIf { prevDeclaration -> hasNoBlankLineBetweenDeclarations(node, prevDeclaration) }
            ?.let {
                val prevLeaf = node.prevCodeLeaf()?.nextLeaf { it.isWhiteSpace() }!!
                emit(
                    prevLeaf.startOffset + 1,
                    "Declarations and declarations with annotations should have an empty space between.",
                    true,
                )
                if (autoCorrect) {
                    prevLeaf.upsertWhitespaceBeforeMe("\n".plus(node.indent()))
                }
            }
    }

    private fun ASTNode.isAnnotated(): Boolean =
        findChildByType(MODIFIER_LIST)
            ?.children()
            ?.any { it.psi is KtAnnotationEntry }
            ?: false

    private fun hasNoBlankLineBetweenDeclarations(
        node: ASTNode,
        prevDeclaration: PsiElement,
    ) = node
        .leaves(false)
        .takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
        .takeWhile { it.psi != prevDeclaration }
        .none { it.isBlankLine() }

    private fun ASTNode.isBlankLine() = isWhiteSpace() && text.count { it == '\n' } > 1
}

public val SPACING_BETWEEN_DECLARATIONS_WITH_ANNOTATIONS_RULE_ID: RuleId = SpacingBetweenDeclarationsWithAnnotationsRule().ruleId
