package com.pinterest.ktlint.ruleset.standard.internal

/**
 * Transforms a string containing regular expression ranges like "A-Z" and "a-z" to a RegEx which checks whether a
 * unicode character has an uppercase versus a lowercase mapping to a letter. This function intents to keep the original
 * expression more readable
 */
internal fun String.regExIgnoringDiacriticsAndStrokesOnLetters() =
    replace("A-Z", "\\p{Lu}")
        .replace("a-z", "\\p{Ll}")
        .toRegex()
