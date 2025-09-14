package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * https://kotlinlang.org/docs/reference/coding-conventions.html#formatting-control-flow-statements
 */
@SinceKtlint("0.25", STABLE)
public class MultiLineIfElseRule :
    StandardRule(
        id = "multiline-if-else",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != THEN && node.elementType != ELSE) {
            return
        }

        // Ignore when already wrapped in a block
        if (node.firstChildNode?.elementType == BLOCK) {
            return
        }

        if (node.elementType == ELSE &&
            node.firstChildNode?.elementType == BINARY_EXPRESSION &&
            node.firstChildNode.firstChildNode?.elementType == IF
        ) {
            // Allow
            // val foo2 = if (bar1) {
            //     "bar1"
            // } else if (bar2) {
            //     null
            // } else {
            //     null
            // } ?: "something-else"
            return
        }

        if (node.elementType == ELSE &&
            node.firstChildNode?.elementType == DOT_QUALIFIED_EXPRESSION &&
            node.firstChildNode.firstChildNode?.elementType == IF
        ) {
            // Allow
            // val foo = if (bar1) {
            //     "bar1"
            // } else if (bar2) {
            //     "bar2"
            // } else {
            //     "bar3"
            // }.plus("foo")
            return
        }

        if (!node.prevSibling20!!.textContains('\n')) {
            if (node.firstChildNode == null) {
                // if block with an empty 'then' block
                return
            }
            if (node.firstChildNode.elementType == IF) {
                // Allow single line for:
                // else if (...)
                return
            }
            if (node.parent?.textContains('\n') == false) {
                // Allow single line if statements as long as they are really simple (e.g. do not contain newlines)
                //    if (...) <statement> // no else statement
                //    if (...) <statement> else <statement>
                if (node.parent?.parent?.elementType == ELSE) {
                    // Except in case nested if-else-if on single line
                    //    if (...) <statement> else if (..) <statement>
                } else {
                    return
                }
            }
        }

        emit(node.firstChildNode.startOffset, "Missing { ... }", true)
            .ifAutocorrectAllowed {
                autocorrect(node)
            }
    }

    private fun autocorrect(node: ASTNode) {
        val prevLeaves =
            node
                .leaves(forward = false)
                .takeWhile { it.elementType !in listOf(RPAR, ELSE_KEYWORD) }
                .toList()
                .reversed()
        val nextLeaves =
            node
                .leaves(forward = true)
                .takeWhile { it.isWhiteSpaceWithoutNewline20 || it.isPartOfComment20 }
                .toList()
                .dropLastWhile { it.isWhiteSpaceWithoutNewline20 }

        prevLeaves
            .firstOrNull()
            .takeIf { it.isWhiteSpace20 }
            ?.replaceTextWith(" ")
        KtBlockExpression(null).apply {
            val previousChild = node.firstChildNode
            node.replaceChild(node.firstChildNode, this)
            addChild(LeafPsiElement(LBRACE, "{"))
            addChild(PsiWhiteSpaceImpl(indentConfig.childIndentOf(node)))
            prevLeaves
                .dropWhile { it.isWhiteSpace20 }
                .forEach(::addChild)
            addChild(previousChild)
            nextLeaves.forEach(::addChild)
            addChild(PsiWhiteSpaceImpl(node.indent20))
            addChild(LeafPsiElement(RBRACE, "}"))
        }

        // Make sure else starts on same line as newly inserted right brace
        if (node.elementType == THEN) {
            node
                .nextSibling { !it.isPartOfComment20 }
                ?.upsertWhitespaceBeforeMe(" ")
        }
    }
}

public val MULTI_LINE_IF_ELSE_RULE_ID: RuleId = MultiLineIfElseRule().ruleId
