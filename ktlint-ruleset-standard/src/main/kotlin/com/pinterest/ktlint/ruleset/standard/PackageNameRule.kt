package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.ruleset.standard.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
public class PackageNameRule : Rule("package-name") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == PACKAGE_DIRECTIVE }
            ?.firstChildNode
            ?.nextCodeSibling()
            ?.takeIf { it.elementType == ElementType.DOT_QUALIFIED_EXPRESSION || it.elementType == ElementType.REFERENCE_EXPRESSION }
            ?.let { expression ->
                if (expression.text.contains('_')) {
                    // The Kotlin inspections (preference > Editor > Inspections) for 'Package naming convention' allows
                    // underscores as well. But as this has been forbidden by KtLint since early versions, this is still
                    // prohibited.
                    emit(expression.startOffset, "Package name must not contain underscore", false)
                } else if (!expression.text.matches(VALID_PACKAGE_NAME_REGEXP)) {
                    emit(expression.startOffset, "Package name contains a disallowed character", false)
                }
            }
    }

    private companion object {
        val VALID_PACKAGE_NAME_REGEXP = "[a-z_][a-zA-Z\\d_]*(\\.[a-z_][a-zA-Z\\d_]*)*".regExIgnoringDiacriticsAndStrokesOnLetters()
    }
}
