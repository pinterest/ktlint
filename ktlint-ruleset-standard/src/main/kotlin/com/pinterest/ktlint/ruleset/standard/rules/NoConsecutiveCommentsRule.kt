package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_END
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_START
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Consecutive comments should be disallowed in following cases:
 *   - Any mix of a consecutive kdoc, a block comment or an EOL comment unless separated by a blank line in between
 *   - Consecutive KDocs (even when separated by a blank line)
 *   - Consecutive block comments (even when separated by a blank line)
 *
 * Consecutive EOL comments are always allowed as they are often used instead of a block comment.
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class NoConsecutiveCommentsRule :
    StandardRule("no-consecutive-comments"),
    Rule.OfficialCodeStyle {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.isStartOfComment() }
            ?.prevLeaf { !it.isWhiteSpace() }
            ?.takeIf { previousNonWhiteSpace -> previousNonWhiteSpace.isEndOfComment() }
            ?.let { previousComment ->
                when {
                    previousComment.elementType == KDOC_END && node.elementType == KDOC_START -> {
                        // Disallow consecutive KDocs (even when separated by a blank line):
                        //    /**
                        //     * KDoc 1
                        //     */
                        //
                        //    /**
                        //     * KDoc 2
                        //     */
                        // Disallow block and eol comments preceded by a KDOC (even when separated by a blank line):
                        //    /**
                        //     * KDoc 1
                        //     */
                        //
                        //    //
                        emit(
                            node.startOffset,
                            "${node.commentType()} may not be preceded by ${previousComment.commentType()}",
                            false,
                        )
                        true
                    }

                    previousComment.elementType == KDOC_END && node.elementType != KDOC_START -> {
                        // Disallow block and eol comments preceded by a KDOC (even when separated by a blank line):
                        //    /**
                        //     * KDoc 1
                        //     */
                        //
                        //    //
                        emit(
                            node.startOffset,
                            "${node.commentType()} may not be preceded by ${previousComment.commentType()}. Reversed order is allowed " +
                                "though when separated by a newline.",
                            false,
                        )
                        true
                    }

                    previousComment.elementType == BLOCK_COMMENT && node.elementType == BLOCK_COMMENT -> {
                        // Disallow consecutive block comments (even when separated by a blank line):
                        //    /*
                        //     * Block comment 1
                        //     */
                        //
                        //    /**
                        //     * Block comment 2
                        //     */
                        emit(
                            node.startOffset,
                            "${node.commentType()} may not be preceded by ${previousComment.commentType()}",
                            false,
                        )
                    }

                    previousComment.elementType == EOL_COMMENT && node.elementType == EOL_COMMENT -> {
                        // Allow consecutive EOL-comments:
                        //    // EOL-comment 1
                        //    // EOL-comment 2
                        false
                    }

                    previousComment.elementType != node.elementType &&
                        node
                            .prevLeaf()
                            .takeIf { it.isWhiteSpace() }
                            ?.text
                            .orEmpty()
                            .count { it == '\n' } > 1 -> {
                        // Allow different element types when separated by a blank line
                        false
                    }

                    else -> {
                        emit(
                            node.startOffset,
                            "${node.commentType()} may not be preceded by ${previousComment.commentType()} unless separated by a blank " +
                                "line",
                            false,
                        )
                    }
                }
            }
            ?: false
    }

    private fun ASTNode?.isStartOfComment() =
        when (this?.elementType) {
            EOL_COMMENT,
            BLOCK_COMMENT,
            KDOC_START,
            ->
                true

            else ->
                false
        }

    private fun ASTNode?.isEndOfComment() =
        when (this?.elementType) {
            EOL_COMMENT,
            BLOCK_COMMENT,
            KDOC_END,
            ->
                true

            else ->
                false
        }

    private fun ASTNode.commentType() =
        when (this.elementType) {
            EOL_COMMENT -> "an EOL comment"

            BLOCK_COMMENT -> "a block comment"

            KDOC_START,
            KDOC_END,
            -> "a KDoc"

            else -> this.elementType.toString().lowercase()
        }
}

public val NO_CONSECUTIVE_COMMENTS_RULE_ID: RuleId = NoConsecutiveCommentsRule().ruleId
