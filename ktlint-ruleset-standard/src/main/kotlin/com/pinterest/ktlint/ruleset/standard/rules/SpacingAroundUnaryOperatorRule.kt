package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
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
        if (node.elementType == ElementType.PREFIX_EXPRESSION ||
            node.elementType == ElementType.POSTFIX_EXPRESSION
        ) {
            val children = node.children().toList()

            // ignore: var a = + /* comment */ 1
            if (children.any { it.isPartOfComment() }) {
                return
            }

            val whiteSpace =
                children
                    .firstOrNull { it.isWhiteSpace20 }
                    ?: return
            emit(whiteSpace.startOffset, "Unexpected spacing in ${node.text.replace("\n", "\\n")}", true)
                .ifAutocorrectAllowed {
                    node.removeChild(whiteSpace)
                }
        }
    }
}

public val SPACING_AROUND_UNARY_OPERATOR_RULE_ID: RuleId = SpacingAroundUnaryOperatorRule().ruleId
