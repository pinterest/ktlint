package com.pinterest.ktlint.ruleset.standard.internal.trailingcomma

internal enum class TrailingCommaState {
    /**
     * The trailing comma is needed and exists
     */
    EXISTS,

    /**
     * The trailing comma is needed and doesn't exist
     */
    MISSING,

    /**
     * The trailing comma isn't needed and doesn't exist
     */
    NOT_EXISTS,

    /**
     * The trailing comma isn't needed, but exists
     */
    REDUNDANT,
    ;
}
