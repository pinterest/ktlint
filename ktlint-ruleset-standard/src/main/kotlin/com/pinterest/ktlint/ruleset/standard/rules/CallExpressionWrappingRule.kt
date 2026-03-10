package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.dropTrailingEolComment
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine20
import com.pinterest.ktlint.rule.engine.core.api.lineLength
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("2.0", SinceKtlint.Status.EXPERIMENTAL)
public class CallExpressionWrappingRule :
    StandardRule(
        id = "call-expression-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ),
    RuleV2.Experimental {
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == CALL_EXPRESSION) {
            visitCallExpression(node, emit)
        }
    }

    private fun visitCallExpression(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(REFERENCE_EXPRESSION)
            ?.nextSibling { it.elementType == VALUE_ARGUMENT_LIST }
            ?.let { visitReferenceExpressionValueArgumentList(it, emit) }
        node
            .findChildByType(LAMBDA_ARGUMENT)
            ?.let { visitLambdaArgument(it, emit) }
    }

    private fun visitReferenceExpressionValueArgumentList(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == VALUE_ARGUMENT_LIST)
        if (node.textContains('\n') || node.exceedsMaxLineLength(node.lastChildLeafOrSelf20)) {
            node
                .findChildByType(LPAR)
                ?.takeUnless { it.nextSibling20.isWhiteSpaceWithNewline20 }
                ?.let { lpar ->
                    emit(lpar.startOffset, "Expected new line after '('", true)
                        .ifAutocorrectAllowed { lpar.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(lpar)) }
                }
            node
                .findChildByType(RPAR)
                ?.takeUnless { it.prevSibling20.isWhiteSpaceWithNewline20 }
                ?.let { rbrace ->
                    emit(rbrace.startOffset, "Expected new line before ')'", true)
                        .ifAutocorrectAllowed {
                            rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(rbrace))
                        }
                }
        }
    }

    private fun visitLambdaArgument(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == LAMBDA_ARGUMENT)
        if (node.textContains('\n') || node.exceedsMaxLineLength(node.lastChildLeafOrSelf20)) {
            val functionLiteral =
                node
                    .findChildByType(LAMBDA_EXPRESSION)
                    ?.findChildByType(FUNCTION_LITERAL)
            val arrow = functionLiteral?.findChildByType(ARROW)
            if (arrow == null || node.exceedsMaxLineLength(arrow.lastChildLeafOrSelf20)) {
                // Arrow not found, or does not fit on the line. Wrap after brace
                functionLiteral
                    ?.findChildByType(LBRACE)
                    .takeUnless { it?.nextSibling20.isWhiteSpaceWithNewline20 }
                    ?.let { lbrace ->
                        emit(lbrace.startOffset, "Expected new line after '{'", true)
                            .ifAutocorrectAllowed { lbrace.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(lbrace)) }
                    }
            } else {
                arrow
                    .takeUnless { it.nextSibling20.isWhiteSpaceWithNewline20 }
                    ?.let {
                        emit(arrow.startOffset + 1, "Expected new line after '->'", true)
                            .ifAutocorrectAllowed { arrow.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(arrow)) }
                    }
            }
            functionLiteral
                ?.findChildByType(RBRACE)
                ?.takeUnless { it.prevSibling20.isWhiteSpaceWithNewline20 }
                ?.let { rbrace ->
                    emit(rbrace.startOffset, "Expected new line before '}'", true)
                        .ifAutocorrectAllowed {
                            rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(rbrace))
                        }
                }
        }
    }

    private fun ASTNode.exceedsMaxLineLength(stopAtLeaf: ASTNode): Boolean =
        maxLineLength <
            leavesOnLine20
                .dropTrailingEolComment()
                .takeWhile { it.prevLeaf != stopAtLeaf }
                .lineLength
}

public val CALL_EXPRESSION_WRAPPING_RULE_ID: RuleId = CallExpressionWrappingRule().ruleId
