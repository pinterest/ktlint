package com.github.shyiko.ktlint.ruleset.experimental

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider

class ExperimentalRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet(
        "experimental",
        ImportOrderingRule(),
        IndentationRule()
    )
}
