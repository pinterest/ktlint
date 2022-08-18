package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.RuleSetProviderTest

class StandardRuleSetProviderTest : RuleSetProviderTest(
    rulesetClass = StandardRuleSetProvider::class.java,
    packageName = "com.pinterest.ktlint.ruleset.standard",
)
