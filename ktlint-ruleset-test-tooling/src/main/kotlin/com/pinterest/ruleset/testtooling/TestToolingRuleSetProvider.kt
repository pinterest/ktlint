package com.pinterest.ruleset.testtooling

import com.pinterest.ktlint.ruleset.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.core.api.RuleSetProviderV3

internal const val TEST_TOOLING_RULESET_ID = "test-tooling"

public class TestToolingRuleSetProvider :
    RuleSetProviderV3(TEST_TOOLING_RULESET_ID) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { DumpASTRule() },
        )
}
