package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
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

public class NoSameLineStatementAsOfBlockStartOrEnd
    : StandardRule(
    "no-same-line-statement-as-of-block-start-or-end",
    usesEditorConfigProperties = setOf(
        INDENT_SIZE_PROPERTY,
        INDENT_STYLE_PROPERTY,
    )
) {
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
        if (node.elementType == ElementType.FUN) {
            node.findChildByType(ElementType.BLOCK)?.let { blockAstNode ->
                val lBrace = blockAstNode.findChildByType(ElementType.LBRACE)!!
                val rBrace = blockAstNode.findChildByType(ElementType.RBRACE)!!
                val lBraceNextCodeElement = lBrace.nextCodeLeaf()!!
                val rBracePrevCodeElement = rBrace.prevCodeLeaf()!!
                val text = blockAstNode.text
                val indexOfLBrace = text.indexOf("{")

                assert(indexOfLBrace >= 0)

                val indexOfRBrace = text.lastIndexOf("}")
                assert(indexOfRBrace >= 0)

                if (lBraceNextCodeElement.elementType == ElementType.RBRACE) {
                    return
                }

                if (noNewLineInClosedRange(lBrace, lBraceNextCodeElement)) {
                    emit(
                        lBraceNextCodeElement.startOffset,
                        "Expected new line after `{` of function body", true
                    )

                    if (autoCorrect) {
                        blockAstNode.findChildByType(ElementType.LBRACE)!!
                            .upsertWhitespaceAfterMe(blockAstNode.treeParent.requiredChildIndentation)
                    }
                }

                if (noNewLineInClosedRange(rBracePrevCodeElement, rBrace)) {
                    emit(
                        rBracePrevCodeElement.startOffset,
                        "Expected new line before `}` of function body",
                        true
                    )

                    if (autoCorrect) {
                        blockAstNode.findChildByType(ElementType.RBRACE)!!
                            .upsertWhitespaceBeforeMe(blockAstNode.treeParent.indent())
                    }
                }
            }
        }
    }

    private inline val ASTNode.requiredChildIndentation: String
        get() {
            return this.indent().plus(indentConfig.indent)
        }
}
