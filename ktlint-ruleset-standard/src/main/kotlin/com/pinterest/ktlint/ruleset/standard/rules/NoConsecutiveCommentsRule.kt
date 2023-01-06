package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoConsecutiveCommentsRule :
    Rule("no-consecutive-comments"),
    Rule.Experimental,
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> = listOf(
        CODE_STYLE_PROPERTY,
    )

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        if (editorConfigProperties.getEditorConfigValue(CODE_STYLE_PROPERTY) != ktlint_official) {
            stopTraversalOfAST()
        }
    }

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
