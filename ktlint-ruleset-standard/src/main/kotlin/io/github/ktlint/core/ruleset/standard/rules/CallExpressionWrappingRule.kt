package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ARROW
import io.github.ktlint.core.rule.engine.core.api.ElementType.CALL_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import io.github.ktlint.core.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import io.github.ktlint.core.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.LPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.dropTrailingEolComment
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.hasNoMaxLineLengthSuppression
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.leavesOnLine
import io.github.ktlint.core.rule.engine.core.api.lineLength
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.prevSibling
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
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
        if (node.textContains('\n') || node.exceedsMaxLineLength(node.lastChildLeafOrSelf)) {
            node
                .findChildByType(LPAR)
                ?.takeUnless { it.nextSibling.isWhiteSpaceWithNewline }
                ?.let { lpar ->
                    emit(lpar.startOffset, "Expected new line after '('", true)
                        .ifAutocorrectAllowed { lpar.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(lpar)) }
                }
            node
                .findChildByType(RPAR)
                ?.takeUnless { it.prevSibling.isWhiteSpaceWithNewline }
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
        if (node.textContains('\n') || node.exceedsMaxLineLength(node.lastChildLeafOrSelf)) {
            val functionLiteral =
                node
                    .findChildByType(LAMBDA_EXPRESSION)
                    ?.findChildByType(FUNCTION_LITERAL)
            val arrow = functionLiteral?.findChildByType(ARROW)
            if (arrow == null || node.exceedsMaxLineLength(arrow.lastChildLeafOrSelf)) {
                // Arrow not found, or does not fit on the line. Wrap after brace
                functionLiteral
                    ?.findChildByType(LBRACE)
                    .takeUnless { it?.nextSibling.isWhiteSpaceWithNewline }
                    ?.let { lbrace ->
                        emit(lbrace.startOffset, "Expected new line after '{'", true)
                            .ifAutocorrectAllowed { lbrace.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(lbrace)) }
                    }
            } else {
                arrow
                    .takeUnless { it.nextSibling.isWhiteSpaceWithNewline }
                    ?.let {
                        emit(arrow.startOffset + 1, "Expected new line after '->'", true)
                            .ifAutocorrectAllowed { arrow.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(arrow)) }
                    }
            }
            functionLiteral
                ?.findChildByType(RBRACE)
                ?.takeUnless { it.prevLeaf.isWhiteSpaceWithNewline }
                ?.let { rbrace ->
                    emit(rbrace.startOffset, "Expected new line before '}'", true)
                        .ifAutocorrectAllowed {
                            rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(rbrace))
                        }
                }
        }
    }

    private fun ASTNode.exceedsMaxLineLength(stopAtLeaf: ASTNode): Boolean =
        hasNoMaxLineLengthSuppression() &&
            maxLineLength <
            leavesOnLine
                .dropTrailingEolComment()
                .takeWhile { it.prevLeaf != stopAtLeaf }
                .lineLength
}

public val CALL_EXPRESSION_WRAPPING_RULE_ID: RuleId = CallExpressionWrappingRule().ruleId
