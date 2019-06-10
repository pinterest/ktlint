package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

class ExperimentalRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet(
        "experimental",
        ImportOrderingRule(),
        IndentationRule(),
        NoFirstLineBlankInMethodBlockRule()
    )
}
