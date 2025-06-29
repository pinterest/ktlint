package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACKET
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isPartOfString20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

@SinceKtlint("0.1", STABLE)
public class SpacingAroundCommaRule : StandardRule("comma-spacing") {
    private val rTokenSet = TokenSet.create(RPAR, RBRACKET, GT)

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node is LeafPsiElement && node.textMatches(",") && !node.isPartOfString20) {
            node
                .prevLeaf
                .takeIf { it.isWhiteSpace20 }
                ?.let { prevLeaf ->
                    emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                        .ifAutocorrectAllowed {
                            val isPrecededByComment =
                                prevLeaf
                                    .prevLeaf { !it.isWhiteSpace20 }
                                    ?.isPartOfComment20
                                    ?: false
                            if (isPrecededByComment && prevLeaf.isWhiteSpaceWithNewline20) {
                                // If comma is on new line and preceded by a comment, it should be moved before this comment
                                // https://github.com/pinterest/ktlint/issues/367
                                val previousStatement = node.prevCodeLeaf!!
                                previousStatement.parent?.addChild(node.clone(), previousStatement.nextSibling20)
                                node.nextLeaf.takeIf { it.isWhiteSpace20 }?.remove()
                                node.remove()
                            } else {
                                prevLeaf.remove()
                            }
                        }
                }
            node
                .nextLeaf
                .takeUnless { it.isWhiteSpace20 }
                ?.takeUnless { it.elementType in rTokenSet }
                ?.let {
                    emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                        .ifAutocorrectAllowed { node.upsertWhitespaceAfterMe(" ") }
                }
        }
    }
}

public val SPACING_AROUND_COMMA_RULE_ID: RuleId = SpacingAroundCommaRule().ruleId
