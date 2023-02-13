package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.siblings

public class NoLineBreakBeforeAssignmentRule : StandardRule("no-line-break-before-assignment") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == EQ) {
            val prevCodeSibling = node.prevCodeSibling()
            val unexpectedLinebreak =
                prevCodeSibling
                    ?.siblings()
                    ?.takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
                    ?.lastOrNull { it.isWhiteSpaceWithNewline() }
            if (unexpectedLinebreak != null) {
                emit(unexpectedLinebreak.startOffset, "Line break before assignment is not allowed", true)
                if (autoCorrect) {
                    val prevPsi = prevCodeSibling.psi
                    val parentPsi = prevPsi.parent
                    val psiFactory = KtPsiFactory(prevPsi)
                    if (prevPsi.nextSibling !is PsiWhiteSpace) {
                        parentPsi.addAfter(psiFactory.createWhiteSpace(), prevPsi)
                    }
                    parentPsi.addAfter(psiFactory.createEQ(), prevPsi)
                    parentPsi.addAfter(psiFactory.createWhiteSpace(), prevPsi)
                    (node as? LeafPsiElement)?.delete()
                }
            }
        }
    }
}

public val NO_LINE_BREAK_BEFORE_ASSIGNMENT_RULE_ID: RuleId = NoLineBreakBeforeAssignmentRule().ruleId
