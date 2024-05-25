package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * The AST allows comments to be placed anywhere. This however can lead to code which is unnecessarily hard to read.
 * Disallowing comments at certain positions in the AST makes development of a rule easier as they have not to be taken
 * into account in that rule.
 *
 * As of Ktlint 1.1.0 the logic of this rule is moved to normal rule classes in case a discouraged comment location was added for one
 * specific rule only. Comment locations that are more generic (e.g. used by at least two different rules) are moved to separate rules as
 * this provides more clarity about rule dependencies.
 * The [DiscouragedCommentLocationRule] no longer contains any functionality to avoid duplication and conflicts with other rules. The rule
 * will only be removed in Ktlint 2.0 to avoid breaking changes in 1.x.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
@Deprecated("Marked for removal in ktlint 2.0. See https://github.com/pinterest/ktlint/issues/2367")
public class DiscouragedCommentLocationRule : StandardRule("discouraged-comment-location") {
    // Prevent binary incompatibility not changing the public functions of the class.
    @Suppress("RedundantOverride")
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        super.beforeVisitChildNodes(node, emit)
    }
}

@Deprecated("Marked for removal in ktlint 2.0. See https://github.com/pinterest/ktlint/issues/2367")
public val DISCOURAGED_COMMENT_LOCATION_RULE_ID: RuleId = DiscouragedCommentLocationRule().ruleId
