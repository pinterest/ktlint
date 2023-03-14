package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.core.api.RuleId

/**
 * Lint error found by the [KtLintRuleEngine].
 *
 * [line]: line number (one-based)
 * [col]: column number (one-based)
 * [ruleId]: rule id
 * [detail]: error message
 * [canBeAutoCorrected]: flag indicating whether the error can be corrected by the rule if "format" is run
 */
public data class LintError(
    val line: Int,
    val col: Int,
    val ruleId: RuleId,
    val detail: String,
    val canBeAutoCorrected: Boolean,
)
