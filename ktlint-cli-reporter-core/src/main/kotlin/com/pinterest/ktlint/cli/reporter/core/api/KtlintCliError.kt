package com.pinterest.ktlint.cli.reporter.core.api

import java.io.Serializable

/**
 * Lint error.
 *
 * [line]: line number (one-based)
 * [col]: column number (one-based)
 * [ruleId]: rule id
 * [detail]: error message
 * [status]: status of error
 */
public data class KtlintCliError(
    val line: Int,
    val col: Int,
    val ruleId: String,
    val detail: String,
    val status: Status,
) : Serializable {
    public enum class Status {
        /**
         * An error that was ignored previously by adding it to the baseline file so that it will not be reported again at later invocations
         * of ktlint cli.
         */
        BASELINE_IGNORED,

        /**
         * An error found by ktlint cli linter and which can not be autocorrected.
         */
        LINT_CAN_NOT_BE_AUTOCORRECTED,

        /**
         * An error found by ktlint cli linter and which can be autocorrected.
         */
        LINT_CAN_BE_AUTOCORRECTED,

        /**
         * An error found by ktlint cli linter and which has been autocorrected by the formatter.
         */
        FORMAT_IS_AUTOCORRECTED,

        /**
         * An error found by the kotlin compiler when parsing.
         */
        KOTLIN_PARSE_EXCEPTION,

        /**
         * An internal error which was not handled correctly in ktlint rule engine.
         */
        KTLINT_RULE_ENGINE_EXCEPTION,
    }
}
