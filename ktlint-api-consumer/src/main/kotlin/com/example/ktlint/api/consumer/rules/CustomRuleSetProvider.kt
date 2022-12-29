package com.example.ktlint.api.consumer.rules

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.ruleset.standard.IndentationRule

internal val CUSTOM_RULE_SET_ID = "custom-rule-set-id"

internal val KTLINT_API_CONSUMER_RULE_PROVIDERS = setOf(
    // Can provide custom rules
    RuleProvider { NoVarRule() },
    // but also reuse rules from KtLint rulesets
    RuleProvider { IndentationRule() },
)
