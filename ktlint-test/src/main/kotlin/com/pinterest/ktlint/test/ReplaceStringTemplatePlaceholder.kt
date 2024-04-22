package com.pinterest.ktlint.test

/**
 * Replace the [placeholder] with an escaped "$" (i.e. "${'$'}") so that the resulting raw string literal still contains a string template
 * which then can be processed by the KtlintRuleEngine.
 */
public fun String.replaceStringTemplatePlaceholder(placeholder: String = "$."): String = replace(placeholder, "${'$'}")
