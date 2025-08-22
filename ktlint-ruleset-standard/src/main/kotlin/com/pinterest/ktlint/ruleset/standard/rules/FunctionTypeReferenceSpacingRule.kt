package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class FunctionTypeReferenceSpacingRule : StandardRule("function-type-reference-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == FUN) {
            node
                .findFunctionReceiverTypeReference()
                ?.let { typeReference ->
                    typeReference
                        .firstChildNode
                        .takeIf { it.elementType == NULLABLE_TYPE }
                        ?.let { nullableTypeElement ->
                            visitNodesUntilStartOfValueParameterList(nullableTypeElement.firstChildNode, emit)
                        }

                    if (typeReference.elementType != NULLABLE_TYPE) {
                        visitNodesUntilStartOfValueParameterList(typeReference, emit)
                    }
                }
        }
    }

    private fun ASTNode.findFunctionReceiverTypeReference(): ASTNode? {
        require(elementType == FUN)
        var currentNode: ASTNode? = firstChildNode
        while (currentNode != null && currentNode.elementType != VALUE_PARAMETER_LIST) {
            if (currentNode.elementType == TYPE_REFERENCE) {
                return currentNode
            }
            currentNode = currentNode.nextSibling20
        }
        return null
    }

    private fun visitNodesUntilStartOfValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        var currentNode: ASTNode? = node
        while (currentNode != null && currentNode.elementType != VALUE_PARAMETER_LIST) {
            val nextNode = currentNode.nextSibling20
            removeIfNonEmptyWhiteSpace(currentNode, emit)
            currentNode = nextNode
        }
    }

    private fun removeIfNonEmptyWhiteSpace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpace20 && node.text.isNotEmpty()) {
            emit(node.startOffset, "Unexpected whitespace", true)
                .ifAutocorrectAllowed { node.remove() }
        }
    }
}

public val FUNCTION_TYPE_REFERENCE_SPACING_RULE_ID: RuleId = FunctionTypeReferenceSpacingRule().ruleId
