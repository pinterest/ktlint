package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicy

/**
 * A group of [Rule]s discoverable through [RuleSetProvider].
 * @see RuleSetProvider
 */
open class RuleSet(
    val id: String,
    vararg val rules: Rule
) : Iterable<Rule> {

    init {
        IdNamingPolicy.enforceRuleSetIdNaming(id)
        require(rules.isNotEmpty()) { "At least one rule must be provided" }
    }

    override fun iterator(): Iterator<Rule> = rules.iterator()
}
