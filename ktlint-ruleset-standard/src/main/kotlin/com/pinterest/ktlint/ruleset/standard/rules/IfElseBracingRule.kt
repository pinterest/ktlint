package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
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
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * All branches of the if-statement should be wrapped between braces if at least one branch is wrapped between braces. Consistent bracing
 * makes statements easier to read.
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class IfElseBracingRule :
    StandardRule(
        id = "if-else-bracing",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.OfficialCodeStyle {
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
        if (node.elementType == IF) {
            visitIfStatement(node, emit)
        }
    }

    private fun visitIfStatement(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val thenNode =
            requireNotNull(node.findChildByType(THEN)) {
                "Can not find THEN branch in IF"
            }
        val elseNode = node.findChildByType(ELSE) ?: return
        val parentIfBracing =
            node
                .treeParent
                .takeIf { it.elementType == ELSE }
                ?.treeParent
                ?.hasBracing()
                ?: false
        val thenBracing = thenNode.hasBracing()
        val elseBracing = elseNode.hasBracing()
        if (parentIfBracing || thenBracing || elseBracing) {
            if (!thenBracing) {
                visitBranchWithoutBraces(thenNode, emit)
            }
            if (!elseBracing) {
                if (elseNode.firstChildNode?.elementType != IF) {
                    visitBranchWithoutBraces(elseNode, emit)
                } else {
                    // Postpone changing the else-if until that node is being processed
                }
            }
        }
    }

    private fun ASTNode?.hasBracing(): Boolean =
        when {
            this == null -> {
                false
            }

            this.elementType == BLOCK -> {
                true
            }

            this.elementType == IF -> {
                findChildByType(THEN).hasBracing() || findChildByType(ELSE).hasBracing()
            }

            else -> {
                this.firstChildNode.hasBracing()
            }
        }

    private fun visitBranchWithoutBraces(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ): Boolean {
        emit(
            node.firstChildNode?.startOffset ?: node.startOffset,
            "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces",
            true,
        ).ifAutocorrectAllowed {
            autocorrect(node)
        }
        return true
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
            ?.let {
                (it as LeafPsiElement).rawReplaceWithText(" ")
            }
        KtBlockExpression(null).apply {
            val previousChild = node.firstChildNode
            if (previousChild == null) {
                node.addChild(this, null)
            } else {
                node.replaceChild(node.firstChildNode, this)
            }
            addChild(LeafPsiElement(LBRACE, "{"))
            if (previousChild != null) {
                addChild(PsiWhiteSpaceImpl(indentConfig.childIndentOf(node)))
            }
            prevLeaves
                .dropWhile { it.isWhiteSpace20 }
                .takeIf { it.isNotEmpty() }
                ?.forEach(::addChild)
            if (previousChild != null) {
                addChild(previousChild)
            }
            nextLeaves.forEach(::addChild)
            if (previousChild != null) {
                addChild(PsiWhiteSpaceImpl(node.indent20))
            }
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

public val IF_ELSE_BRACING_RULE_ID: RuleId = IfElseBracingRule().ruleId
