package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DO_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * https://developer.android.com/kotlin/style-guide#braces
 */
@SinceKtlint("1.0", EXPERIMENTAL)
public class MultiLineLoopRule :
    StandardRule(
        id = "multiline-loop",
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
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != BODY) {
            return
        }

        // Ignore when already wrapped in a block
        if (node.firstChildNode?.elementType == BLOCK) {
            return
        }

        if (!node.treePrev.textContains('\n')) {
            if (!node.treeParent.textContains('\n')) {
                // Allow single line loop statements as long as they are really simple (e.g. do not contain newlines)
                //    for (...) <statement>
                //    while (...) <statement>
                //    do <statement> while (...)
                return
            }

            Unit
        }

        emit(node.firstChildNode.startOffset, "Missing { ... }", true)
        if (autoCorrect) {
            autocorrect(node)
        }
    }

    private fun autocorrect(node: ASTNode) {
        val prevLeaves =
            node
                .leaves(forward = false)
                .takeWhile { it.elementType !in listOf(RPAR, DO_KEYWORD) }
                .toList()
                .reversed()
        val nextLeaves =
            node
                .leaves(forward = true)
                .takeWhile { it.isWhiteSpaceWithoutNewline() || it.isPartOfComment() }
                .toList()
                .dropLastWhile { it.isWhiteSpaceWithoutNewline() }

        prevLeaves
            .firstOrNull()
            .takeIf { it.isWhiteSpace() }
            ?.let {
                (it as LeafPsiElement).rawReplaceWithText(" ")
            }
        KtBlockExpression(null).apply {
            val previousChild = node.firstChildNode
            node.replaceChild(node.firstChildNode, this)
            addChild(LeafPsiElement(LBRACE, "{"))
            addChild(PsiWhiteSpaceImpl(indentConfig.childIndentOf(node)))
            prevLeaves
                .dropWhile { it.isWhiteSpace() }
                .forEach(::addChild)
            addChild(previousChild)
            nextLeaves.forEach(::addChild)
            addChild(PsiWhiteSpaceImpl(node.indent()))
            addChild(LeafPsiElement(RBRACE, "}"))
        }

        // Make sure while starts on same line as newly inserted right brace
        if (node.elementType == BODY) {
            node
                .nextSibling { !it.isPartOfComment() }
                ?.upsertWhitespaceBeforeMe(" ")
        }
    }
}

public val MULTI_LINE_LOOP_RULE_ID: RuleId = MultiLineLoopRule().ruleId
