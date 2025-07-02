package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures there are no spaces around unary operators
 *
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/reference/coding-conventions.html#horizontal-whitespace)
 */
@SinceKtlint("0.38", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class SpacingAroundUnaryOperatorRule : StandardRule("unary-op-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == PREFIX_EXPRESSION || node.elementType == POSTFIX_EXPRESSION) {
            val children = node.children20.toList()

            // ignore: var a = + /* comment */ 1
            if (children.any { it.isPartOfComment20 }) {
                return
            }

            children
                .firstOrNull { it.isWhiteSpace20 }
                ?.let { whiteSpace ->
                    emit(whiteSpace.startOffset, "Unexpected spacing in ${node.text.replace("\n", "\\n")}", true)
                        .ifAutocorrectAllowed { whiteSpace.remove() }
                }
        }
    }
}

public val SPACING_AROUND_UNARY_OPERATOR_RULE_ID: RuleId = SpacingAroundUnaryOperatorRule().ruleId
