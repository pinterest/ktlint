package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACKET
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.lineLengthWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Methods chained with operator '.' or '?.' should all fit on a single line. Otherwise, each chained method should be on a separate line.
 *
 * The Kotlin Coding Conventions https://kotlinlang.org/docs/coding-conventions.html#wrap-chained-calls are a more lenient as it defines
 * these rules:
 *  - When wrapping chained calls, put the . character or the ?. operator on the next line, with a single indent:
 *  - The first call in the chain should usually have a line break before it, but it's OK to omit it if the code makes more sense that way.
 *
 * As of that the rule is restricted to ktlint_official code style unless explicitly enabled.
 */
public class ChainMethodContinuation : // TODO: Rename to ChainMethodContinuationRule
    StandardRule(
        id = "chain-method-continuation",
        visitorModifiers =
            setOf(
                RunAfterRule(DISCOURAGED_COMMENT_LOCATION_RULE_ID, ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED)
            ),
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ),
    Rule.Experimental,
    Rule.OfficialCodeStyle
{
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength: Int = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.elementType in chainOperators }
           ?.let { chainOperator ->
                val forceMultiline =
                    chainOperator.isPartOfMultilineExpression() ||
                        chainOperator.lineLengthWithoutNewlinePrefix() > maxLineLength
                if (forceMultiline || chainOperator.isPrecededByComment()) {
                    fixWhiteSpaceBeforeChainOperator(chainOperator, emit, autoCorrect)
                }
                if (forceMultiline) {
                    fixWhiteSpaceAfterChainOperator(chainOperator, emit, autoCorrect)
                }
            }
    }

    private fun ASTNode.isPartOfMultilineExpression(): Boolean {
        require(elementType in chainOperators)
        val rootExpression = rootExpression()
        return hasNewLineInClosedRange(rootExpression, rootExpression.lastChildLeafOrSelf())
    }

    private fun ASTNode.rootExpression(): ASTNode {
        require(elementType in chainOperators)
        var rootExpression = requireNotNull(treeParent)
        while (rootExpression.treeParent?.elementType in chainableElementTypes) {
            rootExpression = rootExpression.treeParent
        }
        return rootExpression
    }

    private fun ASTNode.isPrecededByComment() =
        treeParent.children().any { it.isPartOfComment() }

    private fun fixWhiteSpaceBeforeChainOperator(
        chainOperator: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        chainOperator
            .prevLeaf()
            .takeIf { it.isWhiteSpace() || it?.isPartOfComment() == true }
            .let { whiteSpaceOrComment ->
                if (whiteSpaceOrComment?.isPartOfComment() == true) {
                    // In a chained method containing comments before each method in the chain starts on a newline
                    // Disallow:
                    //     fooBar
                    //         .bar { ... }.foo()
                    emit(chainOperator.startOffset, "Expected newline before '${chainOperator.text}'", true)
                    if (autoCorrect) {
                        chainOperator.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(chainOperator.treeParent))
                    }
                } else if (chainOperator.shouldBeOnSameLineAsClosingElementOfPreviousExpressionInMethodChain()) {
                    // Disallow:
                    //     bar {
                    //         ...
                    //     }.
                    //     foo()
                    // or
                    //     """
                    //     some text
                    //     """
                    //         .trimIndent()
                    if (whiteSpaceOrComment.isWhiteSpaceWithNewline()) {
                        emit(chainOperator.startOffset, "Unexpected newline before '${chainOperator.text}'", true)
                        if (autoCorrect) {
                            whiteSpaceOrComment?.treeParent?.removeChild(whiteSpaceOrComment)
                        }
                    }
                } else {
                    if (whiteSpaceOrComment == null || whiteSpaceOrComment.isWhiteSpaceWithoutNewline()) {
                        // In a multiline chained method each method in the chain starts on a newline
                        // Disallow:
                        //     fooBar
                        //         .bar { ... }.foo()
                        emit(chainOperator.startOffset, "Expected newline before '${chainOperator.text}'", true)
                        if (autoCorrect) {
                            chainOperator.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(chainOperator.treeParent))
                        }
                    }
                }
            }
    }

    private fun ASTNode.shouldBeOnSameLineAsClosingElementOfPreviousExpressionInMethodChain() =
        prevLeaf { !it.isWhiteSpace() }
            ?.takeIf { it.elementType in groupClosingElementType }
            ?.prevLeaf()
            ?.isWhiteSpaceWithNewline()
            ?: false

    private fun fixWhiteSpaceAfterChainOperator(
        chainOperator: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        chainOperator
            .nextLeaf()
            .takeIf { it.isWhiteSpaceWithNewline() }
            ?.let { whiteSpace ->
                emit(whiteSpace.startOffset - 1, "Unexpected newline after '${chainOperator.text}'", true)
                if (autoCorrect) {
                    whiteSpace.treeParent.removeChild(whiteSpace)
                }
            }
    }

    private companion object {
        val chainOperators = TokenSet.create(DOT, SAFE_ACCESS)
        val chainableElementTypes =
            TokenSet.create(
                CALL_EXPRESSION,
                DOT_QUALIFIED_EXPRESSION,
                POSTFIX_EXPRESSION,
                PREFIX_EXPRESSION,
                SAFE_ACCESS_EXPRESSION
            )
        val groupClosingElementType = TokenSet.create(CLOSING_QUOTE, RBRACE, RBRACKET, RPAR)
    }
}

public val CHAIN_METHOD_CONTINUATION_RULE_ID: RuleId = ChainMethodContinuation().ruleId
