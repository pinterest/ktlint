package com.github.shyiko.ktlint.core

/**
 * A group of [Rule]s discoverable through [RuleSetProvider].
 * @see RuleSetProvider
 */
class RuleSet(val id: String, vararg val rules: Rule) : Iterable<Rule> {

    init {
        require(id.matches(Regex("[a-z]+([-][a-z]+)*"))) { "id must match [a-z]+([-][a-z]+)*" }
        require(!rules.isEmpty()) { "At least one rule must be provided" }
    }

    override fun iterator(): Iterator<Rule> = rules.iterator()

}
