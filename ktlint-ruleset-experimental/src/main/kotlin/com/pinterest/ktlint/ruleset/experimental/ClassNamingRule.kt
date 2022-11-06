package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
public class ClassNamingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:class-naming") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == CLASS }
            ?.findChildByType(IDENTIFIER)
            ?.takeUnless { it.text.matches(VALID_CLASS_NAME_REGEXP) }
            ?.let {
                emit(it.startOffset, "Class name should start with an uppercase letter and use camel case", false)
            }
    }

    private companion object {
        val VALID_CLASS_NAME_REGEXP = Regex("[A-Z][a-zA-Z0-9]*")
    }
}
