package com.pinterest.ktlint.core

/**
 * Provides a [Rule] instance. Important: to ensure that a [Rule] can keep internal state and that processing of files
 * is thread-safe, a *new* instance should be provided on each call of [createNewRuleInstance]. A custom [RuleProvider]
 * does not need to take care of clearing internal state as KtLint calls this method any time the [Rule] instance has
 * been used for processing a file and as of that might have an internal state set.
 */
public class RuleProvider(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> Rule,
) {
    public fun createNewRuleInstance(): Rule =
        provider()
}
