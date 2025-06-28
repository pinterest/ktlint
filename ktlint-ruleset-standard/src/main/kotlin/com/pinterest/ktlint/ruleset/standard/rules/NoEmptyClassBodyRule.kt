package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.9", STABLE)
public class NoEmptyClassBodyRule : StandardRule("no-empty-class-body") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == CLASS_BODY &&
            node.firstChildNode?.let { n ->
                n.elementType == LBRACE &&
                    n.nextLeaf { !it.isWhiteSpace20 }?.elementType == RBRACE
            } == true &&
            !node.isPartOf(ElementType.OBJECT_LITERAL) &&
            node
                .treeParent
                .firstChildNode
                .children20
                .none { it.text == "companion" }
        ) {
            emit(node.startOffset, "Unnecessary block (\"{}\")", true)
                .ifAutocorrectAllowed {
                    // remove space between declaration and block
                    node.prevSibling20.takeIf { it.isWhiteSpace20 }?.remove()
                    // remove block
                    node.remove()
                }
        }
    }
}

public val NO_EMPTY_CLASS_BODY_RULE_ID: RuleId = NoEmptyClassBodyRule().ruleId
