package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Lint and format the spacing between the modifiers in and after the last modifier in a modifier list.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class ModifierListSpacingRule : StandardRule("modifier-list-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == MODIFIER_LIST) {
            node
                .children()
                .forEach { visitModifierChild(it, emit) }
            // The whitespace of the last entry of the modifier list is actually placed outside the modifier list
            visitModifierChild(node, emit)
        }
    }

    private fun visitModifierChild(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == WHITE_SPACE) {
            return
        }
        node
            .nextSibling { it.elementType == WHITE_SPACE && it.nextLeaf()?.isPartOfComment20 != true }
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.takeUnless {
                // Regardless of element type, a single white space is always ok and does not need to be checked.
                it.text == " "
            }?.takeUnless {
                // A single newline after a comment is always ok and does not need further checking.
                it.text.trim(' ', '\t').contains('\n') && it.prevLeaf()?.isPartOfComment20 == true
            }?.let { whitespace ->
                if (node.isAnnotationElement() ||
                    (node.elementType == MODIFIER_LIST && node.lastChildNode.isAnnotationElement())
                ) {
                    if (whitespace.text.contains("\n\n")) {
                        emit(whitespace.startOffset, "Single newline expected after annotation", true)
                            .ifAutocorrectAllowed {
                                (whitespace as LeafPsiElement).rawReplaceWithText(
                                    "\n".plus(whitespace.text.substringAfterLast("\n")),
                                )
                            }
                    } else if (!whitespace.text.contains('\n') && whitespace.text != " ") {
                        emit(whitespace.startOffset, "Single whitespace or newline expected after annotation", true)
                            .ifAutocorrectAllowed {
                                (whitespace as LeafPsiElement).rawReplaceWithText(" ")
                            }
                    }
                    Unit
                } else {
                    emit(whitespace.startOffset, "Single whitespace expected after modifier", true)
                        .ifAutocorrectAllowed {
                            (whitespace as LeafPsiElement).rawReplaceWithText(" ")
                        }
                }
            }
    }

    private fun ASTNode?.isAnnotationElement() = this != null && (elementType == ANNOTATION || elementType == ANNOTATION_ENTRY)
}

public val MODIFIER_LIST_SPACING_RULE_ID: RuleId = ModifierListSpacingRule().ruleId
