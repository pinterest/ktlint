package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.noNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

public class StatementWrapping :
    StandardRule(
        "statement-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            BLOCK ->
                if (node.treeParent.elementType == FUNCTION_LITERAL) {
                    // LBRACE and RBRACE are outside of BLOCK
                    visitBlock(node.treeParent, emit, autoCorrect)
                } else {
                    visitBlock(node, emit, autoCorrect)
                }

            CLASS_BODY, WHEN ->
                visitBlock(node, emit, autoCorrect)
        }
    }

    private fun visitBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .takeUnless {
                // Allow
                //     val foo = {}
                //     val foo = { /* no-op */ }
                //     val foo = { a, b -> println(a + b) }
                it.isBlockWithoutStatements || it.isFunctionLiteralWithSingleStatementOnSingleLine
            }?.findChildByType(LBRACE)
            ?.applyIf(node.isFunctionLiteralWithParameterList) {
                // Allow:
                // val foobar =
                //     foo { bar ->
                //         doSomething()
                //     }
                node.findChildByType(ARROW)
            }?.let { lbraceOrArrow ->
                val nextCodeLeaf = lbraceOrArrow.nextCodeLeaf()
                if (nextCodeLeaf != null && noNewLineInClosedRange(lbraceOrArrow, nextCodeLeaf)) {
                    emit(nextCodeLeaf.startOffset, "Expected new line after '${lbraceOrArrow.text}'", true)
                    if (autoCorrect) {
                        if (node.elementType == WHEN) {
                            lbraceOrArrow.upsertWhitespaceAfterMe(lbraceOrArrow.indentAsChild)
                        } else {
                            lbraceOrArrow.upsertWhitespaceAfterMe(lbraceOrArrow.indentAsSibling)
                        }
                    }
                }

                node
                    .findChildByType(RBRACE)
                    ?.let { rbrace ->
                        val prevCodeLeaf = rbrace.prevCodeLeaf()
                        if (prevCodeLeaf != null && noNewLineInClosedRange(prevCodeLeaf, rbrace)) {
                            emit(rbrace.startOffset, "Expected new line before '}'", true)
                            if (autoCorrect) {
                                rbrace.upsertWhitespaceBeforeMe(rbrace.indentAsParent)
                            }
                        }
                    }
            }
    }

    private inline val ASTNode.isBlockWithoutStatements: Boolean
        get() =
            RBRACE ==
                findChildByType(LBRACE)
                    ?.nextCodeLeaf()
                    ?.elementType

    private inline val ASTNode.isFunctionLiteralWithParameterList: Boolean
        get() =
            elementType == FUNCTION_LITERAL &&
                findChildByType(VALUE_PARAMETER_LIST) != null

    private inline val ASTNode.isFunctionLiteralWithSingleStatementOnSingleLine: Boolean
        get() =
            takeIf { elementType == FUNCTION_LITERAL }
                ?.takeUnless { it.textContains('\n') }
                ?.findChildByType(BLOCK)
                ?.children()
                ?.count { it.elementType != VALUE_PARAMETER_LIST && it.elementType != ARROW }
                ?.let { count -> count <= 1 }
                ?: false

    private inline val ASTNode.indentAsChild: String
        get() = indent().plus(indentConfig.indent)

    private inline val ASTNode.indentAsSibling: String
        get() = treeParent.indent().plus(indentConfig.indent)

    private inline val ASTNode.indentAsParent: String
        get() = treeParent.indent()
}
