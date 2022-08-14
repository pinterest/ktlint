package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiCommentImpl

/**
 * Checks external wrapping of block comments. Wrapping inside the comment is not altered. A block comment following
 * another element on the same line is replaced with an EOL comment, if possible.
 */
public class CommentWrappingRule :
    Rule("$experimentalRulesetId:comment-wrapping"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            DefaultEditorConfigProperties.indentSizeProperty,
            DefaultEditorConfigProperties.indentStyleProperty,
        )

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == BLOCK_COMMENT) {
            val nonIndentLeafOnSameLinePrecedingBlockComment =
                node
                    .prevLeaf()
                    ?.takeIf { isNonIndentLeafOnSameLine(it) }
            val nonIndentLeafOnSameLineFollowingBlockComment =
                node
                    .nextLeaf()
                    ?.takeIf { isNonIndentLeafOnSameLine(it) }

            if (nonIndentLeafOnSameLinePrecedingBlockComment != null &&
                nonIndentLeafOnSameLineFollowingBlockComment != null
            ) {
                if (nonIndentLeafOnSameLinePrecedingBlockComment.lineNumber() == nonIndentLeafOnSameLineFollowingBlockComment.lineNumber()) {
                    // Do not try to fix constructs like below:
                    //    val foo /* some comment */ = "foo"
                    emit(
                        node.startOffset,
                        "A block comment in between other elements on the same line is disallowed",
                        false,
                    )
                } else {
                    // Do not try to fix constructs like below:
                    //    val foo = "foo" /*
                    //    some comment
                    //    */ val bar = "bar"
                    emit(
                        node.startOffset,
                        "A block comment starting on same line as another element and ending on another line before another element is disallowed",
                        false,
                    )
                }
                return
            }

            nonIndentLeafOnSameLinePrecedingBlockComment
                ?.precedesBlockCommentOnSameLine(node, emit, autoCorrect)

            nonIndentLeafOnSameLineFollowingBlockComment
                ?.followsBlockCommentOnSameLine(node, emit, autoCorrect)
        }
    }

    private fun ASTNode.precedesBlockCommentOnSameLine(
        blockCommentNode: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val leafAfterBlockComment = blockCommentNode.nextLeaf()
        if (!blockCommentNode.textContains('\n') && leafAfterBlockComment.isLastElementOnLine()) {
            emit(
                startOffset,
                "A single line block comment after a code element on the same line must be replaced with an EOL comment",
                true,
            )
            if (autoCorrect) {
                blockCommentNode.replaceWithEndOfLineComment()
            }
        } else {
            // It can not be autocorrected as it might depend on the situation and code style what is preferred.
            emit(
                blockCommentNode.startOffset,
                "A block comment after any other element on the same line must be separated by a new line",
                false,
            )
        }
    }

    private fun ASTNode.replaceWithEndOfLineComment() {
        val content = text.removeSurrounding("/*", "*/").trim()
        val eolComment = PsiCommentImpl(EOL_COMMENT, "// $content")
        (this as LeafPsiElement).rawInsertBeforeMe(eolComment)
        rawRemove()
    }

    private fun ASTNode.followsBlockCommentOnSameLine(
        blockCommentNode: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        emit(startOffset, "A block comment may not be followed by any other element on that same line", true)
        if (autoCorrect) {
            if (elementType == WHITE_SPACE) {
                (this as LeafPsiElement).rawReplaceWithText("\n${blockCommentNode.lineIndent()}")
            } else {
                (this as LeafPsiElement).upsertWhitespaceBeforeMe("\n${blockCommentNode.lineIndent()}")
            }
        }
    }

    private fun isNonIndentLeafOnSameLine(it: ASTNode) =
        it.elementType != WHITE_SPACE || !it.textContains('\n')

    private fun ASTNode?.isLastElementOnLine() =
        this == null || (elementType == WHITE_SPACE && textContains('\n'))
}
