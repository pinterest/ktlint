package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

/**
 * When present, align the initial star in a block comment.
 */
public class BlockCommentInitialStarAlignmentRule :
    StandardRule(
        "block-comment-initial-star-alignment",
        visitorModifiers =
            setOf(
                // The block comment is a node which can contain multiple lines. The indent of the second and later line
                // should be determined based on the indent of the block comment node. This indent is determined by the
                // indentation rule.
                VisitorModifier.RunAfterRule(
                    ruleId = INDENTATION_RULE_ID,
                    mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                ),
            ),
    ) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == BLOCK_COMMENT) {
            val expectedIndentForLineWithInitialStar = node.indent(false) + " *"
            val lines = node.text.split("\n")
            var offset = node.startOffset
            val modifiedLines = mutableListOf<String>()
            lines.forEach { line ->
                val modifiedLine =
                    CONTINUATION_COMMENT_REGEX
                        .find(line)
                        ?.let { matchResult ->
                            val (prefix, content) = matchResult.destructured
                            if (prefix != expectedIndentForLineWithInitialStar) {
                                emit(offset + prefix.length, "Initial star should align with start of block comment", true)
                                expectedIndentForLineWithInitialStar + content
                            } else {
                                line
                            }
                        }
                        ?: line
                modifiedLines.add(modifiedLine)
                offset += line.length + 1
            }
            if (autoCorrect) {
                val newText = modifiedLines.joinToString(separator = "\n")
                if (node.text != newText) {
                    (node as LeafElement).rawReplaceWithText(newText)
                }
            }
        }
    }

    private companion object {
        val CONTINUATION_COMMENT_REGEX = Regex("^([\t ]+\\*)(.*)$")
    }
}

public val BLOCK_COMMENT_INITIAL_STAR_ALIGNMENT_RULE_ID: RuleId = BlockCommentInitialStarAlignmentRule().ruleId
