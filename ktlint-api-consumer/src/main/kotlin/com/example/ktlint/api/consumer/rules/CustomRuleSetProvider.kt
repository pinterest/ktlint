package com.example.ktlint.api.consumer.rules

import com.pinterest.ktlint.rule.engine.core.api.RuleV2InstanceProvider

internal val CUSTOM_RULE_SET_ID = "custom-rule-set-id"

internal val KTLINT_API_CONSUMER_RULE_PROVIDERS =
    setOf(
        // Can provide custom rules
        RuleV2InstanceProvider { NoVarRule() },
        // If rulesets are include at compile time, they can be added to the custom rule provider.
        // RuleV2InstanceProvider { IndentationRule() },
    )
