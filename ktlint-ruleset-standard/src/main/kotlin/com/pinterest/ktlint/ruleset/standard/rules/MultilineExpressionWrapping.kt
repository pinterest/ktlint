package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * This rule wraps each multiline expression to a newline.
 */
public class MultilineExpressionWrapping :
    StandardRule(
        id = "multiline-expression-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    private var indentConfig = DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig = IndentConfig(
            indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
            tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
        )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType in CHAINABLE_EXPRESSION &&
            node.treeParent.elementType !in CHAINABLE_EXPRESSION
        ) {
            visitExpression(node, emit, autoCorrect)
        }
        if (node.elementType == BINARY_EXPRESSION &&
            node.treeParent.elementType != BINARY_EXPRESSION
        ) {
            visitExpression(node, emit, autoCorrect)
        }
    }

    private fun visitExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        if (node.containsWhitespaceWithNewline() && node.isValueInAnAssignment()) {
            node
                .prevLeaf { !it.isPartOfComment() }
                .let { prevLeaf ->
                    val expectedIndent =
                        node
                            .treeParent
                            .indent()
                            .plus(indentConfig.indent)
                    if (prevLeaf != null && prevLeaf.text != expectedIndent) {
                        emit(node.startOffset, "A multiline expression should start on a new line", true)
                        if (autoCorrect) {
                            node.upsertWhitespaceBeforeMe(expectedIndent)
                        }
                    }
                }
        }
    }

    private fun ASTNode.containsWhitespaceWithNewline(): Boolean {
        val lastLeaf = lastChildLeafOrSelf()
        return firstChildLeafOrSelf()
            .leavesIncludingSelf()
            .takeWhile { it != lastLeaf }
            .any { it.isWhiteSpaceWithNewline() }
    }

    private fun ASTNode.isValueInAnAssignment() =
        null !=
            prevCodeSibling()
                ?.takeIf { it.elementType == EQ }
                ?.takeUnless {
                    it.closingParenthesisOfFunctionOrNull()
                        ?.prevLeaf()
                        .isWhiteSpaceWithNewline()
                }

    private fun ASTNode.closingParenthesisOfFunctionOrNull() =
        takeIf { treeParent.elementType == FUN }
            ?.prevCodeLeaf()
            ?.takeIf { it.elementType == RPAR }

    private companion object {
        val CHAINABLE_EXPRESSION =
            setOf(
                CALL_EXPRESSION,
                DOT_QUALIFIED_EXPRESSION,
                REFERENCE_EXPRESSION,
                SAFE_ACCESS_EXPRESSION,
            )
    }
}

public val MULTILINE_EXPRESSION_WRAPPING_RULE_ID: RuleId = MultilineExpressionWrapping().ruleId
