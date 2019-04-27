package com.pinterest.ktlint.core

/**
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param ruleId rule id (prepended with "&lt;ruleSetId&gt;:" in case of non-standard ruleset)
 */
class RuleExecutionException(val line: Int, val col: Int, val ruleId: String, cause: Throwable) :
    RuntimeException(cause)
