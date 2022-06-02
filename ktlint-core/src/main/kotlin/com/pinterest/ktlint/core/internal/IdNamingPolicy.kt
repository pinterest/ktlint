package com.pinterest.ktlint.core.internal

/**
 * Provides policy to have consistent and restricted `id` field naming style.
 */
internal object IdNamingPolicy {
    private const val SIMPLE_ID_REGEX = "[a-z]+(-[a-z]+)*"
    private val ruleIdRegex = "($SIMPLE_ID_REGEX:)?($SIMPLE_ID_REGEX)".toRegex()
    private val ruleSetIdRegex = "($SIMPLE_ID_REGEX)".toRegex()

    /**
     * Checks provided [ruleId] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [ruleId] name.
     */
    internal fun enforceRuleIdNaming(ruleId: String) =
        require(ruleId.matches(ruleIdRegex)) { "Rule id '$ruleId' must match '${ruleIdRegex.pattern}'" }

    /**
     * Checks provided [ruleSetId] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [ruleSetId] name.
     */
    internal fun enforceRuleSetIdNaming(ruleSetId: String) =
        require(ruleSetId.matches(ruleSetIdRegex)) { "RuleSet id '$ruleSetId' must match '${ruleSetIdRegex.pattern}'" }
}
