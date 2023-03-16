package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundRangeOperatorRuleTest {
    private val spacingAroundRangeOperatorRuleAssertThat = assertThatRule { SpacingAroundRangeOperatorRule() }

    @Test
    fun `Given a range`() {
        val code =
            """
            val foo1 = (1..12 step 2).last
            val foo2 = (1.. 12 step 2).last
            val foo3 = (1 .. 12 step 2).last
            val foo4 = (1 ..12 step 2).last
            fun foo() {
                for (i in 1..4) print(i)
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = (1..12 step 2).last
            val foo2 = (1..12 step 2).last
            val foo3 = (1..12 step 2).last
            val foo4 = (1..12 step 2).last
            fun foo() {
                for (i in 1..4) print(i)
            }
            """.trimIndent()
        spacingAroundRangeOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 16, "Unexpected spacing after \"..\""),
                LintViolation(3, 15, "Unexpected spacing around \"..\""),
                LintViolation(4, 14, "Unexpected spacing before \"..\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a range in a for loop`() {
        val code =
            """
            fun foo() {
                for (i in 1..4) print(i)
                for (i in 1.. 4) print(i)
                for (i in 1 .. 4) print(i)
                for (i in 1 ..4) print(i)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                for (i in 1..4) print(i)
                for (i in 1..4) print(i)
                for (i in 1..4) print(i)
                for (i in 1..4) print(i)
            }
            """.trimIndent()
        spacingAroundRangeOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 18, "Unexpected spacing after \"..\""),
                LintViolation(4, 17, "Unexpected spacing around \"..\""),
                LintViolation(5, 16, "Unexpected spacing before \"..\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a rangeUntil operator`() {
        val code =
            """
            @OptIn(ExperimentalStdlibApi::class)
            fun foo(int: Int): String =
                when (int) {
                    in 10..<20 -> "A"
                    in 20..< 30 -> "B"
                    in 30 ..< 40 -> "C"
                    in 40 ..<50 -> "D"
                    else -> "unknown"
                }
            """.trimIndent()
        val formattedCode =
            """
            @OptIn(ExperimentalStdlibApi::class)
            fun foo(int: Int): String =
                when (int) {
                    in 10..<20 -> "A"
                    in 20..<30 -> "B"
                    in 30..<40 -> "C"
                    in 40..<50 -> "D"
                    else -> "unknown"
                }
            """.trimIndent()
        spacingAroundRangeOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 17, "Unexpected spacing after \"..<\""),
                LintViolation(6, 15, "Unexpected spacing around \"..<\""),
                LintViolation(7, 14, "Unexpected spacing before \"..<\""),
            ).isFormattedAs(formattedCode)
    }
}
