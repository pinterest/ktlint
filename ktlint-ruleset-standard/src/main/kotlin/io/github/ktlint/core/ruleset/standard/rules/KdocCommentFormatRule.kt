package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_END
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_LEADING_ASTERISK
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_SECTION
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHITE_SPACE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.firstChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indentWithoutNewlinePrefix
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Checks that a KDoc comment is formatted consistently:
 * - A single-line KDoc comment starts and ends with the KDoc delimiters, each separated from the content by exactly
 *   one space.
 * - A multi-line KDoc comment starts with a line containing only the KDoc opening delimiter, ends with a line
 *   containing only the KDoc closing delimiter, and every other line starts with a leading asterisk followed by a
 *   single space (or by nothing, for an otherwise empty line), where the leading asterisk is indented one space
 *   more than the opening delimiter.
 *
 * Autocorrect is only applied to whitespace and delimiters. It is never applied when doing so could change the
 * visual indentation, or the meaning, of the actual content of the KDoc comment.
 */
@SinceKtlint("2.0", STABLE)
public class KdocCommentFormatRule :
    StandardRule("kdoc-comment-format"),
    RuleV2.OfficialCodeStyle {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KDOC) {
            return
        }

        if (node.text.startsWith(TRIPLE_ASTERISK_OPENING)) {
            // The extra asterisk becomes part of the actual content of the KDoc. Removing it safely would require
            // rewriting that content, so this is reported but not autocorrected.
            emit(
                node.startOffset + KDOC_START_LENGTH,
                "A KDoc comment should start with '/**' and not with additional asterisks",
                false,
            )
            return
        }

        if (node.textContains('\n')) {
            visitMultiLineKdoc(node, emit)
        } else {
            visitSingleLineKdoc(node, emit)
        }
    }

    /**
     * The KDoc closing delimiter is lexed greedily: any run of asterisks immediately preceding the final slash (with
     * no separating space) is included in the [KDOC_END] token instead of the actual content. For example, when a
     * single-line KDoc comment contains the text `foo` followed directly (no space) by the closing delimiter, and
     * `foo` itself ends with an asterisk, then that asterisk becomes part of [KDOC_END] instead of the content.
     * Those extra leading asterisks are actual content (typically the closing asterisk of markdown emphasis) and
     * are treated as such below.
     */
    private fun visitSingleLineKdoc(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val kdocEnd = node.lastChildLeafOrSelf
        val extraClosingAsterisks = kdocEnd.text.dropLast(2)

        val kdocSection = node.findChildByType(KDOC_SECTION)
        val actualContent = kdocSection?.text.orEmpty() + extraClosingAsterisks
        val trimmedContent = actualContent.trim()
        val expectedContent = if (trimmedContent.isEmpty()) " " else " $trimmedContent "
        if (actualContent == expectedContent) {
            return
        }

        val firstLeaf = kdocSection?.firstChildLeafOrSelf
        val lastLeaf = kdocSection?.lastChildLeafOrSelf

        emit(
            node.startOffset,
            "A single-line KDoc comment should start with '/** ' and end with ' */'",
            firstLeaf != null && lastLeaf != null,
        ).ifAutocorrectAllowed {
            if (firstLeaf != null && lastLeaf != null) {
                if (firstLeaf === lastLeaf) {
                    firstLeaf.replaceTextWith(expectedContent)
                } else {
                    firstLeaf.replaceTextWith(" " + firstLeaf.text.trimStart())
                    lastLeaf.replaceTextWith(lastLeaf.text.trimEnd() + extraClosingAsterisks + " ")
                }
                kdocEnd.replaceTextWith("*/")
            }
        }
    }

    private fun visitMultiLineKdoc(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val leaves = node.collectLeaves()
        if (leaves.size < 2) {
            return
        }

        val afterStart = leaves[1]
        if (!afterStart.isWhiteSpaceWithNewline) {
            emit(
                node.startOffset + KDOC_START_LENGTH,
                "Opening '/**' of a multi-line KDoc comment should not be followed by any other text on the same line",
                false,
            )
        }

        val kdocEnd = leaves.last()
        val beforeEnd = leaves[leaves.size - 2]
        if (!beforeEnd.isWhiteSpaceWithNewline) {
            emit(
                kdocEnd.startOffset,
                "Closing '*/' of a multi-line KDoc comment should not be preceded by any other text on the same line",
                false,
            )
        }

        val expectedContinuationIndent = node.indentWithoutNewlinePrefix + " "
        for (index in 1 until leaves.size - 1) {
            val leaf = leaves[index]
            if (leaf.elementType != WHITE_SPACE || !leaf.isWhiteSpaceWithNewline) {
                continue
            }

            val next = leaves[index + 1]
            when (next.elementType) {
                KDOC_END -> {
                    checkContinuationIndent(
                        leaf,
                        expectedContinuationIndent,
                        "Closing '*/' should align with the leading asterisks of the KDoc comment",
                        emit,
                    )
                }

                KDOC_LEADING_ASTERISK -> {
                    checkContinuationIndent(
                        leaf,
                        expectedContinuationIndent,
                        "Leading asterisk should align with the second asterisk of the KDoc opening '/**'",
                        emit,
                    )
                    checkContentAfterLeadingAsterisk(next, leaves, index + 1, emit)
                }

                else -> {
                    checkMissingLeadingAsterisk(leaf, next, expectedContinuationIndent, emit)
                }
            }
        }
    }

    private fun checkContinuationIndent(
        whitespace: ASTNode,
        expectedIndent: String,
        errorMessage: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val actualIndent = whitespace.text.substringAfterLast('\n')
        if (actualIndent == expectedIndent) {
            return
        }
        val prefixLength = whitespace.text.length - actualIndent.length
        emit(whitespace.startOffset + prefixLength, errorMessage, true)
            .ifAutocorrectAllowed {
                whitespace.replaceTextWith(whitespace.text.substring(0, prefixLength) + expectedIndent)
            }
    }

    private fun checkContentAfterLeadingAsterisk(
        asterisk: ASTNode,
        leaves: List<ASTNode>,
        asteriskIndex: Int,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = leaves.getOrNull(asteriskIndex + 1) ?: return
        if (next.elementType == KDOC_END) {
            return
        }
        if (next.elementType == WHITE_SPACE) {
            if (next.isWhiteSpaceWithNewline) {
                val trailingWhitespace = next.text.substringBefore('\n')
                if (trailingWhitespace.isNotEmpty()) {
                    emit(
                        asterisk.startOffset + asterisk.textLength,
                        "An empty line in a KDoc comment should not contain trailing whitespace after the leading asterisk",
                        true,
                    ).ifAutocorrectAllowed {
                        next.replaceTextWith(next.text.substring(trailingWhitespace.length))
                    }
                }
            }
            return
        }
        if (next.text.isNotEmpty() && next.text.isBlank()) {
            // A blank content leaf directly following the leading asterisk is either the mandatory single space
            // before more content on the same line (e.g. before a KDOC_TAG), or the trailing whitespace of an
            // otherwise empty line. Only the latter is a violation, which is the case when nothing but a line break
            // or the end of the KDoc comment follows.
            val afterNext = leaves.getOrNull(asteriskIndex + 2)
            val isEndOfLine = afterNext == null || afterNext.elementType == KDOC_END || afterNext.isWhiteSpaceWithNewline
            if (isEndOfLine) {
                emit(
                    asterisk.startOffset + asterisk.textLength,
                    "An empty line in a KDoc comment should not contain trailing whitespace after the leading asterisk",
                    true,
                ).ifAutocorrectAllowed {
                    next.replaceTextWith("")
                }
            }
            return
        }
        if (next.text.isNotEmpty() && !next.text.startsWith(" ")) {
            emit(
                asterisk.startOffset + asterisk.textLength,
                "A leading asterisk in a KDoc comment should be followed by a single space",
                true,
            ).ifAutocorrectAllowed {
                next.replaceTextWith(" " + next.text)
            }
        }
    }

    /**
     * Reports (and, when it does not risk changing the visual indentation of the content, autocorrects) a
     * continuation line of a multi-line KDoc comment which does not start with a leading asterisk.
     *
     * Autocorrect is only applied when the actual indentation of the line is at least as wide as the expected
     * indentation, and does not involve tabs. In that case the required ` * ` prefix is inserted while the
     * remaining (extra) indentation is preserved as part of the content, so that the relative visual indentation of
     * the content is not changed. See https://github.com/ktlint/ktlint/issues/2982 for the rationale.
     */
    private fun checkMissingLeadingAsterisk(
        whitespace: ASTNode,
        contentLeaf: ASTNode,
        expectedIndent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val actualIndent = whitespace.text.substringAfterLast('\n')
        val canAutocorrect =
            actualIndent.length >= expectedIndent.length &&
                '\t' !in actualIndent &&
                '\t' !in expectedIndent

        emit(
            whitespace.startOffset + whitespace.textLength,
            "Each line of a multi-line KDoc comment should start with a leading asterisk",
            canAutocorrect,
        ).ifAutocorrectAllowed {
            val prefixLength = whitespace.text.length - actualIndent.length
            val preservedIndent = actualIndent.substring(expectedIndent.length)
            whitespace.replaceTextWith(whitespace.text.substring(0, prefixLength) + expectedIndent)
            (contentLeaf as LeafPsiElement).rawInsertBeforeMe(LeafPsiElement(KDOC_LEADING_ASTERISK, "*"))
            contentLeaf.replaceTextWith(" " + preservedIndent + contentLeaf.text)
        }
    }

    private fun ASTNode.collectLeaves(): List<ASTNode> {
        val endOffset = startOffset + textLength
        val leaves = mutableListOf<ASTNode>()
        var current: ASTNode? = firstChildLeafOrSelf
        while (current != null && current.startOffset < endOffset) {
            leaves.add(current)
            current = current.nextLeaf
        }
        return leaves
    }

    private companion object {
        const val TRIPLE_ASTERISK_OPENING = "/***"
        const val KDOC_START_LENGTH = 3
    }
}

public val KDOC_COMMENT_FORMAT_RULE_ID: RuleId = KdocCommentFormatRule().ruleId
