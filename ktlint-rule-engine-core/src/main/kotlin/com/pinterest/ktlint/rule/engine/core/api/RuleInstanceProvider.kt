package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

// TODO: Replace with sealed class? Merge RuleProvider with RuleV1InstanceProvider (or is this not possible due to backwards compatibility?
//  Merge RuleV2Provider with RuleV2InstanceProvider
public sealed interface RuleInstanceProvider {
    public interface RuleV1InstanceProvider : RuleInstanceProvider {
        public override fun createNewRuleInstance(): RuleV1
    }

    public interface RuleV2InstanceProvider : RuleInstanceProvider {
        public override fun createNewRuleInstance(): RuleV2
    }

    public fun createNewRuleInstance(): RuleBase

    public val ruleId: RuleId

    public val runAsLateAsPossible: Boolean

    public val runAfterRules: List<RuleBase.VisitorModifier.RunAfterRule>
}

/**
 * Extract all [PropertyType]'s for [EditorConfigProperty]'s that are in rules provided via the given collection of
 * [RuleInstanceProvider]'s.
 */
public fun Collection<RuleInstanceProvider>.propertyTypes(): Set<PropertyType<*>> =
    map { it.createNewRuleInstance() }
        .flatMap { it.usesEditorConfigProperties }
        .distinct()
        .toSet()
        .map { it.type }
        .toSet()
