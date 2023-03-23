package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

public class StringTemplateIndentRule :
    StandardRule(
        id = "string-template-indent",
        visitorModifiers =
            setOf(
                // Wrap all multiline string templates to a separate line
                VisitorModifier.RunAfterRule(MULTILINE_EXPRESSION_WRAPPING_RULE_ID, ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED),
                // The IndentationRule first needs to fix the indentation of the opening quotes of the string template. The indentation inside
                // the string template is relative to the opening quotes. Running this rule before the IndentationRule results in a wrong
                // indentation whenever the indent level of the root of the string template is changed.
                VisitorModifier.RunAfterRule(INDENTATION_RULE_ID, ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED),
            ),
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    private lateinit var nextIndent: String
    private lateinit var wrongIndentChar: String
    private lateinit var wrongIndentDescription: String

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        val indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        if (indentConfig.disabled) {
            return
        }

        nextIndent = indentConfig.indent
        when (indentConfig.indentStyle) {
            IndentConfig.IndentStyle.SPACE -> {
                wrongIndentChar = "\t"
                wrongIndentDescription = "tab"
            }
            IndentConfig.IndentStyle.TAB -> {
                wrongIndentChar = " "
                wrongIndentDescription = "space"
            }
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == STRING_TEMPLATE) {
            val psi = node.psi as KtStringTemplateExpression
            if (psi.isMultiLine() && psi.isFollowedByTrimIndent()) {
                if (node.containsMixedIndentationCharacters()) {
                    // It can not be determined with certainty how mixed indentation characters should be interpreted. The trimIndent
                    // function handles tabs and spaces equally (one tabs equals one space) while the user might expect that the tab size in
                    // the indentation is more than one space.
                    emit(node.startOffset, "Indentation of multiline raw string literal should not contain both tab(s) and space(s)", false)
                    return
                }

                val indent = node.getIndent()
//                indentWhiteSpaceBeforeStringTemplate(node, indent, emit, autoCorrect)
                indentStringTemplate(node, indent, emit, autoCorrect)
            }
        }
    }

    private fun ASTNode.getIndent(): String {
        // When executing this rule, the indentation rule may not have run yet. The functionality to determine the correct indentation level
        // is out of scope of this rule as it is owned by the indentation rule. Therefore, the indentation of the line at which the
        // string template is found, is assumed to be correct and is used to indent all lines of the string template. The indent will be
        // fixed once the indent rule is run as well.
        val firstWhiteSpaceLeafOnSameLine = this.prevLeaf { it.elementType == WHITE_SPACE && it.textContains('\n') }
        return if (this.prevLeaf() == firstWhiteSpaceLeafOnSameLine) {
            // The string template already is on a separate new line. Keep the current indent.
            firstWhiteSpaceLeafOnSameLine.getTextAfterLastNewline()
        } else {
            // String template is forced to a new line. So indent must be increased
            firstWhiteSpaceLeafOnSameLine.getTextAfterLastNewline() + nextIndent
        }
    }

    private fun ASTNode?.getTextAfterLastNewline() =
        this
            ?.text
            ?.split("\n")
            ?.last()
            ?: ""

    private fun ASTNode.containsMixedIndentationCharacters(): Boolean {
        require((this.psi as KtStringTemplateExpression).isMultiLine())
        val nonBlankLines = this.getNonBlankLines()
        val prefixLength =
            nonBlankLines
                .minOfOrNull { it.indentLength() }
                ?: 0
        val distinctIndentCharacters =
            nonBlankLines
                .joinToString(separator = "") {
                    it.splitIndentAt(prefixLength).first
                }
                .toCharArray()
                .distinct()
                .count()
        return distinctIndentCharacters > 1
    }

    private fun ASTNode.getNonBlankLines(): List<String> {
        require(elementType == STRING_TEMPLATE)
        return text
            .split("\n")
            .map { it.removePrefix(RAW_STRING_LITERAL_QUOTES) }
            .map { it.removeSuffix(RAW_STRING_LITERAL_QUOTES) }
            .filterNot { it.isBlank() }
    }

    private fun indentWhiteSpaceBeforeStringTemplate(
        node: ASTNode,
        indent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val prevLeaf = node.prevLeaf()!!
        if (!prevLeaf.textContains('\n')) {
            emit(prevLeaf.startOffset + 1, """Missing newline before raw string literal""", true)
        } else if (prevLeaf.getTextAfterLastNewline() != indent) {
            emit(prevLeaf.startOffset + 1, "Unexpected indent before opening quotes of raw string literal", true)
        } else {
            return
        }

        if (autoCorrect) {
            prevLeaf.upsertWhitespaceAfterMe("\n" + indent)
        }
    }

    private fun indentStringTemplate(
        node: ASTNode,
        newIndent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        // Get the max prefix length that all lines in the multiline string have in common. All whitespace characters are counted as
        // one single position. Note that the way of counting should be in sync with the way this is done by the trimIndent
        // function.
        val prefixLength =
            node.text
                .split("\n")
                .asSequence()
                .filterNot {
                    // For a multiline raw string literal it is very unlikely that text after the opening quotes do contain indentation
                    // characters (it really looks ugly). In such a case this text should be ignored when calculating the common prefix
                    // length as otherwise it is probably set to 0.
                    it.startsWith(RAW_STRING_LITERAL_QUOTES)
                }
                .map {
                    // Indentation before the closing quotes however is relevant to take into account
                    it.removeSuffix(RAW_STRING_LITERAL_QUOTES)
                }
                .filterNot { it.isBlank() }
                .map { it.indentLength() }
                .minOrNull() ?: 0

        checkAndFixNewLineAfterOpeningQuotes(node, newIndent, emit, autoCorrect)

        node
            .children()
            .filterNot { it.elementType == OPEN_QUOTE }
            .filterNot {
                // Blank lines inside the string template should not be indented
                it.text == "\n"
            }
            .forEach {
                if (it.prevLeaf()?.text == "\n") {
                    val (currentIndent, currentContent) =
                        if (it.isIndentBeforeClosingQuote()) {
                            Pair(it.text, "")
                        } else {
                            it.text.splitIndentAt(prefixLength)
                        }
                    if (currentIndent.contains(wrongIndentChar)) {
                        checkAndFixWrongIndentationChar(
                            it = it,
                            oldIndent = currentIndent,
                            newIndent = newIndent,
                            newContent = currentContent,
                            emit = emit,
                            autoCorrect = autoCorrect,
                        )
                    } else if (currentIndent != newIndent) {
                        checkAndFixIndent(
                            node = it,
                            oldIndentLength = currentIndent.length,
                            newIndent = newIndent,
                            newContent = currentContent,
                            autoCorrect = autoCorrect,
                            emit = emit,
                        )
                    }
                }
            }

        checkAndFixNewLineBeforeClosingQuotes(node, newIndent, emit, autoCorrect)
    }

    private fun checkAndFixNewLineAfterOpeningQuotes(
        node: ASTNode,
        indent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val firstNodeAfterOpeningQuotes = node.firstChildNode.nextLeaf() ?: return
        if (firstNodeAfterOpeningQuotes.text.isNotBlank()) {
            emit(
                firstNodeAfterOpeningQuotes.startOffset + firstNodeAfterOpeningQuotes.text.length,
                "Missing newline after the opening quotes of the raw string literal",
                true,
            )
            if (autoCorrect) {
                (firstNodeAfterOpeningQuotes as LeafPsiElement).rawReplaceWithText(
                    "\n" + indent + firstNodeAfterOpeningQuotes.text,
                )
            }
        }
    }

    private fun checkAndFixWrongIndentationChar(
        it: ASTNode,
        oldIndent: String,
        newIndent: String,
        newContent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        emit(
            it.startOffset + oldIndent.indexOf(wrongIndentChar),
            "Unexpected '$wrongIndentDescription' character(s) in margin of multiline string",
            true,
        )
        if (autoCorrect) {
            (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                newIndent + newContent,
            )
        }
    }

    private fun checkAndFixIndent(
        node: ASTNode,
        oldIndentLength: Int,
        newIndent: String,
        newContent: String,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(
            node.startOffset + oldIndentLength,
            "Unexpected indent of raw string literal",
            true,
        )
        if (autoCorrect) {
            if (node.elementType == CLOSING_QUOTE) {
                (node as LeafPsiElement).rawInsertBeforeMe(
                    LeafPsiElement(REGULAR_STRING_PART, newIndent),
                )
            } else {
                (node.firstChildLeafOrSelf() as LeafPsiElement).rawReplaceWithText(
                    newIndent + newContent,
                )
            }
        }
    }

    private fun checkAndFixNewLineBeforeClosingQuotes(
        node: ASTNode,
        indent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val lastNodeBeforeClosingQuotes = node.lastChildNode.prevLeaf() ?: return
        if (lastNodeBeforeClosingQuotes.text.isNotBlank()) {
            emit(
                lastNodeBeforeClosingQuotes.startOffset + lastNodeBeforeClosingQuotes.text.length,
                "Missing newline before the closing quotes of the raw string literal",
                true,
            )
            if (autoCorrect) {
                (lastNodeBeforeClosingQuotes as LeafPsiElement).rawReplaceWithText(
                    lastNodeBeforeClosingQuotes.text + "\n" + indent,
                )
            }
        }
    }

    private fun KtStringTemplateExpression.isMultiLine(): Boolean =
        node
            .children()
            .any { it.elementType == LITERAL_STRING_TEMPLATE_ENTRY && it.text == "\n" }

    private fun KtStringTemplateExpression.isFollowedByTrimIndent() =
        node
            .nextSibling { it.elementType != DOT }
            .let { it?.elementType == CALL_EXPRESSION && it.text == "trimIndent()" }

    private fun String.indentLength() =
        indexOfFirst { !it.isWhitespace() }
            .let { if (it == -1) length else it }

    /**
     * Splits the string at the given index or at the first non-white space character before that index. The returned pair
     * consists of the indentation and the second part contains the remainder. Note that the second part still can start
     * with whitespace characters in case the original strings starts with more white space characters than the requested
     * split index.
     */
    private fun String.splitIndentAt(index: Int): Pair<String, String> {
        require(index >= 0)
        val firstNonWhitespaceIndex =
            indexOfFirst { !it.isWhitespace() }
                .let {
                    if (it == -1) {
                        this.length
                    } else {
                        it
                    }
                }
        val safeIndex = kotlin.math.min(firstNonWhitespaceIndex, index)
        return Pair(
            first = this.take(safeIndex),
            second = this.substring(safeIndex),
        )
    }

    private fun ASTNode.isIndentBeforeClosingQuote() = text.isBlank() && nextCodeSibling()?.elementType == CLOSING_QUOTE

    private companion object {
        const val RAW_STRING_LITERAL_QUOTES = "\"\"\""
    }
}

public val STRING_TEMPLATE_INDENT_RULE_ID: RuleId = StringTemplateIndentRule().ruleId
