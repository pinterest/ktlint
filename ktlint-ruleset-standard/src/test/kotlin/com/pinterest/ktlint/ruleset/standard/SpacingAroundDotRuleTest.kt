package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundDotRuleTest {
    private val spacingAroundDotRuleAssertThat = assertThatRule { SpacingAroundDotRule() }

    @Test
    fun `Given an extension function with an unexpected space before the dot`() {
        val code =
            """
            fun String .foo() = "foo . "
            """.trimIndent()
        val formattedCode =
            """
            fun String.foo() = "foo . "
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code)
            .hasLintViolation(1, 11, "Unexpected spacing before \".\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an extension function with an unexpected space after the dot`() {
        val code =
            """
            fun String. foo() = "foo . "
            """.trimIndent()
        val formattedCode =
            """
            fun String.foo() = "foo . "
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code)
            .hasLintViolation(1, 12, "Unexpected spacing after \".\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an extension function with an unexpected space before and after the dot`() {
        val code =
            """
            fun String . foo() = "foo . "
            """.trimIndent()
        val formattedCode =
            """
            fun String.foo() = "foo . "
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Unexpected spacing before \".\""),
                LintViolation(1, 13, "Unexpected spacing after \".\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a range operator with spaces around the operator`() {
        val code =
            """
            val foo1 = (2 .. 10).toSet()
            val foo2 = (2..10).toSet()
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an unexpected space before the dot in a chained call`() {
        val code =
            """
            val foo = (2..10) .map { it * 2 }.toSet()
            """.trimIndent()
        val formattedCode =
            """
            val foo = (2..10).map { it * 2 }.toSet()
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code)
            .hasLintViolation(1, 18, "Unexpected spacing before \".\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an unexpected space after the dot in a chained call`() {
        val code =
            """
            val foo1 = (2 .. 10). map { it * 2 }.toSet()
            val foo2 = (2 .. 10) // some comment
                . map { it * 2 }.toSet()
            val foo3 = (2 .. 10)
                 // some comment
                . map { it * 2 }.toSet()
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = (2 .. 10).map { it * 2 }.toSet()
            val foo2 = (2 .. 10) // some comment
                .map { it * 2 }.toSet()
            val foo3 = (2 .. 10)
                 // some comment
                .map { it * 2 }.toSet()
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 22, "Unexpected spacing after \".\""),
                LintViolation(3, 6, "Unexpected spacing after \".\""),
                LintViolation(6, 6, "Unexpected spacing after \".\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some comments including dots`() {
        val code =
            """
            fun foo() { //
                /**.*/
                generateSequence(locate(dir)) { seed -> locate(seed.parent.parent) } // seed.parent == .editorconfig dir
                    .map { it to lazy { load(it) } }
            }
            """.trimIndent()
        spacingAroundDotRuleAssertThat(code)
            .hasNoLintViolations()
    }
}
