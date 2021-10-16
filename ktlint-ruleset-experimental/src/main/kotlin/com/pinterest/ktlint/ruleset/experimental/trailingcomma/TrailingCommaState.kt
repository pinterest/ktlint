package com.pinterest.ktlint.ruleset.experimental.trailingcomma

internal enum class TrailingCommaState {
    /**
     * The trailing comma is needed and exists
     */
    EXISTS,

    /**
     * The trailing comma is needed and doesn't exists
     */
    MISSING,

    /**
     * The trailing comma isn't needed and doesn't exists
     */
    NOT_EXISTS,

    /**
     * The trailing comma isn't needed, but exists
     */
    REDUNDANT,
    ;
}
