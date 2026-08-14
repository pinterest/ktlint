package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoBlankLineAtStartOfFileRuleTest {
    private val noBlankLineAtStartOfFileRuleAssertThat = assertThatRule { NoBlankLineAtStartOfFileRule() }

    @Test
    fun `Given a blank line before the Copyright comment`() {
        val code =
            """
            |
            |/*
            | * Copyright 2026
            | */
            """.trimMargin()
        val formattedCode =
            """
            /*
             * Copyright 2026
             */
            """.trimIndent()
        noBlankLineAtStartOfFileRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unexpected whitespace at start of file")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a blank line before the package statement`() {
        val code =
            """
            |
            |package foo
            """.trimMargin()
        val formattedCode =
            """
            package foo
            """.trimIndent()
        noBlankLineAtStartOfFileRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unexpected whitespace at start of file")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a blank line before the first import statement`() {
        val code =
            """
            |
            |import foo
            """.trimMargin()
        val formattedCode =
            """
            import foo
            """.trimIndent()
        noBlankLineAtStartOfFileRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unexpected whitespace at start of file")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a blank line before the first declaration`() {
        val code =
            """
            |
            |val foo = "foo"
            """.trimMargin()
        val formattedCode =
            """
            val foo = "foo"
            """.trimIndent()
        noBlankLineAtStartOfFileRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unexpected whitespace at start of file")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an empty file`() {
        val code = ""
        noBlankLineAtStartOfFileRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a file containing whitespace only`() {
        val code = " "
        val formattedCode = ""
        noBlankLineAtStartOfFileRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unexpected whitespace at start of file")
            .isFormattedAs(formattedCode)
    }
}
