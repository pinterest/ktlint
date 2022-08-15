package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.IF
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * https://kotlinlang.org/docs/reference/coding-conventions.html#formatting-control-flow-statements
 */
public class MultiLineIfElseRule :
    Rule("multiline-if-else"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            DefaultEditorConfigProperties.indentSizeProperty,
            DefaultEditorConfigProperties.indentStyleProperty,
        )
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        indentConfig = IndentConfig(
            indentStyle = editorConfigProperties.getEditorConfigValue(DefaultEditorConfigProperties.indentStyleProperty),
            tabWidth = editorConfigProperties.getEditorConfigValue(DefaultEditorConfigProperties.indentSizeProperty),
        )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == THEN || node.elementType == ELSE) {
            if (node.firstChildNode?.elementType == BLOCK) {
                return
            }

            if (!node.treePrev.textContains('\n')) {
                if (node.firstChildNode.elementType == IF) {
                    // Allow single line for:
                    // else if (...)
                    return
                }
                if (!node.treeParent.textContains('\n')) {
                    // Allow single line if statements as long as they are really simple (e.g. do not contain newlines)
                    //    if (...) <statement> // no else statement
                    //    if (...) <statement> else <statement>
                    return
                }
            }

            emit(node.firstChildNode.startOffset, "Missing { ... }", true)
            if (autoCorrect) {
                autocorrect(node)
            }
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
            addChild(PsiWhiteSpaceImpl("\n" + node.lineIndent() + indentConfig.indent))
            prevLeaves
                .dropWhile { it.isWhiteSpace() }
                .forEach(::addChild)
            addChild(previousChild)
            nextLeaves.forEach(::addChild)
            addChild(PsiWhiteSpaceImpl("\n" + node.lineIndent()))
            addChild(LeafPsiElement(RBRACE, "}"))
        }

        // Make sure else starts on same line as newly inserted right brace
        if (node.elementType == THEN) {
            node
                .nextSibling { !it.isPartOfComment() }
                ?.let { nextSibling ->
                    if (nextSibling.elementType == ELSE_KEYWORD) {
                        (nextSibling as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
                    }
                    if (nextSibling.elementType == WHITE_SPACE && nextSibling.text != " ") {
                        (nextSibling as LeafPsiElement).rawReplaceWithText(" ")
                    }
                }
        }
    }
}
