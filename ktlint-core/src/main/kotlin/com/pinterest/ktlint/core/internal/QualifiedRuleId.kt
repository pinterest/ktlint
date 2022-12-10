package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Rule

internal fun Rule.toQualifiedRuleId() =
    id.toQualifiedRuleId()

internal fun String.toQualifiedRuleId(): String {
    val ruleId = if (contains(":")) {
        this
    } else {
        "standard:$this"
    }
    // When IntelliJ IDEA is reformatting the ".editorconfig" file it sometimes add a space after the comma in a
    // comma-separate-list which should not be a part of the ruleId
    return ruleId.removeSpaces()
}

internal fun toQualifiedRuleId(
    ruleSetId: String,
    ruleId: String,
): String {
    if (ruleSetId.contains(" ") || ruleId.contains(" ")) {
        return toQualifiedRuleId(ruleSetId.removeSpaces(), ruleId.removeSpaces())
    }
    return if (ruleSetId == "" || ruleId.startsWith("$ruleSetId:")) {
        ruleId.toQualifiedRuleId()
    } else {
        "$ruleSetId:$ruleId"
    }
}

private fun String.removeSpaces() =
    this.replace(" ", "")

internal fun ruleId(qualifiedRuleId: String) =
    qualifiedRuleId.substringAfter(":", qualifiedRuleId)

internal fun ruleSetId(qualifiedRuleId: String) =
    qualifiedRuleId.substringBefore(":", "standard")
