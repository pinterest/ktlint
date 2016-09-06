package com.github.shyiko.ktlint.core

/**
 * Lint error.
 *
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param ruleId rule id (prepended with "&lt;ruleSetId&gt;:" in case of non-standard ruleset)
 * @param detail error message
 */
data class LintError(val line: Int, val col: Int, val ruleId: String, val detail: String)
