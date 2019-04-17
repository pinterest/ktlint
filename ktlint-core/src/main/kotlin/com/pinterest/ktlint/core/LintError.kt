package com.pinterest.ktlint.core

/**
 * Lint error.
 *
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param ruleId rule id (prepended with "&lt;ruleSetId&gt;:" in case of non-standard ruleset)
 * @param detail error message
 */
data class LintError(val line: Int, val col: Int, val ruleId: String, val detail: String) {

    // fixme:
    // not included in equals/hashCode for backward-compatibility with ktlint < 0.25.0
    // subject to change in 1.0.0
    var canBeAutoCorrected: Boolean = false
        private set(value) {
            field = value
        }

    constructor(
        line: Int,
        col: Int,
        ruleId: String,
        detail: String,
        canBeAutoCorrected: Boolean
    ) : this(line, col, ruleId, detail) {
        this.canBeAutoCorrected = canBeAutoCorrected
    }
}
