package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
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
            node.isEmptyBlockBody() &&
            !node.isPartOf(OBJECT_LITERAL) &&
            isNotCompanion(node)
        ) {
            emit(node.startOffset, "Unnecessary block (\"{}\")", true)
                .ifAutocorrectAllowed {
                    // remove space between declaration and block
                    node.prevSibling.takeIf { it.isWhiteSpace }?.remove()
                    // remove block
                    node.remove()
                }
        }
    }

    private fun ASTNode.isEmptyBlockBody(): Boolean =
        firstChildNode != null &&
            firstChildNode.elementType == LBRACE &&
            firstChildNode.nextLeaf { !it.isWhiteSpace }?.elementType == RBRACE

    private fun isNotCompanion(node: ASTNode): Boolean =
        node
            .parent
            ?.firstChildNode
            ?.children
            .orEmpty()
            .none { it.text == "companion" }
}

public val NO_EMPTY_CLASS_BODY_RULE_ID: RuleId = NoEmptyClassBodyRule().ruleId
