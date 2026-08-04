package io.github.ktlint.core.rule.engine.core.api

import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

public class RuleV2Provider private constructor(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> RuleV2,
    /**
     * The rule id of the [RuleV2] created by the provider.
     */
    public val ruleId: RuleId,
    /**
     * Set of [EditorConfigProperty]'s that are to provided to the rule. Only specify the properties that are actually used by the rule.
     */
    public val usesEditorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) {
    /**
     * Creates a new [RuleV2] instance.
     */
    public fun createNewRuleInstance(): RuleV2 = provider()

    /**
     * Lambda which creates a new instance of the [RuleV2]. Important: to ensure that a [RuleV2] can keep internal state and that processing
     * of files is thread-safe, a *new* instance should be provided on each call of the [provider] function.
     */
    public companion object {
        // Note that the KDOC is placed on the companion object to make it actually visually when the RuleProvider identifier is being
        // hovered in IntelliJ IDEA
        public operator fun invoke(provider: () -> RuleV2): RuleV2Provider =
            with(provider()) {
                RuleV2Provider(provider = provider, ruleId = ruleId, usesEditorConfigProperties = usesEditorConfigProperties)
            }
    }
}

/**
 * Extract all [PropertyType]'s for [EditorConfigProperty]'s that are in rules provided via the given collection of
 * [RuleV2Provider]'s.
 */
public fun Collection<RuleV2Provider>.propertyTypes(): Set<PropertyType<*>> =
    asSequence()
        .flatMap { it.usesEditorConfigProperties }
        .map { it.type }
        .toSet()
