package com.pinterest.ktlint.rule.engine.core.api

@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public enum class AutocorrectDecision {
    /**
     * Autocorrect lint violation if supported by the [Rule].
     */
    ALLOW_AUTOCORRECT,

    /**
     * Do not autocorrect lint violation even when this is supported by the [Rule].
     */
    NO_AUTOCORRECT,
}

@Deprecated(message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x. Don't use for RuleV2")
public inline fun <T> AutocorrectDecision.ifAutocorrectAllowed(function: () -> T): T? =
    takeIf { this == AutocorrectDecision.ALLOW_AUTOCORRECT }
        ?.let { function() }
