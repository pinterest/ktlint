package com.github.shyiko.ktlint.core

/**
 * https://github.com/pinterest/ktlint/issues/481
 *
 * Keep the RuleSetProvider in the old package so that we can detect it and throw an error.
 */
@Deprecated("RuleSetProvider has moved to com.pinterest.ktlint.core")
interface RuleSetProvider
