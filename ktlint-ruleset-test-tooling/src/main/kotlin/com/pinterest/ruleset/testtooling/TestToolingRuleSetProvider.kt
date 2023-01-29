package com.pinterest.ruleset.testtooling

import com.pinterest.ktlint.ruleset.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.core.api.RuleSetProviderV3

public class TestToolingRuleSetProvider :
    RuleSetProviderV3(
        id = "test",
        about = NO_ABOUT,
    ) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { DumpASTRule() },
        )
}
