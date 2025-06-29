package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * The AST allows comments to be placed anywhere. This however can lead to code which is unnecessarily hard to read. Or, it makes
 * development of rules unnecessarily complex.
 *
 * This rule is based on the DiscouragedCommentLocationRule which is split in several distinct rules. In this way it becomes more clear why
 * another rule depends on this rule.
 */
@SinceKtlint("1.1", STABLE)
public class ValueParameterCommentRule : StandardRule("value-parameter-comment") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isPartOfComment20 && node.parent?.elementType == VALUE_PARAMETER) {
            if (node.elementType == KDOC && node.parent?.firstChildNode == node) {
                // Allow KDoc to be the first element of a value parameter. EOL and block comments directly before a value parameter are
                // a child of the value parameter list, and as of that will never be the first child of the value.
                //     class Foo(
                //         /** some kdoc */
                //         bar = "bar"
                //     )
            } else {
                // Disallow:
                //     class Foo(
                //         bar /* some comment */ = "bar"
                //     )
                // or
                //     class Foo(
                //         bar =
                //            // some comment
                //            "bar"
                //     )
                emit(
                    node.startOffset,
                    "A comment inside or on same line after a 'value_parameter' is not allowed. It may be placed on a separate line " +
                        "above.",
                    false,
                )
            }
        }
    }
}

public val VALUE_PARAMETER_COMMENT_RULE_ID: RuleId = ValueParameterCommentRule().ruleId
