package com.pinterest.ktlint.core.internal

internal fun String.toQualifiedRuleId() =
    if (contains(":")) {
        this
    } else {
        "standard:$this"
    }

internal fun RuleReference.toQualifiedRuleId() =
    toQualifiedRuleId(ruleSetId, ruleId)

internal fun toQualifiedRuleId(
    ruleSetId: String,
    ruleId: String
) =
    if (ruleId.startsWith("$ruleSetId:")) {
        ruleId
    } else {
        "$ruleSetId:$ruleId"
    }

internal fun RuleReference.toShortenedQualifiedRuleId() =
    if (ruleSetId == "standard") {
        ruleId
    } else {
        toQualifiedRuleId()
    }
