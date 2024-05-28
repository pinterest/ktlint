package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.ALLOW_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.NO_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutocorrectHandler {
    fun autocorrectDecision(lintError: LintError): AutocorrectDecision
}

/**
 * Autocorrect no [LintError]s. This handler is used for backward compatability with rules that have not implemented
 * [RuleAutocorrectApproveHandler].
 */
internal data object NoneAutocorrectHandler : AutocorrectHandler {
    override fun autocorrectDecision(lintError: LintError) = NO_AUTOCORRECT
}

/**
 * Autocorrect all [LintError]s. This handler is used for backward compatability with rules that have not implemented
 * [RuleAutocorrectApproveHandler].
 */
internal data object AllAutocorrectHandler : AutocorrectHandler {
    override fun autocorrectDecision(lintError: LintError) = ALLOW_AUTOCORRECT
}

/**
 * The [LintErrorAutocorrectHandler] only works for rules that implement [RuleAutocorrectApproveHandler]. For rules that do not implement
 * that interface, no autocorrections will be made even though the rule is capable doing so. Reason for this is that the API consumer should
 * be able to control whether a specific [LintError] is to be corrected or not.
 */
internal class LintErrorAutocorrectHandler(
    val autocorrectRuleWithoutAutocorrectApproveHandler: Boolean,
    private val callback: (LintError) -> AutocorrectDecision,
) : AutocorrectHandler {
    override fun autocorrectDecision(lintError: LintError) = callback(lintError)
}
