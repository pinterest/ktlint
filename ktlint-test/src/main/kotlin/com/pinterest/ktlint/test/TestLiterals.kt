package com.pinterest.ktlint.test

/**
 * Literal for a multiline string quote """. To be used in code samples for testing the KtLint rules.
 */
public const val MULTILINE_STRING_QUOTE: String = "${'"'}${'"'}${'"'}"

/**
 * Literal for a TAB character. To be used in code samples for testing the KtLint rules.
 */
public const val TAB: String = "${'\t'}"

/**
 * Literal for a SPACE. To be used in code samples for testing the KtLint rules.
 */
public const val SPACE: String = "${' '}"

/*
 * Replace the [placeholder] with an escaped "$" (i.e. "${'$'}") so that the resulting raw sting literal still contains a string template
 * which then can be processed by the KtlintRuleEngine.
 */
public fun String.replaceStringTemplatePlaceholder(placeholder: String = "$."): String = replace(placeholder, "${'$'}")
