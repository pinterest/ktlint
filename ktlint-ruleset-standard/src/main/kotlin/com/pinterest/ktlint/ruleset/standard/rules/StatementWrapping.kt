package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
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
            ElementType.FUN, ElementType.TRY, ElementType.CATCH, ElementType.FINALLY, ElementType.CLASS_INITIALIZER -> {
                node.findChildByType(ElementType.BLOCK)
            }

            ElementType.FUNCTION_LITERAL, ElementType.WHEN, ElementType.CLASS_BODY -> {
                node
            }

            ElementType.FOR, ElementType.WHILE, ElementType.DO_WHILE -> {
                node.findChildByType(ElementType.BODY)?.findChildByType(ElementType.BLOCK)
            }

            else -> {
                null
            }
        }?.takeUnless {
            it.isEmptyBlock ||
                (it.elementType == ElementType.FUNCTION_LITERAL && it.isSingleLineBlock)
        }?.let { blockAstNode ->
            val lBraceOrArrow =
                requireNotNull(
                    blockAstNode.findChildByType(ElementType.ARROW) ?: blockAstNode.findChildByType(ElementType.LBRACE),
                )
            val rBrace = requireNotNull(blockAstNode.findChildByType(ElementType.RBRACE))
            val lBraceOrArrowNextCodeElement = requireNotNull(lBraceOrArrow.nextCodeLeaf())
            val rBracePrevCodeElement = requireNotNull(rBrace.prevCodeLeaf())

            if (noNewLineInClosedRange(lBraceOrArrow, lBraceOrArrowNextCodeElement)) {
                emit(lBraceOrArrowNextCodeElement.startOffset, "Expected new line after '{' of function body", true)

                if (autoCorrect) {
                    lBraceOrArrow.upsertWhitespaceAfterMe(blockAstNode.parentAstForIndentation.childIndent)
                }
            }

            if (noNewLineInClosedRange(rBracePrevCodeElement, rBrace)) {
                emit(rBracePrevCodeElement.startOffset, "Expected new line before '}' of function body", true)

                if (autoCorrect) {
                    rBrace.upsertWhitespaceBeforeMe(blockAstNode.treeParent.indent())
                }
            }
        }
    }

    private inline val ASTNode.isSingleLineBlock: Boolean
        get() {
            val lBrace = requireNotNull(this.findChildByType(ElementType.LBRACE))
            val rBrace = requireNotNull(this.findChildByType(ElementType.RBRACE))
            return noNewLineInClosedRange(lBrace, rBrace)
        }

    private inline val ASTNode.isEmptyBlock: Boolean
        get() {
            val lBrace = requireNotNull(this.findChildByType(ElementType.LBRACE))
            return lBrace.nextCodeLeaf()?.elementType == ElementType.RBRACE
        }

    private inline val ASTNode.parentAstForIndentation: ASTNode
        get() =
            if (this.elementType == ElementType.WHEN) {
                this
            } else {
                this.treeParent
            }

    private inline val ASTNode.childIndent: String
        get() {
            return this.indent().plus(indentConfig.indent)
        }
}
