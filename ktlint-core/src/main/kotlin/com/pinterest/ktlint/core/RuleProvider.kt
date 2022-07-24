package com.pinterest.ktlint.core

/**
 * Provides a [Rule] instance. Important: to ensure that a [Rule] can keep internal state and that processing of files
 * is thread-safe, a *new* instance should be provided on each call of [createNewRuleInstance]. A custom
 * [RuleSetProvider] does not need to take care of clearing internal state as KtLint calls this method any time the
 * [Rule] instance has been used for processing a file and as of that might have an internal state set.
 */
public class RuleProvider(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> Rule
) {
    /**
     * Creates a RuleProvider based on a [RuleSet] to provide backwards compatability with KtLint 0.46. This will be
     * removed in KtLint 0.48. Creation of the rule in this way is memory expensive as each new instance for all rules
     * in a rule set have to be created from which only one instance is used.
     */
    @Deprecated("Marked for removal in KtLint 0.48")
    public constructor(ruleSetProvider: RuleSetProvider, ruleIndex: Int) : this(
        provider = {
            ruleSetProvider
                // Get a new RuleSet to create new instances for all rules
                .get()
                // but only use the instance for the rule at the specified index
                .elementAt(ruleIndex)
        }
    )

    public fun createNewRuleInstance() =
        provider()
}
