package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
public class PackageNameRule : StandardRule("package-name") {
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

public val PACKAGE_NAME_RULE_ID: RuleId = PackageNameRule().ruleId
