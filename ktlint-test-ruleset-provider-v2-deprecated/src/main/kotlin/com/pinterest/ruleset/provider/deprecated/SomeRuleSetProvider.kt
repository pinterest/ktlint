package com.pinterest.ruleset.provider.deprecated

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2

// This class deliberately extends a deprecated RuleSetProvider as for testing purposes a custom ruleset jar is required that only contains
// a deprecated ruleset provider.
public class SomeRuleSetProvider :
    RuleSetProviderV2(
        id = "test",
        about = NO_ABOUT,
    ) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { SomeRule() },
        )
}
