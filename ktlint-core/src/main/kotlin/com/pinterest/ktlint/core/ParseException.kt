package com.pinterest.ktlint.core

/**
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param message message
 */
public class ParseException(public val line: Int, public val col: Int, message: String) : RuntimeException("$line:$col $message")
