package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.LT
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class SpacingAroundAngleBracketsRule : Rule("angle-brackets-rule") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == TYPE_ARGUMENT_LIST) {
            val openingBracketChild = node.nextLeaf { it.elementType == LT }
            if (openingBracketChild != null) {
                // Check for rogue spacing before an opening bracket:  Map <String, Int>
                val beforeAngleBracket = openingBracketChild.prevLeaf()
                if (beforeAngleBracket != null && beforeAngleBracket.elementType == WHITE_SPACE) {
                    emit(beforeAngleBracket.startOffset, "Unexpected spacing before \"<\"", true)
                    if (autoCorrect) {
                        beforeAngleBracket.treeParent.removeChild(beforeAngleBracket)
                    }
                }

                // Check for rogue spacing after an opening bracket:  Map< String, Int>
                val afterAngleBracket = openingBracketChild.nextLeaf()
                if (afterAngleBracket != null && afterAngleBracket.elementType == WHITE_SPACE) {
                    emit(afterAngleBracket.startOffset, "Unexpected spacing after \"<\"", true)
                    if (autoCorrect) {
                        afterAngleBracket.treeParent.removeChild(afterAngleBracket)
                    }
                }
            }

            // Check for rogue spacing before a closing bracket:  Map<String, Int >
            val closingBracket = node.nextLeaf { it.elementType == GT }
            if (closingBracket != null) {
                val beforeBracket = closingBracket.prevLeaf()
                if (beforeBracket != null && beforeBracket.elementType == WHITE_SPACE) {
                    emit(beforeBracket.startOffset, "Unexpected spacing before \">\"", true)
                    if (autoCorrect) {
                        beforeBracket.treeParent.removeChild(beforeBracket)
                    }
                }
            }
        }
    }
}
