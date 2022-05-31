package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule

internal data class RuleReference(
    val ruleId: String,
    val ruleSetId: String,
    val runOnRootNodeOnly: Boolean,
    val runAsLateAsPossible: Boolean,
    val runAfterRule: Rule.VisitorModifier.RunAfterRule?
)
