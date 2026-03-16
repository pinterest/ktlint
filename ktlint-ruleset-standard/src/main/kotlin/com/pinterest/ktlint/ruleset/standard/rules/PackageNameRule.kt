package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
@SinceKtlint("0.25", STABLE)
public class PackageNameRule : StandardRule("package-name") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { node.elementType == PACKAGE_DIRECTIVE } // Reverted 'it' back to 'node'
            ?.firstChildNode
            ?.nextCodeSibling20
            ?.takeIf { it.elementType == DOT_QUALIFIED_EXPRESSION || it.elementType == REFERENCE_EXPRESSION }
            ?.let { expression ->
                if (expression.text.contains('_')) {
                    emit(expression.startOffset, "Package name must not contain underscore", false)
                } else if (!expression.text.matches(VALID_PACKAGE_NAME_REGEXP)) {
                    emit(expression.startOffset, "Package name contains a disallowed character", false)
                }
            }
    }

    private companion object {
        
        val VALID_PACKAGE_NAME_REGEXP =
            "[a-z][a-zA-Z\\d]*(\\.[a-z][a-zA-Z\\d]*)*"
                .regExIgnoringDiacriticsAndStrokesOnLetters()
    }
}

public val PACKAGE_NAME_RULE_ID: RuleId = PackageNameRule().ruleId