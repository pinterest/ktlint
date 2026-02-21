package com.pinterest.ktlint.rule.engine.core.api

/**
 * Provides either a [RuleV1], or a [RuleV2] instance. Important: to ensure that a [RuleV2] can keep internal state and that processing of
 * files is thread-safe, a *new* instance should be provided on each call of the [provider] function.
 */
@Deprecated(
    message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x.",
    replaceWith = ReplaceWith("RuleV2InstanceProvider", "com.pinterest.ktlint.core.RuleV2InstanceProvider"),
    level = DeprecationLevel.WARNING,
)
public class RuleProvider private constructor(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> RuleBase,
    /**
     * The rule id of the [RuleBase] created by the provider.
     */
    public override val ruleId: RuleId,
    /**
     * Flag whether the [RuleV2] created by the provider has to run as late as possible.
     */
    public override val runAsLateAsPossible: Boolean,
    /**
     * The list of rules which have to run before the [RuleV2] created by the provider can be run.
     */
    public override val runAfterRules: List<RuleBase.VisitorModifier.RunAfterRule>,
) : RuleInstanceProvider(ruleId, runAsLateAsPossible, runAfterRules) {
    /**
     * Creates a new [RuleV1] or [RuleV2] instance.
     */
    public override fun createNewRuleInstance(): RuleBase =
        provider()
            .also { require(it is RuleV1 || it is RuleV2) }

    /**
     * Lambda which creates a new instance of the [RuleV2]. Important: to ensure that a [RuleV2] can keep internal state and that processing
     * of files is thread-safe, a *new* instance should be provided on each call of the [provider] function.
     */
    public companion object {
        // Note that the KDOC is placed on the companion object to make it actually visually when the RuleProvider identifier is being
        // hovered in IntelliJ IDEA
        public operator fun invoke(provider: () -> RuleBase): RuleProvider =
            provider()
                .let { rule ->
                    RuleProvider(
                        provider = provider,
                        ruleId = rule.ruleId,
                        runAsLateAsPossible =
                            rule
                                .visitorModifiers
                                .filterIsInstance<RuleBase.VisitorModifier.RunAsLateAsPossible>()
                                .any(),
                        runAfterRules =
                            rule
                                .visitorModifiers
                                .filterIsInstance<RuleBase.VisitorModifier.RunAfterRule>(),
                    )
                }
    }
}
