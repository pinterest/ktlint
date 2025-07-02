package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.1", STABLE)
public class NoConsecutiveBlankLinesRule : StandardRule("no-consecutive-blank-lines") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpace20 && node.prevSibling20 != null) {
            val text = node.getText()
            val newLineCount = text.count { it == '\n' }
            if (newLineCount < 2) {
                return
            }

            val eof = node.nextLeaf == null
            val betweenClassAndPrimaryConstructor = node.isBetweenClassAndPrimaryConstructor()

            if (newLineCount > 2 || eof || betweenClassAndPrimaryConstructor) {
                val split = text.split("\n")
                val offset =
                    node.startOffset +
                        split[0].length +
                        split[1].length +
                        if (betweenClassAndPrimaryConstructor) {
                            1
                        } else {
                            2
                        }
                emit(offset, "Needless blank line(s)", true)
                    .ifAutocorrectAllowed {
                        val newText =
                            buildString {
                                append(split.first())
                                append("\n")
                                if (!eof && !betweenClassAndPrimaryConstructor) {
                                    append("\n")
                                }
                                append(split.last())
                            }
                        node.replaceTextWith(newText)
                    }
            }
        }
    }

    private fun ASTNode.isBetweenClassAndPrimaryConstructor() =
        prevCodeLeaf
            ?.let { prevNode ->
                prevNode.elementType == IDENTIFIER &&
                    prevNode.parent?.elementType == CLASS &&
                    this.nextSibling20?.elementType == PRIMARY_CONSTRUCTOR
            }
            ?: false
}

public val NO_CONSECUTIVE_BLANK_LINES_RULE_ID: RuleId = NoConsecutiveBlankLinesRule().ruleId
