package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.nextSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class SpacingBetweenFunctionNameAndOpeningParenthesisRule : Rule("$experimentalRulesetId:spacing-between-function-name-and-opening-parenthesis") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        node
            .takeIf { node.elementType == ElementType.FUN }
            ?.findChildByType(ElementType.IDENTIFIER)
            ?.nextSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whiteSpace ->
                emit(whiteSpace.startOffset, "Unexpected whitespace", true)
                if (autoCorrect) {
                    whiteSpace.treeParent.removeChild(whiteSpace)
                }
            }
    }
}
