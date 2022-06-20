package com.pinterest.ktlint.core.internal

internal fun String.toQualifiedRuleId() =
    if (contains(":")) {
        this
    } else {
        // When IntelliJ IDEA is reformatting the ".editorconfig" file it sometimes add a space after the comma in a
        // comma-separate-list.
        "standard:$this"
    }.removeSpaces()

internal fun RuleReference.toQualifiedRuleId() =
    toQualifiedRuleId(ruleSetId, ruleId)

internal fun toQualifiedRuleId(
    ruleSetId: String,
    ruleId: String
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

internal fun RuleReference.toShortenedQualifiedRuleId() =
    if (ruleSetId.removeSpaces() == "standard" || ruleSetId.removeSpaces() == "") {
        ruleId.removeSpaces()
    } else {
        toQualifiedRuleId()
    }

private fun String.removeSpaces() =
    this.replace(" ", "")
