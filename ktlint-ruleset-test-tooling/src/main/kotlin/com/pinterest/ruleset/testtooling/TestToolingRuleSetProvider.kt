package com.pinterest.ruleset.testtooling

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

internal const val TEST_TOOLING_RULESET_ID = "test-tooling"

public class TestToolingRuleSetProvider :
    RuleSetProviderV3(RuleSetId(TEST_TOOLING_RULESET_ID)) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { DumpASTRule() },
        )
}
