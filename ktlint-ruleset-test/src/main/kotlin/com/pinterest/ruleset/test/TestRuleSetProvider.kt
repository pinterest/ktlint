package com.pinterest.ruleset.test

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2

public class TestRuleSetProvider :
    RuleSetProviderV2(
        id = "test",
        about = NO_ABOUT
    ) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { DumpASTRule() }
        )
}
