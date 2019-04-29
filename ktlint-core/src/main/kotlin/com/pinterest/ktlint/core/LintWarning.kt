package com.pinterest.ktlint.core

data class LintWarning(override val line: Int, override val col: Int, override val ruleId: String, override val detail: String) :
    LintIssue {
    // fixme:
    // not included in equals/hashCode for backward-compatibility with ktlint < 0.25.0
    // subject to change in 1.0.0
    override var canBeAutoCorrected: Boolean = false
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

    override fun asNonCorrectableIfItIs(): LintIssue =
        copy(detail = detail + if (!canBeAutoCorrected)" (cannot be auto-corrected)" else "")
}
