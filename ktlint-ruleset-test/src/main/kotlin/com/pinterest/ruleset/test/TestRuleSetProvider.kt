package com.pinterest.ruleset.test

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

public class TestRuleSetProvider : RuleSetProvider {
    override fun get(): RuleSet = RuleSet(
        "test",
        DumpASTRule(),
    )
}
