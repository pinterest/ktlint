package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#function-names
 */
public class FunctionNamingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:function-naming") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == FUN }
            ?.takeUnless {
                node.isFactoryMethod() ||
                    node.isTestMethod() ||
                    node.hasValidFunctionName()
            }?.let {
                val identifierOffset =
                    node
                        .findChildByType(IDENTIFIER)
                        ?.startOffset
                        ?: 1
                emit(identifierOffset, "Function name should start with a lowercase letter (except factory methods) and use camel case", false)
            }
    }

    private fun ASTNode.isFactoryMethod() =
        findChildByType(TYPE_REFERENCE)?.text == findChildByType(IDENTIFIER)?.text

    private fun ASTNode.isTestMethod() =
        hasBackTickedIdentifier() && hasTestAnnotation()

    private fun ASTNode.hasBackTickedIdentifier() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(BACK_TICKED_FUNCTION_NAME_REGEXP)

    private fun ASTNode.hasTestAnnotation() =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.hasAnnotationWithIdentifierEndingWithTest() }

    private fun ASTNode.hasAnnotationWithIdentifierEndingWithTest() =
        elementType == ANNOTATION_ENTRY &&
            nextLeaf { it.elementType == IDENTIFIER }
                ?.text
                .orEmpty()
                .endsWith("Test")

    private fun ASTNode.hasValidFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_FUNCTION_NAME_REGEXP)

    private companion object {
        val VALID_FUNCTION_NAME_REGEXP = Regex("[a-z][a-zA-Z0-9]*")
        val BACK_TICKED_FUNCTION_NAME_REGEXP = Regex("`.*`")
    }
}
