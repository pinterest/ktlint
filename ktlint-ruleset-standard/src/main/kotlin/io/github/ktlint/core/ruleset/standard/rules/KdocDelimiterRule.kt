package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_END
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_LEADING_ASTERISK
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_SECTION
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_START
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_TEXT
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHITE_SPACE
import io.github.ktlint.core.rule.engine.core.api.KtlintKotlinCompiler
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.endOffset
import io.github.ktlint.core.rule.engine.core.api.firstChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indentWithoutNewlinePrefix
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.replaceWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Checks that a KDoc comment is delimited consistently:
 * - A single-line KDoc comment starts and ends with the KDoc delimiters, each separated from the content by exactly one space.
 * - A multi-line KDoc comment starts with a line containing only the KDoc opening delimiter, ends with a line containing only the KDoc
 *   closing delimiter, and every other line starts with a leading asterisk followed by a single space (or by nothing, for an otherwise
 *   empty line), where the leading asterisk is indented one space more than the opening delimiter. The line directly after the opening
 *   delimiter, and the line directly before the closing delimiter, are not allowed to be empty.
 * - An empty (or blank) KDoc comment is not allowed.
 *
 * Autocorrect is only applied to whitespace and delimiters. It is never applied when doing so could change the meaning, or the visual
 * indentation, of the actual content of the KDoc comment. Most notably, a continuation line which is missing its leading asterisk is always
 * reported without autocorrect, since inserting the leading asterisk could change the intended visual indentation of that content.
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
        when (node.elementType) {
            KDOC_START -> {
                if (!node.isPartOfMultiLineKdoc()) {
                    visitKdocStartInSingleLineKdoc(node, emit)
                }
            }

            KDOC_SECTION -> {
                if (node.isEmptyOrBlank()) {
                    // Nothing but whitespace and/or (empty) leading asterisks between the opening and closing delimiter,
                    // e.g. `/***/`, `/** */`, `/**\n */` or `/**\n *\n */`.
                    // Although it could be removed automatically, it is likely that the developer had an intent to write a KDoc.
                    emit(node.treeParent.findChildByType(KDOC_START)!!.endOffset, "An empty KDoc comment is not allowed", false)
                }
            }

            KDOC_LEADING_ASTERISK -> {
                visitLeadingAsterisk(node, emit)
            }

            KDOC_TEXT -> {
                if (node.isPartOfMultiLineKdoc()) {
                    visitKdocTextInMultiLineKdoc(node, emit)
                }
            }

            KDOC_END -> {
                if (node.isPartOfMultiLineKdoc()) {
                    visitKdocEndInMultiLineKdoc(node, emit)
                } else {
                    visitKdocEndInSingleLineKdoc(node, emit)
                }
            }
        }
    }

    private fun ASTNode.isPartOfMultiLineKdoc(): Boolean = parent { it.elementType == KDOC }?.textContains('\n') ?: false

    private fun visitKdocStartInSingleLineKdoc(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == KDOC_START)
        node
            .nextLeaf
            ?.takeUnless { it.elementType == KDOC_END }
            ?.let { leafAfterKdocStart ->
                leafAfterKdocStart
                    .text
                    .takeWhile { it == ' ' }
                    .takeUnless { it.length == 1 }
                    ?.let {
                        emit(leafAfterKdocStart.startOffset, "Expected a single space after '/**' in a single-line KDoc comment", true)
                            .ifAutocorrectAllowed {
                                leafAfterKdocStart.replaceTextWith(" " + leafAfterKdocStart.text.trimStart(' '))
                            }
                    }
            }
    }

    // Checks whether the KDOC_SECTION contains any content except for whitespaces and leading asterisks.
    private fun ASTNode.isEmptyOrBlank(): Boolean {
        require(elementType == KDOC_SECTION)
        return children.all {
            it.elementType == WHITE_SPACE ||
                it.elementType == KDOC_LEADING_ASTERISK ||
                (it.elementType == KDOC_TEXT && it.text.isBlank())
        }
    }

    private fun visitLeadingAsterisk(
        leadingAsterisk: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(leadingAsterisk.elementType == KDOC_LEADING_ASTERISK)

        leadingAsterisk
            .nextLeaf
            .takeIf { it.isWhiteSpaceWithNewline }
            ?.let { whitespaceAfterLeadingAsterisk ->
                val errorMessage =
                    when (leadingAsterisk) {
                        leadingAsterisk.treeParent.firstChildLeafOrSelf -> "No empty line expected after opening delimiter"
                        leadingAsterisk.treeParent.lastChildLeafOrSelf -> "No empty line expected before closing delimiter"
                        else -> null
                    }
                if (errorMessage != null) {
                    emit(leadingAsterisk.startOffset, errorMessage, true)
                        .ifAutocorrectAllowed {
                            whitespaceAfterLeadingAsterisk.remove()
                            leadingAsterisk.remove()
                        }
                    // Remainder of checks (improper indentation, and context after asterisk) are not relevant when the line is (to be)
                    // removed.
                    return
                }
            }

        checkIndentationBeforeLeadingAsterisk(leadingAsterisk, emit)
        checkContentAfterLeadingAsterisk(leadingAsterisk, emit)
    }

    private fun checkIndentationBeforeLeadingAsterisk(
        leadingAsterisk: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        val actualIndent =
            leadingAsterisk
                .prevLeaf { it.isWhiteSpace }
                ?.text
                .orEmpty()
                .substringAfterLast('\n')
        val expectedContinuationIndent = leadingAsterisk.parent { it.elementType == KDOC }?.indentWithoutNewlinePrefix.orEmpty() + " "
        if (actualIndent != expectedContinuationIndent) {
            // Given code sample below, the lines for "option 1" and "option 2" below do start with an actual leading asterisk but
            // are not properly indented. However, the intent of the developer can not be determined here with certainty.
            //    /**
            //     * However, in case below:
            //        * option 1
            //        * option 2
            //     * following applies: some more text.
            //     */
            // A possible intent could be that it should have been formatted as:
            //    /**
            //     * However, in case below:
            //     *   * option 1
            //     *   * option 2
            //     * following applies: some more text.
            //     */
            emit(
                leadingAsterisk.startOffset,
                "Leading asterisk is not properly aligned with the first asterisk of the KDoc opening '/**'",
                false,
            )
        }
    }

    private fun checkContentAfterLeadingAsterisk(
        leadingAsterisk: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(leadingAsterisk.elementType == KDOC_LEADING_ASTERISK)
        leadingAsterisk
            .nextSibling
            // Ignore leading asterisk when it is the last element in the KDoc section. There is a separate message for this case.
            ?.let { nextSibling ->
                when {
                    nextSibling.text.isBlank() -> {
                        // A blank content leaf directly following the leading asterisk is either the mandatory single space before more
                        // content on the same line (e.g. before a KDOC_TAG), or the trailing whitespace of an otherwise empty line. Only
                        // the latter is a violation, which is the case when nothing but a line break or the end of the KDoc comment
                        // follows.
                        if (nextSibling.nextLeaf!!.isWhiteSpaceWithNewline) {
                            emit(
                                leadingAsterisk.startOffset + leadingAsterisk.textLength,
                                "An empty line in a KDoc comment should not contain trailing whitespace after the leading asterisk",
                                true,
                            ).ifAutocorrectAllowed { nextSibling.remove() }
                        }
                    }

                    !nextSibling.text.startsWith(" ") -> {
                        emit(
                            leadingAsterisk.startOffset + leadingAsterisk.textLength,
                            "A leading asterisk in a KDoc comment should be followed by a single space",
                            true,
                        ).ifAutocorrectAllowed { nextSibling.upsertWhitespaceBeforeMe(" ") }
                    }
                }
            }
    }

    private fun visitKdocTextInMultiLineKdoc(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == KDOC_TEXT)
        if (node.isFirstKdocTextInKdocSection()) {
            val kdocStart = node.parent { it.elementType == KDOC }!!.findChildByType(KDOC_START)!!
            emit(
                kdocStart.endOffset,
                "Opening '/**' of a multi-line KDoc comment should not be followed by any other text on the same line",
                false,
            )
        } else {
            val hasKdocLeadingAsteriskOnSameLine =
                node
                    .prevLeaf { it.isWhiteSpaceWithNewline || it.elementType == KDOC_LEADING_ASTERISK }
                    .let { it?.elementType == KDOC_LEADING_ASTERISK }
            if (!hasKdocLeadingAsteriskOnSameLine) {
                emit(
                    node.startOffset,
                    "Each line of a multi-line KDoc comment should start with a leading asterisk",
                    false,
                )
            }
        }
    }

    private fun ASTNode.isFirstKdocTextInKdocSection(): Boolean {
        require(elementType == KDOC_TEXT)
        return this == parent { it.elementType == KDOC_SECTION }!!.firstChildNode
    }

    private fun visitKdocEndInMultiLineKdoc(
        kdocEnd: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(kdocEnd.elementType == KDOC_END)
        val siblingBeforeKdocEnd = kdocEnd.prevSibling!!
        when {
            siblingBeforeKdocEnd.elementType == KDOC_SECTION && siblingBeforeKdocEnd.text.isNotBlank() -> {
                emit(
                    kdocEnd.startOffset,
                    "Closing '*/' of a multi-line KDoc comment should not be preceded by any other text on the same line",
                    false,
                )
            }

            siblingBeforeKdocEnd.isWhiteSpaceWithNewline -> {
                val actualIndent = siblingBeforeKdocEnd.text.substringAfterLast('\n')
                val expectedIndent = kdocEnd.treeParent.indentWithoutNewlinePrefix + " "
                if (actualIndent != expectedIndent) {
                    val prefixLength = siblingBeforeKdocEnd.text.length - actualIndent.length
                    emit(
                        siblingBeforeKdocEnd.startOffset + prefixLength,
                        "Closing '*/' should align with the leading asterisks of the KDoc comment",
                        true,
                    ).ifAutocorrectAllowed {
                        siblingBeforeKdocEnd.replaceTextWith(siblingBeforeKdocEnd.text.substring(0, prefixLength) + expectedIndent)
                    }
                }
            }
        }
    }

    private fun visitKdocEndInSingleLineKdoc(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == KDOC_END)

        // Ignore empty single line KDOC: `/***/`
        if (node.prevLeaf?.elementType == KDOC_START) return

        if (node.text == "*/") {
            node
                .prevSibling!!
                .text
                .takeLastWhile { it == ' ' }
                .takeUnless { it.length == 1 }
                ?.let {
                    emit(node.startOffset, "Expected a single space before '*/' in a single-line KDoc comment", true)
                        .ifAutocorrectAllowed {
                            node
                                .prevSibling!!
                                .lastChildLeafOrSelf
                                .let { it.replaceTextWith(it.text.trimEnd(' ') + " ") }
                        }
                }
        } else {
            // The Kotlin parser adds all asterisks, but no other characters, directly preceding the closing delimiter '*/' to the KDOC_END
            // token. Ktlint considers those to be part of the actual content. For example the KDoc `/***Foo***/` is parsed into following three
            // tokens:
            // - KDOC_START = `/**`
            // - KDOC_SECTION = `*Foo`
            // - KDOC_END = `***/`
            // Those extra leading asterisks are actual content (typically the closing asterisk of markdown emphasis) and are treated as such
            // below.
            val extraClosingAsterisks = node.text.dropLast(2)
            val kdocSectionContent = node.prevSibling!!.text + extraClosingAsterisks
            kdocSectionContent
                .takeLastWhile { it == ' ' }
                .takeUnless { it.length == 1 }
                ?.let {
                    emit(node.startOffset, "Expected a single space before '*/' in a single-line KDoc comment", true)
                        .ifAutocorrectAllowed {
                            node
                                .prevSibling!!
                                .lastChildLeafOrSelf
                                .let { it.replaceTextWith(it.text.trimEnd(' ') + "$extraClosingAsterisks ") }

                            // Simply replacing the text inside the KDOC_END element results in an exception in the IndentationRule. So
                            // recreate a completely new KDOC_END element to replace the existing.
                            val newKdocEnd =
                                KtlintKotlinCompiler
                                    .createASTNodeFromText(
                                        """
                                        /**
                                         */
                                        """.trimIndent(),
                                    )?.findChildByType(KDOC)
                                    ?.findChildByType(KDOC_END)
                                    ?: throw IllegalStateException("Cannot create KdocEnd")
                            node.replaceWith(newKdocEnd)
                        }
                }
        }
    }
}

public val KDOC_DELIMITER_RULE_ID: RuleId = KdocDelimiterRule().ruleId
