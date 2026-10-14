package io.github.ktlint.core.rule.engine.core.api

public enum class AutocorrectDecision {
    /**
     * Autocorrect lint violation if supported by the [RuleV2].
     */
    ALLOW_AUTOCORRECT,

    /**
     * Do not autocorrect lint violation even when this is supported by the [RuleV2].
     */
    NO_AUTOCORRECT,
}
