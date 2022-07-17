package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Checks external wrapping of KDoc comment. Wrapping inside the KDoc comment is not altered.
 */
public class KdocWrappingRule :
    Rule("$experimentalRulesetId:kdoc-wrapping"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            DefaultEditorConfigProperties.indentSizeProperty,
            DefaultEditorConfigProperties.indentStyleProperty
        )

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KDOC) {
            val nonIndentLeafOnSameLinePrecedingKdocComment =
                node
                    .findChildByType(KDOC_START)
                    ?.prevLeaf()
                    ?.takeIf { isNonIndentLeafOnSameLine(it) }
            val nonIndentLeafOnSameLineFollowingKdocComment =
                node
                    .findChildByType(KDOC_END)
                    ?.nextLeaf()
                    ?.takeIf { isNonIndentLeafOnSameLine(it) }

            if (nonIndentLeafOnSameLinePrecedingKdocComment != null &&
                nonIndentLeafOnSameLineFollowingKdocComment != null
            ) {
                if (nonIndentLeafOnSameLinePrecedingKdocComment.lineNumber() == nonIndentLeafOnSameLineFollowingKdocComment.lineNumber()) {
                    // Do not try to fix constructs like below:
                    //    val foo /** some comment */ = "foo"
                    emit(
                        node.startOffset,
                        "A KDoc comment in between other elements on the same line is disallowed",
                        false
                    )
                } else {
                    // Do not try to fix constructs like below:
                    //    val foo = "foo" /*
                    //    some comment*
                    //    */ val bar = "bar"
                    emit(
                        node.startOffset,
                        "A KDoc comment starting on same line as another element and ending on another line before another element is disallowed",
                        false
                    )
                }
                return
            }

            if (nonIndentLeafOnSameLinePrecedingKdocComment != null) {
                // It can not be autocorrected as it might depend on the situation and code style what is
                // preferred.
                emit(
                    node.startOffset,
                    "A KDoc comment after any other element on the same line must be separated by a new line",
                    false
                )
            }

            nonIndentLeafOnSameLineFollowingKdocComment
                ?.followsKdocCommentOnSameLine(node, emit, autoCorrect)
        }
    }

    private fun ASTNode.followsKdocCommentOnSameLine(
        kdocCommentNode: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        emit(startOffset, "A KDoc comment may not be followed by any other element on that same line", true)
        if (autoCorrect) {
            if (elementType == WHITE_SPACE) {
                (this as LeafPsiElement).rawReplaceWithText("\n${kdocCommentNode.lineIndent()}")
            } else {
                (this as LeafPsiElement).upsertWhitespaceBeforeMe("\n${kdocCommentNode.lineIndent()}")
            }
        }
    }

    private fun isNonIndentLeafOnSameLine(it: ASTNode) =
        it.elementType != WHITE_SPACE || !it.textContains('\n')
}
