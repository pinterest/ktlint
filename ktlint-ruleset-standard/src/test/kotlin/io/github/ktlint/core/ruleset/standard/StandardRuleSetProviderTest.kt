package io.github.ktlint.core.ruleset.standard

import io.github.ktlint.core.test.RuleSetProviderTest

class StandardRuleSetProviderTest :
    RuleSetProviderTest(
        rulesetClass = StandardRuleSetProvider::class.java,
        packageName = "io.github.ktlint.core.ruleset.standard.rules",
    )
