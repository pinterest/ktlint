package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_LEADING_ASTERISK
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_SECTION
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_TEXT
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHITE_SPACE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.firstChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indentWithoutNewlinePrefix
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Checks that a KDoc comment is delimited consistently:
 * - A single-line KDoc comment starts and ends with the KDoc delimiters, each separated from the content by exactly
 *   one space.
 * - A multi-line KDoc comment starts with a line containing only the KDoc opening delimiter, ends with a line
 *   containing only the KDoc closing delimiter, and every other line starts with a leading asterisk followed by a
 *   single space (or by nothing, for an otherwise empty line), where the leading asterisk is indented one space
 *   more than the opening delimiter. The line directly after the opening delimiter, and the line directly before the
 *   closing delimiter, are not allowed to be empty.
 * - An empty (or blank) KDoc comment is not allowed.
 *
 * Autocorrect is only applied to whitespace and delimiters. It is never applied when doing so could change the
 * meaning, or the visual indentation, of the actual content of the KDoc comment. Most notably, a continuation line
 * which is missing its leading asterisk is always reported without autocorrect, since inserting the leading asterisk
 * could change the intended visual indentation of that content.
 */
@SinceKtlint("2.0", EXPERIMENTAL)
public class KdocDelimiterRule :
    StandardRule("kdoc-delimiter"),
    RuleV2.OfficialCodeStyle,
    RuleV2.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KDOC) {
            return
        }

        // Note that a KDoc containing one or more tags (e.g. "@param") is represented as multiple sibling
        // KDOC_SECTION elements (one section per tag), each directly following the previous one. This first section
        // is representative for the KDoc as a whole being empty or not, as a tag can only be present after actual
        // content.
        val kdocSection = node.findChildByType(KDOC_SECTION)
        val hasNoActualContent =
            kdocSection == null ||
                kdocSection.children.all {
                    it.elementType == WHITE_SPACE ||
                        it.elementType == KDOC_LEADING_ASTERISK ||
                        (it.elementType == KDOC_TEXT && it.text.isBlank())
                }
        if (hasNoActualContent) {
            // Nothing but whitespace and/or (empty) leading asterisks between the opening and closing delimiter,
            // e.g. `/***/`, `/** */`, `/**\n */` or `/**\n *\n */`.
            emit(node.startOffset + KDOC_START_LENGTH, "An empty KDoc comment is not allowed", false)
            return
        }

        if (node.textContains('\n')) {
            visitMultiLineKdoc(node, emit)
        } else {
            visitSingleLineKdoc(node, kdocSection, emit)
        }
    }

    /**
     * The KDoc closing delimiter is lexed greedily: any run of asterisks immediately preceding the final slash (with
     * no separating space) is included in the KDOC_END token instead of the actual content. For example the KDoc
     * `/***Foo***/` is parsed into following three tokens:
     *   - KDOC_START = `/**`
     *   - KDOC_SECTION = `*Foo`
     *   - KDOC_END = `***/`
     * Those extra leading asterisks are actual content (typically the closing asterisk of markdown emphasis) and are
     * treated as such below.
     */
    private fun visitSingleLineKdoc(
        node: ASTNode,
        kdocSection: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val kdocEnd = node.lastChildLeafOrSelf
        val extraClosingAsterisks = kdocEnd.text.dropLast(2)

        val firstLeaf = kdocSection.firstChildLeafOrSelf
        val lastLeaf = kdocSection.lastChildLeafOrSelf
        val actualContent = kdocSection.text + extraClosingAsterisks

        val leadingSpaces = actualContent.takeWhile { it == ' ' }.length
        if (leadingSpaces != 1) {
            emit(node.startOffset, "A single-line KDoc comment should start with '/** '", true)
                .ifAutocorrectAllowed {
                    firstLeaf.replaceTextWith(" " + firstLeaf.text.trimStart(' '))
                }
        }

        val trailingSpaces = actualContent.takeLastWhile { it == ' ' }.length
        if (trailingSpaces != 1) {
            // Shrinking the KDOC_END leaf (e.g. from "**/" to "*/") replaces it with a new AST node. The
            // IndentationRule caches a reference to this exact leaf while visiting the KDoc, so replacing it here
            // would leave that rule unable to resolve its own bookkeeping for this KDoc when it is run in the same
            // pass. As this only happens when there are extra closing asterisks to move into the actual content,
            // that specific case is reported without autocorrect to avoid this conflict.
            emit(kdocEnd.startOffset, "A single-line KDoc comment should end with ' */'", extraClosingAsterisks.isEmpty())
                .ifAutocorrectAllowed {
                    lastLeaf.replaceTextWith(lastLeaf.text.trimEnd(' ') + " ")
                }
        }
    }

    /**
     * Whenever a multi-line KDoc contains one or more tags (e.g. "@param"), it is represented in the AST as multiple
     * sibling KDOC_SECTION elements (one per tag) sandwiched directly between the opening and closing whitespace, so
     * this method does not restrict itself to a single KDOC_SECTION. Instead, it walks the leaves of the KDoc
     * (crossing section and tag boundaries transparently) line by line, starting right after the opening delimiter.
     */
    private fun visitMultiLineKdoc(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val kdocStart = node.firstChildLeafOrSelf
        val openingWhitespace = kdocStart.nextSibling
        if (openingWhitespace?.elementType != WHITE_SPACE || !openingWhitespace.isWhiteSpaceWithNewline) {
            emit(
                node.startOffset + KDOC_START_LENGTH,
                "Opening '/**' of a multi-line KDoc comment should not be followed by any other text on the same line",
                false,
            )
            return
        }

        val kdocEnd = node.lastChildLeafOrSelf
        val closingWhitespace = kdocEnd.prevSibling
        if (closingWhitespace?.elementType != WHITE_SPACE || !closingWhitespace.isWhiteSpaceWithNewline) {
            emit(
                kdocEnd.startOffset,
                "Closing '*/' of a multi-line KDoc comment should not be preceded by any other text on the same line",
                false,
            )
            return
        }

        checkNoEmptyLineAfterOpening(openingWhitespace, emit)
        checkNoEmptyLineBeforeClosing(closingWhitespace, emit)

        val expectedContinuationIndent = node.indentWithoutNewlinePrefix + " "
        var lineStartWhitespace: ASTNode = openingWhitespace
        while (lineStartWhitespace !== closingWhitespace) {
            val afterWhitespace = lineStartWhitespace.nextLeaf ?: break

            if (afterWhitespace.elementType == KDOC_LEADING_ASTERISK) {
                checkContinuationIndent(
                    lineStartWhitespace,
                    expectedContinuationIndent,
                    "Leading asterisk should align with the first asterisk of the KDoc opening '/**'",
                    emit,
                )
                checkContentAfterLeadingAsterisk(afterWhitespace, emit)
            } else {
                emit(
                    lineStartWhitespace.startOffset + lineStartWhitespace.textLength,
                    "Each line of a multi-line KDoc comment should start with a leading asterisk",
                    false,
                )
            }

            lineStartWhitespace = afterWhitespace.nextLineBreak() ?: break
        }
        checkContinuationIndent(
            closingWhitespace,
            expectedContinuationIndent,
            "Closing '*/' should align with the leading asterisks of the KDoc comment",
            emit,
        )
    }

    /**
     * Reports (and autocorrects, by removing the empty line) the case in which the line directly after the opening
     * delimiter of a multi-line KDoc comment is empty. The opening whitespace itself is left untouched so that the
     * indentation of the (new) first content line remains correctly separated from the opening delimiter.
     */
    private fun checkNoEmptyLineAfterOpening(
        openingWhitespace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val firstAsterisk = openingWhitespace.nextLeaf ?: return
        val afterFirstAsterisk = firstAsterisk.nextLeaf
        if (firstAsterisk.elementType == KDOC_LEADING_ASTERISK && afterFirstAsterisk.isWhiteSpaceWithNewline) {
            emit(firstAsterisk.startOffset + firstAsterisk.textLength, "No empty line expected after opening delimiter", true)
                .ifAutocorrectAllowed {
                    afterFirstAsterisk?.remove()
                    firstAsterisk.remove()
                }
        }
    }

    /**
     * Reports (and autocorrects, by removing the empty line) the case in which the line directly before the closing
     * delimiter of a multi-line KDoc comment is empty. The closing whitespace itself is left untouched so that it
     * keeps aligning correctly with the leading asterisks of the KDoc comment.
     */
    private fun checkNoEmptyLineBeforeClosing(
        closingWhitespace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val lastAsterisk = closingWhitespace.prevLeaf ?: return
        val beforeLastAsterisk = lastAsterisk.prevLeaf
        if (lastAsterisk.elementType == KDOC_LEADING_ASTERISK && beforeLastAsterisk.isWhiteSpaceWithNewline) {
            emit(lastAsterisk.startOffset + lastAsterisk.textLength, "No empty line expected before closing delimiter", true)
                .ifAutocorrectAllowed {
                    lastAsterisk.remove()
                    beforeLastAsterisk?.remove()
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = asterisk.nextLeaf ?: return
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
        if (next.textLength > 0 && next.text.isBlank()) {
            // A blank content leaf directly following the leading asterisk is either the mandatory single space
            // before more content on the same line (e.g. before a KDOC_TAG), or the trailing whitespace of an
            // otherwise empty line. Only the latter is a violation, which is the case when nothing but a line break
            // or the end of the KDoc comment follows.
            val afterNext = next.nextLeaf
            if (afterNext == null || afterNext.isWhiteSpaceWithNewline) {
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
        if (next.textLength > 0 && !next.text.startsWith(" ")) {
            emit(
                asterisk.startOffset + asterisk.textLength,
                "A leading asterisk in a KDoc comment should be followed by a single space",
                true,
            ).ifAutocorrectAllowed {
                next.upsertWhitespaceBeforeMe(" ")
            }
        }
    }

    /**
     * Starting at this leaf (inclusive), walks forward over leaves (transparently crossing section and tag
     * boundaries) until a whitespace element containing a newline is found. This whitespace element marks the end
     * of the current line of a multi-line KDoc comment.
     */
    private tailrec fun ASTNode.nextLineBreak(): ASTNode? =
        if (elementType == WHITE_SPACE && isWhiteSpaceWithNewline) this else nextLeaf?.nextLineBreak()

    private companion object {
        const val KDOC_START_LENGTH = 3
    }
}

public val KDOC_DELIMITER_RULE_ID: RuleId = KdocDelimiterRule().ruleId
