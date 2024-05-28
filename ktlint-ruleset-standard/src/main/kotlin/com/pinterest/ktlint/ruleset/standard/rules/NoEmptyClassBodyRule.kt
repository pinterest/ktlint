package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression

@SinceKtlint("0.9", STABLE)
public class NoEmptyClassBodyRule : StandardRule("no-empty-class-body") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == CLASS_BODY &&
            node.firstChildNode?.let { n ->
                n.elementType == LBRACE &&
                    n.nextLeaf { it.elementType != WHITE_SPACE }?.elementType == RBRACE
            } == true &&
            !node.isPartOf(KtObjectLiteralExpression::class) &&
            node
                .treeParent
                .firstChildNode
                .children()
                .none { it.text == "companion" }
        ) {
            emit(node.startOffset, "Unnecessary block (\"{}\")", true)
                .ifAutocorrectAllowed {
                    val prevNode = node.treePrev
                    if (prevNode.elementType == WHITE_SPACE) {
                        // remove space between declaration and block
                        prevNode.treeParent.removeChild(prevNode)
                    }
                    // remove block
                    node.treeParent.removeChild(node)
                }
        }
    }
}

public val NO_EMPTY_CLASS_BODY_RULE_ID: RuleId = NoEmptyClassBodyRule().ruleId
