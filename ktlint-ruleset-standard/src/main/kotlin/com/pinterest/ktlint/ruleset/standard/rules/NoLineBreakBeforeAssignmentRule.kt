package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.psiUtil.siblings

public class NoLineBreakBeforeAssignmentRule : StandardRule("no-line-break-before-assignment") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == EQ) {
            visitEquals(node, emit, autoCorrect)
        }
    }

    private fun visitEquals(
        assignmentNode: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        assignmentNode
            .prevSibling()
            .takeIf { it.isWhiteSpaceWithNewline() }
            ?.let { unexpectedNewlineBeforeAssignment ->
                emit(unexpectedNewlineBeforeAssignment.startOffset, "Line break before assignment is not allowed", true)
                if (autoCorrect) {
                    val parent = assignmentNode.treeParent
                    // Insert assigment surrounded by whitespaces at new position
                    assignmentNode
                        .siblings(false)
                        .takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
                        .last()
                        .let { before ->
                            if (!before.prevSibling().isWhiteSpace()) {
                                parent.addChild(PsiWhiteSpaceImpl(" "), before)
                            }
                            parent.addChild(LeafPsiElement(EQ, "="), before)
                            if (!before.isWhiteSpace()) {
                                parent.addChild(PsiWhiteSpaceImpl(" "), before)
                            }
                        }
                    // Cleanup old assignment and whitespace after it. The indent before the old assignment is kept unchanged
                    assignmentNode
                        .nextSibling()
                        .takeIf { it.isWhiteSpace() }
                        ?.let { whiteSpaceAfterEquals ->
                            parent.removeChild(whiteSpaceAfterEquals)
                        }
                    parent.removeChild(assignmentNode)
                }
            }
    }
}

public val NO_LINE_BREAK_BEFORE_ASSIGNMENT_RULE_ID: RuleId = NoLineBreakBeforeAssignmentRule().ruleId
