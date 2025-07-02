package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.KtlintKotlinCompiler
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * If any when condition is using curly braces, then all other when conditions should use braces as well.
 *
 * Braces are helpful for following reasons:
 *   - Bodies of the when-conditions are all aligned at same column position
 *   - Closing braces helps in separation the when-conditions
 */
@SinceKtlint("1.4.0", EXPERIMENTAL)
public class WhenEntryBracing :
    StandardRule(
        id = "when-entry-bracing",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    RuleAutocorrectApproveHandler,
    Rule.OfficialCodeStyle,
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == WHEN) {
            visitWhenStatement(node, emit)
        }
    }

    private fun visitWhenStatement(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.hasAnyWhenEntryWithBlockAfterArrow() || node.hasAnyWhenEntryWithMultilineBody()) {
            addBracesToWhenEntry(node, emitAndApprove)
        }
    }

    private fun ASTNode.hasAnyWhenEntryWithBlockAfterArrow() = children20.any { it.elementType == WHEN_ENTRY && it.hasBlockAfterArrow() }

    private fun ASTNode.hasBlockAfterArrow(): Boolean {
        require(elementType == WHEN_ENTRY)
        return findChildByType(ARROW)
            ?.siblings()
            .orEmpty()
            .any { it.elementType == BLOCK }
    }

    private fun ASTNode.hasAnyWhenEntryWithMultilineBody() = children20.any { it.elementType == WHEN_ENTRY && it.hasMultilineBody() }

    private fun ASTNode.hasMultilineBody(): Boolean {
        require(elementType == WHEN_ENTRY)
        return findChildByType(ARROW)
            ?.siblings()
            .orEmpty()
            .any { it.isWhiteSpaceWithNewline20 }
    }

    private fun addBracesToWhenEntry(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .children20
            .filter { it.elementType == WHEN_ENTRY }
            .filter { !it.hasBlockAfterArrow() }
            .forEach { whenEntry ->
                whenEntry
                    .findChildByType(ARROW)
                    ?.let { arrow ->
                        val nonWhiteSpaceSibling = arrow.nextSibling { !it.isWhiteSpace20 } ?: arrow
                        emitAndApprove(
                            nonWhiteSpaceSibling.startOffset,
                            "Body of when entry should be surrounded by braces if any when entry body is surrounded by braces " +
                                "or has a multiline body",
                            true,
                        ).ifAutocorrectAllowed {
                            arrow
//                                .nextSibling { it.isWhiteSpace() }
                                .surroundWithBraces()
                        }
                    }
            }
    }

    private fun ASTNode.surroundWithBraces() {
        require(elementType == ARROW)
        val whenEntryIndent = indentConfig.parentIndentOf(this).removePrefix("\n")
        val whenEntry =
            "${whenEntryIndent}true -> {" +
                // Replace the whitespaces (possibly this could be a proper indent) at the beginning of the body with an indent. In case
                // the body was already a multiline statement, then the second and following lines should already be properly indented.
                indentConfig.childIndentOf(this) +
                siblings()
                    .dropWhile { it.isWhiteSpace20 }
                    .joinToString(separator = "") { it.text } +
                "\n$whenEntryIndent}"
        val blockExpression = createBlockExpression(whenEntry)
        parent?.removeRange(nextSibling20!!, null)
        prevSibling20!!
            .parent
            ?.run {
                addChild(PsiWhiteSpaceImpl(" "), null)
                addChild(blockExpression!!, null)
            }
    }

    private fun createBlockExpression(whenEntry: String) =
        KtlintKotlinCompiler
            .createASTNodeFromText(
                """
                |when {
                |$whenEntry
                |}
                """.trimMargin(),
            )?.findChildByType(WHEN)
            ?.findChildByType(WHEN_ENTRY)
            ?.findChildByType(BLOCK)
}

public val WHEN_ENTRY_BRACING_RULE_ID: RuleId = WhenEntryBracing().ruleId
