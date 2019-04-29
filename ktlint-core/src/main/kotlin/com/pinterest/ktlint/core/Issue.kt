package com.pinterest.ktlint.core

data class Issue(
    val offset: Int,
    val errorMessage: String,
    val canBeAutoCorrected: Boolean,
    val type: IssueType = IssueType.ERROR
) {
    fun toLintIssue(line: Int, col: Int, ruleId: String): LintIssue {
        return when (type) {
            IssueType.ERROR -> LintError(line, col, ruleId, errorMessage, canBeAutoCorrected)
            IssueType.WARNING -> LintWarning(line, col, ruleId, errorMessage, canBeAutoCorrected)
        }
    }
}

enum class IssueType {
    ERROR, WARNING
}
