package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
public class ObjectNamingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:object-naming") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == OBJECT_DECLARATION }
            ?.findChildByType(IDENTIFIER)
            ?.takeUnless { it.text.matches(VALID_OBJECT_NAME_REGEXP) }
            ?.let {
                emit(it.startOffset, "Object name should start with an uppercase letter and use camel case", false)
            }
    }

    private companion object {
        val VALID_OBJECT_NAME_REGEXP = Regex("[A-Z][a-zA-Z]*")
    }
}
