package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.leaves

public class FunctionReturnTypeSpacingRule :
    StandardRule(
        id = "function-return-type-spacing",
        usesEditorConfigProperties = setOf(MAX_LINE_LENGTH_PROPERTY),
    ) {
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node.firstChildNode
        node
            .takeIf { node.elementType == FUN }
            ?.let { node.findChildByType(COLON) }
            ?.let { colonNode ->
                removeWhiteSpaceBetweenClosingParenthesisAndColon(colonNode, emit, autoCorrect)
                fixWhiteSpaceBetweenColonAndReturnType(colonNode, emit, autoCorrect)
            }
    }

    private fun removeWhiteSpaceBetweenClosingParenthesisAndColon(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == COLON)
        node
            .prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whitespaceBeforeColonNode ->
                emit(whitespaceBeforeColonNode.startOffset, "Unexpected whitespace", true)
                if (autoCorrect) {
                    whitespaceBeforeColonNode.treeParent?.removeChild(whitespaceBeforeColonNode)
                }
            }
    }

    private fun fixWhiteSpaceBetweenColonAndReturnType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == COLON)
        node
            .nextLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceAfterColon ->
                if (whiteSpaceAfterColon?.text != " ") {
                    // In case the whitespace contains a newline than replacing it with a single space results in merging the lines to a
                    // single line. This rule allows this only when the merged lines entirely fit on a single line. Suppose that code below
                    // does not fit on a single line:
                    //    fun foo():
                    //        String = "some-looooooooooooooooong-string"
                    // This rule does *not* attempt to reformat the code as follows:
                    //    fun foo(): String =
                    //        "some-looooooooooooooooong-string"
                    // See FunctionSignatureRule for such reformatting.
                    val newLineLength =
                        node.lengthUntilNewline(false) + // Length of line before but excluding the colon
                            node.textLength + // Length of the colon itself
                            1 + // Length of the fixed whitespace
                            whiteSpaceAfterColon.lengthUntilNewline(true) // Length of the line after but excluding the whitespace
                    if (newLineLength <= maxLineLength) {
                        emit(node.startOffset, "Single space expected between colon and return type", true)
                        if (autoCorrect) {
                            node.upsertWhitespaceAfterMe(" ")
                        }
                    }
                }
            }
    }

    private fun ASTNode?.lengthUntilNewline(forward: Boolean) =
        if (this == null) {
            0
        } else {
            leaves(forward = forward)
                .takeWhile { !it.isWhiteSpaceWithNewline() }
                .sumOf { it.textLength }
        }
}

public val FUNCTION_RETURN_TYPE_SPACING_RULE_ID: RuleId = FunctionReturnTypeSpacingRule().ruleId
