package com.pinterest.ktlint.core.internal

// TODO: Remove once the experimental ruleset is merged into the standard ruleset
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

private fun String.removeSpaces() =
    this.replace(" ", "")
