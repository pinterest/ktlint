package com.pinterest.ktlint.rule.engine.core.internal

/**
 * Provides policy to have consistent and restricted `id` field naming style.
 */
internal object IdNamingPolicy {
    private const val SIMPLE_ID_REGEX = "[a-z]+(-[a-z]+)*"
    private val RULE_ID_REGEX = "$SIMPLE_ID_REGEX:$SIMPLE_ID_REGEX".toRegex()
    private val RULE_SET_ID_REGEX = SIMPLE_ID_REGEX.toRegex()

    /**
     * Checks provided [ruleId] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [ruleId] name.
     */
    internal fun enforceRuleIdNaming(ruleId: String) =
        require(ruleId.matches(RULE_ID_REGEX)) { "Rule with id '$ruleId' must match regexp '${RULE_ID_REGEX.pattern}'" }

    /**
     * Checks provided [ruleSetId] is valid.
     *
     * Will throw [IllegalArgumentException] on invalid [ruleSetId] name.
     */
    internal fun enforceRuleSetIdNaming(ruleSetId: String) =
        require(ruleSetId.matches(RULE_SET_ID_REGEX)) { "Rule set id '$ruleSetId' must match '${RULE_SET_ID_REGEX.pattern}'" }
}
