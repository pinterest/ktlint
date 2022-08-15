package com.pinterest.ktlint.core

/**
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param message message
 */
@Deprecated("Marked for removal from public API in KtLint 0.48. Please raise an issue if you have a use case to keep it public.")
public class ParseException(
    public val line: Int,
    public val col: Int,
    message: String
) : RuntimeException("$line:$col $message")
