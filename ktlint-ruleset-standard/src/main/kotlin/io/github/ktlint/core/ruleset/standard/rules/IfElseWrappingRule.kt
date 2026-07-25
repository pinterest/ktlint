package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.BLOCK
import io.github.ktlint.core.rule.engine.core.api.ElementType.ELSE
import io.github.ktlint.core.rule.engine.core.api.ElementType.ELSE_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.IF
import io.github.ktlint.core.rule.engine.core.api.ElementType.IF_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.LBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RBRACE
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.THEN
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.betweenCodeSiblings
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indent
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.nextCodeLeaf
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevCodeLeaf
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

/**
 * Enforce that single line if statements are kept simple. A single line if statement is allowed only when it has at most one else branch.
 * Also, the branches of such an if statement may not be wrapped in a block.
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class IfElseWrappingRule :
    StandardRule(
        id = "if-else-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    RuleV2.OfficialCodeStyle {
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when {
            node.elementType == IF -> visitIf(node, emit)
            node.isPartOfComment && node.parent?.elementType == IF -> visitComment(node, emit)
        }
    }

    private fun visitIf(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val outerIf = node.outerIf()
        val multilineIf = outerIf.textContains('\n')
        val nestedIf = outerIf.isNestedIf()
        with(node) {
            findChildByType(THEN)
                ?.let { visitElement(it, emit, multilineIf, nestedIf) }
            findChildByType(ELSE_KEYWORD)
                ?.let { visitElement(it, emit, multilineIf, nestedIf) }
            findChildByType(ELSE)
                ?.let { visitElement(it, emit, multilineIf, nestedIf) }
        }
    }

    private fun visitElement(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        multilineIf: Boolean,
        nestedIf: Boolean,
    ) {
        if (!multilineIf) {
            visitBranchSingleLineIf(node, emit)
        }
        if (multilineIf || nestedIf) {
            visitBranch(node, emit, multilineIf)
        }
    }

    private fun visitBranchSingleLineIf(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(BLOCK)
            ?.let {
                // The then or else block is part of single line if-statement. To enforce that such statements are readable and
                // simple, blocks are forbidden entirely in single line if-statements.
                emit(
                    node.startOffset,
                    "A single line if-statement should be kept simple. The '${node.elementType}' may not be wrapped in a block.",
                    false,
                )
            }
    }

    private fun visitBranch(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        multilineIf: Boolean,
    ) {
        if (multilineIf) {
            if (node.isElseIf()) {
                // Allow "else if" on single line
                return
            }
            if (node.elementType == ELSE_KEYWORD && node.nextCodeLeaf?.elementType == LBRACE) {
                // Allow "else {" on single line
                return
            }
        } else {
            // Outer if statement is a single line statement
            if (node.elementType == ELSE && node.nextCodeLeaf?.elementType == IF_KEYWORD) {
                // Ignore "else if" as it is reported via another message
                return
            }
        }

        with(node.findFirstNodeInBlockToBeIndented() ?: node) {
            val expectedIndent =
                if (nextSibling?.elementType == RBRACE) {
                    node.indent
                } else {
                    indentConfig.siblingIndentOf(node)
                }

            applyIf(elementType == THEN || elementType == ELSE || elementType == ELSE_KEYWORD) { prevLeaf!! }
                .takeUnless { it.isWhiteSpaceWithNewline }
                ?.let {
                    // Expected a newline with indent. Leave it up to the IndentationRule to determine exact indent
                    emit(startOffset, "Expected a newline", true)
                        .ifAutocorrectAllowed {
                            upsertWhitespaceBeforeMe(expectedIndent)
                        }
                }
        }
    }

    private fun ASTNode.isElseIf() =
        when (elementType) {
            IF -> prevCodeLeaf?.elementType == ELSE_KEYWORD
            ELSE, ELSE_KEYWORD -> nextCodeLeaf?.elementType == IF_KEYWORD
            else -> false
        }

    private fun ASTNode.findFirstNodeInBlockToBeIndented(): ASTNode? =
        findChildByType(BLOCK)
            ?.children
            ?.first {
                it.elementType != LBRACE &&
                    !it.isWhitespaceBeforeComment() &&
                    !it.isPartOfComment
            }

    private fun ASTNode.isWhitespaceBeforeComment() = isWhiteSpaceWithoutNewline && nextLeaf?.isPartOfComment == true

    private fun ASTNode.outerIf(): ASTNode {
        require(this.elementType == IF)
        return parents()
            .takeWhile { it.elementType in IF_THEN_ELSE_ELEMENT_TYPES }
            .lastOrNull()
            ?: this
    }

    private fun ASTNode.isNestedIf(): Boolean {
        require(elementType == IF)
        return findChildByType(THEN)?.firstChildNode?.elementType == IF ||
            findChildByType(ELSE)?.firstChildNode?.elementType == IF
    }

    private fun visitComment(
        comment: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(comment.isPartOfComment)
        if (comment.betweenCodeSiblings(RPAR, THEN) ||
            comment.betweenCodeSiblings(THEN, ELSE_KEYWORD) ||
            comment.betweenCodeSiblings(ELSE_KEYWORD, ELSE)
        ) {
            emit(comment.startOffset, "No comment expected at this location", false)
        }
    }

    private companion object {
        val IF_THEN_ELSE_ELEMENT_TYPES =
            listOf(
                IF,
                THEN,
                ELSE,
            )
    }
}

public val IF_ELSE_WRAPPING_RULE_ID: RuleId = IfElseWrappingRule().ruleId
