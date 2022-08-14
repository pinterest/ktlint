package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.RuleSetProviderTest

class ExperimentalRuleSetProviderTest : RuleSetProviderTest(
    rulesetClass = ExperimentalRuleSetProvider::class.java,
    packageName = "com.pinterest.ktlint.ruleset.experimental",
)
