package com.example.ktlint.api.consumer.rules

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.ruleset.standard.IndentationRule

internal val KTLINT_API_CONSUMER_RULE_PROVIDERS = setOf(
    // Can provide custom rules
    RuleProvider { NoVarRule() },
    // but also reuse rules from KtLint rulesets
    RuleProvider { IndentationRule() },
)
