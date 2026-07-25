package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.BLOCK
import io.github.ktlint.core.rule.engine.core.api.ElementType.ELSE
import io.github.ktlint.core.rule.engine.core.api.ElementType.ELSE_KEYWORD
import io.github.ktlint.core.rule.engine.core.api.ElementType.IF
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
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indent
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
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
                .parent
                ?.takeIf { it.elementType == ELSE }
                ?.parent
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
                .takeWhile { it.isWhiteSpaceWithoutNewline || it.isPartOfComment }
                .toList()
                .dropLastWhile { it.isWhiteSpaceWithoutNewline }

        prevLeaves
            .firstOrNull()
            .takeIf { it.isWhiteSpace }
            ?.replaceTextWith(" ")
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
                .dropWhile { it.isWhiteSpace }
                .takeIf { it.isNotEmpty() }
                ?.forEach(::addChild)
            if (previousChild != null) {
                addChild(previousChild)
            }
            nextLeaves.forEach(::addChild)
            if (previousChild != null) {
                addChild(PsiWhiteSpaceImpl(node.indent))
            }
            addChild(LeafPsiElement(RBRACE, "}"))
        }

        // Make sure else starts on same line as newly inserted right brace
        if (node.elementType == THEN) {
            node
                .nextSibling { !it.isPartOfComment }
                ?.upsertWhitespaceBeforeMe(" ")
        }
    }
}

public val IF_ELSE_BRACING_RULE_ID: RuleId = IfElseBracingRule().ruleId
