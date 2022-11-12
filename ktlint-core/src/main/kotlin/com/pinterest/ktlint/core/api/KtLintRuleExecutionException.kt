package com.pinterest.ktlint.core.api

/**
 * [KtLintRuleExecutionException] is thrown whenever an error occurs during execution of a KtLint [Rule].
 *
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param ruleId rule id (prepended with "&lt;ruleSetId&gt;:" in case of non-standard ruleset)
 */
public class KtLintRuleExecutionException(
    public val line: Int,
    public val col: Int,
    public val ruleId: String,
    cause: Throwable,
) : RuntimeException(cause)
