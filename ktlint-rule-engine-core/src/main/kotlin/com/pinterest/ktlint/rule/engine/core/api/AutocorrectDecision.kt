package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.ALLOW_AUTOCORRECT

public enum class AutocorrectDecision {
    /**
     * Autocorrect the [LintError] if supported by the rule.
     */
    ALLOW_AUTOCORRECT,

    /**
     * Do not autocorrect the [LintError] even when this is supported by the rule.
     */
    NO_AUTOCORRECT,
}

public inline fun <T> AutocorrectDecision.ifAutocorrectAllowed(function: () -> T): T? =
    takeIf { this == ALLOW_AUTOCORRECT }
        ?.let { function() }
