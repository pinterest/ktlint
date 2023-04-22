package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * This rule inserts missing newlines inside a property.
 *
 * Whenever a linebreak is inserted, this rules assumes that the parent node it indented correctly. So the indentation
 * is fixed with respect to indentation of the parent. This is just a simple best effort for the case that the
 * indentation rule is not run.
 *
 * This rule has many similarities with the [ParameterWrappingRule] but some subtle differences.
 */
public class PropertyWrappingRule :
    StandardRule(
        id = "property-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var line = 1
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private var maxLineLength: Int = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        line = 1
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == PROPERTY) {
            rearrangeProperty(node, autoCorrect, emit)
        }
    }

    private fun rearrangeProperty(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(node.elementType == PROPERTY)

        val baseIndentLength = node.indent(false).length

        // Find the first node after the indenting whitespace on the same line as the identifier
        val nodeFirstChildLeafOrSelf = node.firstChildLeafOrSelf()
        val fromNode =
            node
                .findChildByType(ElementType.IDENTIFIER)
                ?.leavesIncludingSelf(forward = false)
                ?.firstOrNull { it.prevLeaf().isWhiteSpaceWithNewline() || it == nodeFirstChildLeafOrSelf }
                ?: node

        node
            .findChildByType(COLON)
            ?.let { colon ->
                if (baseIndentLength + fromNode.sumOfTextLengthUntil(colon) > maxLineLength) {
                    fromNode.sumOfTextLengthUntil(colon)
                    requireNewlineAfterLeaf(colon, autoCorrect, emit)
                    return
                }
            }

        node
            .findChildByType(TYPE_REFERENCE)
            ?.let { typeReference ->
                if (baseIndentLength + fromNode.sumOfTextLengthUntil(typeReference) > maxLineLength) {
                    requireNewlineBeforeLeaf(typeReference, autoCorrect, emit)
                    return
                }
            }

        node
            .findChildByType(EQ)
            ?.let { equal ->
                if (baseIndentLength + fromNode.sumOfTextLengthUntil(equal) > maxLineLength) {
                    requireNewlineAfterLeaf(equal, autoCorrect, emit)
                    return
                }
            }

        node
            .findChildByType(CALL_EXPRESSION)
            ?.let { callExpression ->
                if (baseIndentLength + fromNode.sumOfTextLengthUntil(callExpression) > maxLineLength) {
                    requireNewlineBeforeLeaf(callExpression, autoCorrect, emit)
                    return
                }
            }
    }

    private fun ASTNode.sumOfTextLengthUntil(astNode: ASTNode): Int {
        val stopAtLeaf = astNode.lastChildLeafOrSelf()
        return leavesIncludingSelf()
            .takeWhile { !it.isWhiteSpaceWithNewline() && it.prevLeaf() != stopAtLeaf }
            .sumOf { it.textLength }
    }

    private fun requireNewlineBeforeLeaf(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(
            node.startOffset - 1,
            """Missing newline before "${node.text}"""",
            true,
        )
        LOGGER.trace { "$line: " + ((if (!autoCorrect) "would have " else "") + "inserted newline before ${node.text}") }
        if (autoCorrect) {
            node.upsertWhitespaceBeforeMe(node.indent())
        }
    }

    private fun requireNewlineAfterLeaf(
        nodeAfterWhichNewlineIsRequired: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        nodeToFix: ASTNode = nodeAfterWhichNewlineIsRequired,
    ) {
        emit(
            nodeAfterWhichNewlineIsRequired.startOffset + 1,
            """Missing newline after "${nodeAfterWhichNewlineIsRequired.text}"""",
            true,
        )
        LOGGER.trace {
            "$line: " + (if (!autoCorrect) "would have " else "") + "inserted newline after ${nodeAfterWhichNewlineIsRequired.text}"
        }
        if (autoCorrect) {
            nodeToFix.upsertWhitespaceAfterMe(nodeToFix.indent().plus(indentConfig.indent))
        }
    }
}

public val PROPERTY_WRAPPING_RULE_ID: RuleId = PropertyWrappingRule().ruleId
