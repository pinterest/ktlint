package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
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
public class ValueArgumentCommentRule : StandardRule("value-argument-comment") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isPartOfComment() && node.treeParent.elementType == VALUE_ARGUMENT) {
            // Disallow:
            //     val foo = foo(
            //         bar /* some comment */ = "bar"
            //     )
            // or
            //     val foo = foo(
            //         bar =
            //            // some comment
            //            "bar"
            //     )
            emit(
                node.startOffset,
                "A (block or EOL) comment inside or on same line after a 'value_argument' is not allowed. It may be placed " +
                    "on a separate line above.",
                false,
            )
        }
    }
}

public val VALUE_ARGUMENT_COMMENT_RULE_ID: RuleId = ValueArgumentCommentRule().ruleId
