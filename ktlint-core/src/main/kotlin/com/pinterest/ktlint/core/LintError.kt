package com.pinterest.ktlint.core

import java.io.Serializable

/**
 * Lint error.
 *
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param ruleId rule id (prepended with "&lt;ruleSetId&gt;:" in case of non-standard ruleset)
 * @param detail error message
 */
@Deprecated(
    "Deprecated since ktlint 0.49.0. See changelog 0.49. Depending on context replace with " +
        "'com.pinterest.ktlint.rule.engine.api.LintError' or 'com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError'."
)
public data class LintError(
    val line: Int,
    val col: Int,
    val ruleId: String,
    val detail: String,
) : Serializable {
    // fixme:
    // not included in equals/hashCode for backward-compatibility with ktlint < 0.25.0
    // subject to change in 1.0.0
    var canBeAutoCorrected: Boolean = false
        private set

    public constructor(
        line: Int,
        col: Int,
        ruleId: String,
        detail: String,
        canBeAutoCorrected: Boolean,
    ) : this(line, col, ruleId, detail) {
        this.canBeAutoCorrected = canBeAutoCorrected
    }
}
