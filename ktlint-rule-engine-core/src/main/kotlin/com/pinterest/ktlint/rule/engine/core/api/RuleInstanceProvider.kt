package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

public sealed class RuleInstanceProvider(
    public open val ruleId: RuleId,
    public open val runAsLateAsPossible: Boolean,
    public open val runAfterRules: List<RuleV2.VisitorModifier.RunAfterRule>,
) {
    public abstract fun createNewRuleInstance(): RuleV2
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
