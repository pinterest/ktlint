package com.pinterest.ktlint.rule.engine.core.api

/**
 * Provides a [RuleV2] instance. Important: to ensure that a [RuleV2] can keep internal state and that processing of files is thread-safe, a
 * *new* instance should be provided on each call of the [provider] function.
 */
public class RuleV2InstanceProvider private constructor(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> RuleV2,
    /**
     * The rule id of the [RuleV2] created by the provider.
     */
    public override val ruleId: RuleId,
) : RuleInstanceProvider(ruleId) {
    /**
     * Creates a new [RuleV2] instance.
     */
    public override fun createNewRuleInstance(): RuleV2 = provider()

    /**
     * Lambda which creates a new instance of the [RuleV2]. Important: to ensure that a [RuleV2] can keep internal state and that processing
     * of files is thread-safe, a *new* instance should be provided on each call of the [provider] function.
     */
    public companion object {
        // Note that the KDOC is placed on the companion object to make it actually visually when the RuleProvider identifier is being
        // hovered in IntelliJ IDEA
        public operator fun invoke(provider: () -> RuleV2): RuleV2InstanceProvider =
            RuleV2InstanceProvider(provider = provider, ruleId = provider().ruleId)
    }
}
