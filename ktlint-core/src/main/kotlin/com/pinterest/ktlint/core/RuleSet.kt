package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.internal.IdNamingPolicy

/**
 * A group of [Rule]s discoverable through [RuleSetProvider].
 *
 * This class becomes redundant when resolving the deprecation of [RuleSetProvider.get].
 */
@Deprecated("Marked for removal in KtLint 0.48. See KDoc.")
public open class RuleSet(
    public val id: String,
    public vararg val rules: Rule
) : Iterable<Rule> {

    init {
        IdNamingPolicy.enforceRuleSetIdNaming(id)
        require(rules.isNotEmpty()) { "At least one rule must be provided" }
    }

    override fun iterator(): Iterator<Rule> = rules.iterator()
}
