package com.pinterest.ktlint.core

/**
 * @param line line number (one-based)
 * @param col column number (one-based)
 * @param message message
 */
class ParseException(val line: Int, val col: Int, message: String) : RuntimeException("$line:$col $message")
