package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoUnitReturnRule : Rule("no-unit-return") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == TYPE_REFERENCE &&
            node.treeParent.elementType == FUN &&
            node.text == "Unit" &&
            node.nextCodeLeaf(skipSubtree = true)?.elementType == LBRACE
        ) {
            emit(node.startOffset, "Unnecessary \"Unit\" return type", true)
            if (autoCorrect) {
                var prevNode = node
                while (prevNode.treePrev.elementType != VALUE_PARAMETER_LIST) {
                    prevNode = prevNode.treePrev
                }
                node.treeParent.removeRange(prevNode, node.treeNext)
            }
        }
    }
}
