package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.psiUtil.siblings

@SinceKtlint("0.13", STABLE)
public class NoLineBreakBeforeAssignmentRule : StandardRule("no-line-break-before-assignment") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == EQ) {
            visitEquals(node, emit)
        }
    }

    private fun visitEquals(
        assignmentNode: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        assignmentNode
            .prevSibling20
            .takeIf { it.isWhiteSpaceWithNewline20 }
            ?.let { unexpectedNewlineBeforeAssignment ->
                emit(unexpectedNewlineBeforeAssignment.startOffset, "Line break before assignment is not allowed", true)
                    .ifAutocorrectAllowed {
                        val parent = assignmentNode.parent!!
                        // Insert assignment surrounded by whitespaces at new position
                        assignmentNode
                            .siblings(false)
                            .takeWhile { !it.isCode }
                            .last()
                            .let { before ->
                                if (!before.prevSibling20.isWhiteSpace20) {
                                    parent.addChild(PsiWhiteSpaceImpl(" "), before)
                                }
                                parent.addChild(LeafPsiElement(EQ, "="), before)
                                if (!before.isWhiteSpace20) {
                                    parent.addChild(PsiWhiteSpaceImpl(" "), before)
                                }
                            }
                        // Cleanup old assignment and whitespace after it. The indent before the old assignment is kept unchanged
                        assignmentNode
                            .nextSibling20
                            .takeIf { it.isWhiteSpace20 }
                            ?.remove()
                        assignmentNode.remove()
                    }
            }
    }
}

public val NO_LINE_BREAK_BEFORE_ASSIGNMENT_RULE_ID: RuleId = NoLineBreakBeforeAssignmentRule().ruleId
