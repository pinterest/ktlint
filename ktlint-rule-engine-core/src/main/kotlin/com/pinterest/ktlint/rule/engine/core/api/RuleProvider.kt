package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

/**
 * Provides a [Rule] instance. Important: to ensure that a [Rule] can keep internal state and that processing of files is thread-safe, a
 * *new* instance should be provided on each call of the [provider] function.
 */
public class RuleProvider private constructor(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> Rule,
    /**
     * The rule id of the [Rule] created by the provider.
     */
    public val ruleId: RuleId,
    /**
     * Flag whether the [Rule] created by the provider has to run as late as possible.
     */
    public val runAsLateAsPossible: Boolean,
    /**
     * The list of rules which have to run before the [Rule] created by the provider can be run.
     */
    public val runAfterRules: List<Rule.VisitorModifier.RunAfterRule>,
) {
    /**
     * Creates a new [Rule] instance.
     */
    public fun createNewRuleInstance(): Rule = provider()

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
                                .filterIsInstance<Rule.VisitorModifier.RunAfterRule>(),
                    )
                }
    }
}

/**
 * Extract all [PropertyType]'s for [EditorConfigProperty]'s that are in rules provided via the given collection of [RuleProvider]'s.
 */
public fun Collection<RuleProvider>.propertyTypes(): Set<PropertyType<*>> =
    map { it.createNewRuleInstance() }
        .flatMap { it.usesEditorConfigProperties }
        .distinct()
        .toSet()
        .map { it.type }
        .toSet()
