package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.core.api.RuleId
import dev.drewhamilton.poko.Poko

/**
 * Lint error found by the [KtLintRuleEngine].
 *
 * [line]: line number (one-based)
 * [col]: column number (one-based)
 * [ruleId]: rule id
 * [detail]: error message
 * [canBeAutoCorrected]: flag indicating whether the error can be corrected by the rule if "format" is run
 */
@Poko
public class LintError(
    public val line: Int,
    public val col: Int,
    public val ruleId: RuleId,
    public val detail: String,
    public val canBeAutoCorrected: Boolean,
)
