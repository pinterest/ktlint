package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.ruleset.experimental.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 *
 * The Kotlin convention does not allow explicitly to use backticked class name but it makes sense to allow this as
 * well as it is more consistent with name of test functions.
 */
public class ClassNamingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:class-naming") {
    private var allowBacktickedClassName = false

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (!allowBacktickedClassName && node.elementType == ElementType.IMPORT_DIRECTIVE) {
            node
                .findChildByType(ElementType.DOT_QUALIFIED_EXPRESSION)
                ?.text
                ?.takeIf { it.startsWith("org.junit.jupiter.api") }
                ?.let {
                    // Assume that each file that imports a Junit Jupiter Api class is a test class
                    allowBacktickedClassName = true
                }
        }

        node
            .takeIf { node.elementType == CLASS || node.elementType == OBJECT_DECLARATION }
            ?.findChildByType(IDENTIFIER)
            ?.takeUnless { it.isValidFunctionName() || it.isTestClass() }
            ?.let {
                emit(it.startOffset, "Class or object name should start with an uppercase letter and use camel case", false)
            }
    }

    private fun ASTNode.isValidFunctionName() =
        text.matches(VALID_CLASS_NAME_REGEXP)

    private fun ASTNode.isTestClass() =
        allowBacktickedClassName && hasBackTickedIdentifier()

    private fun ASTNode.hasBackTickedIdentifier() =
        text.matches(BACK_TICKED_FUNCTION_NAME_REGEXP)

    private companion object {
        val VALID_CLASS_NAME_REGEXP = "[A-Z][A-Za-z\\d]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        val BACK_TICKED_FUNCTION_NAME_REGEXP = Regex("`.*`")
    }
}
