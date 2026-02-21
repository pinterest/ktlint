package com.pinterest.ktlint.rule.engine.core.api

/**
 * Provides a [Rule] instance. Important: to ensure that a [Rule] can keep internal state and that processing of files is thread-safe,
 * a *new* instance should be provided on each call of the [provider] function.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x.",
    replaceWith = ReplaceWith("RuleV2InstanceProvider", "com.pinterest.ktlint.core.RuleV2InstanceProvider"),
    level = DeprecationLevel.WARNING,
)
public class RuleProvider private constructor(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> Rule,
    /**
     * The rule id of the [RuleV2] created by the provider.
     */
    public override val ruleId: RuleId,
    /**
     * Flag whether the [Rule] created by the provider has to run as late as possible.
     */
    public override val runAsLateAsPossible: Boolean,
    /**
     * The list of rules which have to run before the [Rule] created by the provider can be run.
     */
    public override val runAfterRules: List<RuleV2.VisitorModifier.RunAfterRule>,
) : RuleInstanceProvider(ruleId, runAsLateAsPossible, runAfterRules) {
    /**
     * Creates a new [RuleV2] instance.
     */
    public override fun createNewRuleInstance(): RuleV2 =
        provider()
            .also {
                require(it is RuleAutocorrectApproveHandler) {
                    "Ktlint 2.x does not support rules that have not correctly implemented the RuleAutocorrectApproveHandler. Use a new " +
                        "version of the ruleset. or contact the maintainer of this ruleset to upgrade it."
                }
            }.let { rule ->
                object :
                    RuleV2(
                        ruleId = ruleId,
                        about =
                            About(
                                maintainer = rule.about.maintainer,
                                repositoryUrl = rule.about.repositoryUrl,
                                issueTrackerUrl = rule.about.issueTrackerUrl,
                            ),
                        visitorModifiers =
                            rule.visitorModifiers
                                .map {
                                    when (it) {
                                        is Rule.VisitorModifier.RunAfterRule -> {
                                            VisitorModifier.RunAfterRule(
                                                it.ruleId,
                                                when (it.mode) {
                                                    Rule.VisitorModifier.RunAfterRule.Mode
                                                        .ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED,
                                                    -> {
                                                        VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
                                                    }

                                                    Rule.VisitorModifier.RunAfterRule.Mode
                                                        .REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                                    -> {
                                                        VisitorModifier.RunAfterRule.Mode
                                                            .REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
                                                    }
                                                },
                                            )
                                        }

                                        is Rule.VisitorModifier.RunAsLateAsPossible -> {
                                            VisitorModifier.RunAsLateAsPossible
                                        }
                                    }
                                }.toSet(),
                        usesEditorConfigProperties = rule.usesEditorConfigProperties,
                    ) {}
            }

    /**
     * Lambda which creates a new instance of the [Rule]. Important: to ensure that a [Rule] can keep internal state and that processing of
     * files is thread-safe, a *new* instance should be provided on each call of the [provider] function.
     */
    public companion object {
        // Note that the KDOC is placed on the companion object to make it actually visually when the RuleProvider identifier is being
        // hovered in IntelliJ IDEA
        public operator fun invoke(provider: () -> Rule): RuleProvider =
            provider()
                .let { rule ->
                    RuleProvider(
                        provider = provider,
                        ruleId = rule.ruleId,
                        runAsLateAsPossible =
                            rule
                                .visitorModifiers
                                .filterIsInstance<Rule.VisitorModifier.RunAsLateAsPossible>()
                                .any(),
                        runAfterRules =
                            rule
                                .visitorModifiers
                                .filterIsInstance<Rule.VisitorModifier.RunAfterRule>()
                                .map {
                                    RuleV2.VisitorModifier.RunAfterRule(
                                        it.ruleId,
                                        when (it.mode) {
                                            Rule.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED -> {
                                                RuleV2.VisitorModifier.RunAfterRule.Mode.ONLY_WHEN_RUN_AFTER_RULE_IS_LOADED_AND_ENABLED
                                            }

                                            Rule.VisitorModifier.RunAfterRule.Mode
                                                .REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
                                            -> {
                                                RuleV2.VisitorModifier.RunAfterRule.Mode
                                                    .REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
                                            }
                                        },
                                    )
                                },
                    )
                }
    }
}
