package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.lineIndent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

/**
 * When present, align the initial star in a block comment.
 */
class BlockCommentInitialStarAlignmentRule :
    Rule(
        "$experimentalRulesetId:block-comment-initial-star-alignment",
        visitorModifiers = setOf(
            // The block comment is a node which can contain multiple lines. The indent of the second and later line
            // should be determined based on the indent of the block comment node. This indent is determined by the
            // indentation rule.
            VisitorModifier.RunAfterRule("standard:indent")
        )
    ) {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == BLOCK_COMMENT) {
            val expectedIndentForLineWithInitialStar = node.lineIndent() + " *"
            val lines = node.text.split("\n")
            var offset = node.startOffset
            val modifiedLines = mutableListOf<String>()
            lines.forEach { line ->
                val modifiedLine =
                    continuationCommentRegex
                        .find(line)
                        ?.let { matchResult ->
                            val (prefix, content) = matchResult.destructured
                            if (prefix != expectedIndentForLineWithInitialStar) {
                                emit(offset + prefix.length, "Initial star should be align with start of block comment", true)
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
        val continuationCommentRegex = Regex("^([\t ]+\\*)(.*)$")
    }
}
