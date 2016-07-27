package com.github.shyiko.ktlint.rule

data class RuleViolation(val offset: Int, val detail: String, val corrected: Boolean = false)
