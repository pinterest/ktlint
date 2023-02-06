package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.RuleId

/**
 * Standard rules can only be declared and instantiated in the 'ktlint-ruleset-standard'. Custom rule set providers or API consumers have
 * to extend the [Rule] class to define a custom rule.
 */
public open class StandardRule internal constructor(
    id: String,
    override val visitorModifiers: Set<VisitorModifier> = emptySet(),
) : Rule(
    ruleId = RuleId("standard:$id"),
    visitorModifiers = visitorModifiers,
)
