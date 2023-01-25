package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.ruleset.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.ruleset.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.ruleset.core.api.ElementType.KDOC_END
import com.pinterest.ktlint.ruleset.core.api.ElementType.KDOC_START
import com.pinterest.ktlint.ruleset.core.api.isWhiteSpace
import com.pinterest.ktlint.ruleset.core.api.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoConsecutiveCommentsRule :
    Rule("no-consecutive-comments"),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isStartOfComment()) {
            node
                .prevLeaf { !it.isWhiteSpace() }
                ?.takeIf { previousNonWhiteSpace -> previousNonWhiteSpace.isEndOfComment() }
                ?.takeUnless { previousNonWhiteSpace ->
                    // In is not uncommon that consecutive EOL comments are used instead of a block comment
                    previousNonWhiteSpace.elementType == EOL_COMMENT && node.elementType == EOL_COMMENT
                }?.let { previousNonWhiteSpace ->
                    emit(
                        node.startOffset,
                        "${node.commentType()} may not be preceded by ${previousNonWhiteSpace.commentType()}",
                        false,
                    )
                }
        }
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
