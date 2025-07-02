package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * Checks external wrapping of block comments. Wrapping inside the comment is not altered.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class CommentWrappingRule :
    StandardRule(
        id = "comment-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == BLOCK_COMMENT) {
            val beforeBlockComment =
                node
                    .leaves(false)
                    .takeWhile { it.isWhiteSpaceWithoutNewline20 }
                    .firstOrNull()
                    ?: node.firstChildLeafOrSelf20
            val afterBlockComment =
                node
                    .leaves()
                    .takeWhile { it.isWhiteSpaceWithoutNewline20 }
                    .firstOrNull()
                    ?: node.lastChildLeafOrSelf20

            if (!beforeBlockComment.prevLeaf.isWhitespaceWithNewlineOrNull() &&
                !afterBlockComment.nextLeaf.isWhitespaceWithNewlineOrNull()
            ) {
                if (hasNewLineInClosedRange(beforeBlockComment, afterBlockComment)) {
                    // Do not try to fix constructs like below:
                    //    val foo = "foo" /*
                    //    some comment
                    //    */ val bar = "bar"
                    emit(
                        node.startOffset,
                        "A block comment starting on same line as another element and ending on another line before another element is " +
                            "disallowed",
                        false,
                    )
                } else if (beforeBlockComment.prevLeaf?.elementType == LBRACE &&
                    afterBlockComment.nextLeaf?.elementType == RBRACE
                ) {
                    // Allow single line blocks containing a block comment
                    //   val foo = { /* no-op */ }
                    return
                } else {
                    // Do not try to fix constructs like below:
                    //    val foo /* some comment */ = "foo"
                    emit(node.startOffset, "A block comment in between other elements on the same line is disallowed", false)
                }
                return
            }

            beforeBlockComment
                .prevLeaf
                .takeIf { !it.isWhitespaceWithNewlineOrNull() }
                ?.let {
                    if (node.textContains('\n')) {
                        // It can not be autocorrected as it might depend on the situation and code style what is preferred.
                        emit(
                            node.startOffset,
                            "A block comment after any other element on the same line must be separated by a new line",
                            false,
                        )
                    }
                }

            afterBlockComment
                .nextLeaf
                .takeIf { !it.isWhitespaceWithNewlineOrNull() }
                ?.let { nextLeaf ->
                    emit(
                        nextLeaf.startOffset,
                        "A block comment may not be followed by any other element on that same line",
                        true,
                    ).ifAutocorrectAllowed {
                        nextLeaf.upsertWhitespaceBeforeMe(node.indent20)
                    }
                }
        }
    }

    private fun ASTNode?.isWhitespaceWithNewlineOrNull() = this == null || this.isWhiteSpaceWithNewline20
}

public val COMMENT_WRAPPING_RULE_ID: RuleId = CommentWrappingRule().ruleId
