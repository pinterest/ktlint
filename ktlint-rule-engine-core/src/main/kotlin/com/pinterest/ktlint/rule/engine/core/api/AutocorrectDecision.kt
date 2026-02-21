package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.ALLOW_AUTOCORRECT

public enum class AutocorrectDecision {
    /**
     * Autocorrect lint violation if supported by the [RuleBase].
     */
    ALLOW_AUTOCORRECT,

    /**
     * Do not autocorrect lint violation even when this is supported by the [RuleBase].
     */
    NO_AUTOCORRECT,
}

public inline fun <T> AutocorrectDecision.ifAutocorrectAllowed(function: () -> T): T? =
    takeIf { this == ALLOW_AUTOCORRECT }
        ?.let { function() }
