package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.nextCodeSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
public class PackageNamingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:package-naming") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == PACKAGE_DIRECTIVE }
            ?.firstChildNode
            ?.nextCodeSibling()
            ?.takeIf { it.elementType == DOT_QUALIFIED_EXPRESSION || it.elementType == REFERENCE_EXPRESSION }
            ?.takeUnless { it.text.matches(VALID_PACKAGE_NAME_REGEXP) }
            ?.let {
                emit(it.startOffset, "Package name should contain lowercase characters only", false)
            }
    }

    private companion object {
        val VALID_PACKAGE_NAME_REGEXP = Regex("[a-z]+(\\.[a-z]+)*")
    }
}
