package com.pinterest.ktlint.core

/**
 * Ensures that the rule annotated with RunAfterRule appears in the rule execution order after a specified other rule.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class RunAfterRule(
    /**
     * Qualified ruleId in format "ruleSetId:ruleId". For a rule in the standard rule set it suffices to specify the
     * ruleId only.
     */
    val ruleId: String,
    /**
     * The annotated rule will only be loaded in case the other rule is loaded as well.
     */
    val loadOnlyWhenOtherRuleIsLoaded: Boolean = false,
    /**
     * The annotated rule will only be run in case the other rule is enabled.
     */
    val runOnlyWhenOtherRuleIsEnabled: Boolean = false
)
