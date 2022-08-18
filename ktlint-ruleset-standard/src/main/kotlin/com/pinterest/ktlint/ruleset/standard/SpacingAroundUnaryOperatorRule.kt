package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no spaces around unary operators
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/reference/coding-conventions.html#horizontal-whitespace)
 */
public class SpacingAroundUnaryOperatorRule : Rule("unary-op-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == ElementType.PREFIX_EXPRESSION ||
            node.elementType == ElementType.POSTFIX_EXPRESSION
        ) {
            val children = node.children().toList()

            // ignore: var a = + /* comment */ 1
            if (children.any { it.isPartOfComment() }) return

            val whiteSpace = children.firstOrNull { it.isWhiteSpace() } ?: return
            emit(whiteSpace.startOffset, "Unexpected spacing in ${node.text.replace("\n", "\\n")}", true)

            if (autoCorrect) {
                node.removeChild(whiteSpace)
            }
        }
    }
}
