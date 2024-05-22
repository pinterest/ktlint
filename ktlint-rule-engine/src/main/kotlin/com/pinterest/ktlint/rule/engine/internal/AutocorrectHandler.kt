package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutocorrectHandler {
    fun autoCorrect(lintError: LintError): Boolean
}

/**
 * Autocorrect no [LintError]s. This handler is used for backward compatability with rules that have not implemented
 * [RuleAutocorrectApproveHandler].
 */
internal data object NoneAutocorrectHandler : AutocorrectHandler {
    override fun autoCorrect(lintError: LintError) = false
}

/**
 * Autocorrect all [LintError]s. This handler is used for backward compatability with rules that have not implemented
 * [RuleAutocorrectApproveHandler].
 */
internal data object AllAutocorrectHandler : AutocorrectHandler {
    override fun autoCorrect(lintError: LintError) = true
}

/**
 * The [LintErrorAutocorrectHandler] only works for rules that implement [RuleAutocorrectApproveHandler]. For rules that do not implement
 * that interface, no autocorrections will be made even though the rule is capable doing so. Reason for this is that the API consumer should
 * be able to control whether a specific [LintError] is to be corrected or not.
 */
internal class LintErrorAutocorrectHandler(
    private val callback: (LintError) -> Boolean,
) : AutocorrectHandler {
    override fun autoCorrect(lintError: LintError) = callback(lintError)
}
