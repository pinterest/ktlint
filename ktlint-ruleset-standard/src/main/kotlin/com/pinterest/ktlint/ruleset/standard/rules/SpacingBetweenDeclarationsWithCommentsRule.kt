package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.TokenSets
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isDeclaration
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

/**
 * @see https://youtrack.jetbrains.com/issue/KT-35088
 */
@SinceKtlint("0.37", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class SpacingBetweenDeclarationsWithCommentsRule : StandardRule("spacing-between-declarations-with-comments") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType in TokenSets.COMMENTS }
            ?.takeIf { it.treeParent.isDeclaration() }
            ?.let { node ->
                val declarationNode = node.treeParent
                val isTailComment = node.startOffset > declarationNode.startOffset
                if (isTailComment || !declarationNode.prevCodeSibling().isDeclaration()) return

                val prevSibling = declarationNode.prevSibling { !it.isWhiteSpace() }
                if (prevSibling != null &&
                    prevSibling.elementType != FILE &&
                    !prevSibling.isPartOfComment()
                ) {
                    val prevNode = declarationNode.prevSibling()
                    if (prevNode.isWhiteSpace() && prevNode.text.count { it == '\n' } < 2) {
                        emit(
                            node.startOffset,
                            "Declarations and declarations with comments should have an empty space between.",
                            true,
                        ).ifAutocorrectAllowed {
                            val indent = node.prevLeaf()?.text?.trim('\n') ?: ""
                            (prevNode as LeafElement).rawReplaceWithText("\n\n$indent")
                        }
                    }
                }
            }
    }
}

public val SPACING_BETWEEN_DECLARATIONS_WITH_COMMENTS_RULE_ID: RuleId = SpacingBetweenDeclarationsWithCommentsRule().ruleId
