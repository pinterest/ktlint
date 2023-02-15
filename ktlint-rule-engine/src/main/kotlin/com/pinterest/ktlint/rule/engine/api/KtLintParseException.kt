package com.pinterest.ktlint.rule.engine.api

/**
 * [KtLintParseException] is thrown whenever the kotlin code which is to be scanned contains a parsing error. Ensure
 * that the code which is to be scanned, does not contain compilation errors.
 *
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param message message
 */
public class KtLintParseException(
    public val line: Int,
    public val col: Int,
    message: String,
) : RuntimeException("$line:$col $message")
