package com.pinterest.ktlint.core

interface LintIssue {
    val line: Int
    val col: Int
    val ruleId: String
    val detail: String
    val canBeAutoCorrected: Boolean

    operator fun component1() = line
    operator fun component2() = col
    operator fun component3() = ruleId
    operator fun component4() = detail

    fun asNonCorrectableIfItIs(): LintIssue
}
