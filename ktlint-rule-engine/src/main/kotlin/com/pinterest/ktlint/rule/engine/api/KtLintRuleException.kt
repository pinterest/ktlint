package com.pinterest.ktlint.rule.engine.api

/**
 * [KtLintRuleException] is thrown whenever an error occurs during execution of a KtLint [Rule].
 *
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param ruleId rule id (prepended with "&lt;ruleSetId&gt;:" in case of non-standard ruleset)
 * @param message description or error
 */
public class KtLintRuleException(
    public val line: Int,
    public val col: Int,
    public val ruleId: String,
    override val message: String,
    override val cause: Throwable,
) : RuntimeException(message, cause)
