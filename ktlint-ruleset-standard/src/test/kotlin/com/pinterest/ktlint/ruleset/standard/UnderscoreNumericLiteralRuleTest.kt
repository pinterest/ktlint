package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnderscoreNumericLiteralRuleTest {

    @Test
    fun `skip bin and hex values`() {
        assertThat(
            UnderscoreNumericLiteralRule().lint(
                """
                val binValue = 0b11011101
                val hexValue: ULong = 0xFFFFFFFFFFFFu
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `every 3rd digit should be underscored`() {

        assertThat(UnderscoreNumericLiteralRule().lint("""
        val numericLiteral = 12345_678
        val numericLiteral2 = 12345_678L
        val numericLiteral3 = 12345678
        val numericLiteral4 = 12345678u
        val numericLiteral5 = 12345678uL
        val numericLiteral7: ULong = 0xFFFFFFFFFFFFu
        """.trimIndent()
        )).containsExactly(
            LintError(
                line = 1,
                col = 22,
                ruleId = "underscore-numeric-literal",
                detail = "Numeric literals should be delimited with '_'"
            ),
            LintError(
                line = 2,
                col = 23,
                ruleId = "underscore-numeric-literal",
                detail = "Numeric literals should be delimited with '_'"
            ),
            LintError(
                line = 3,
                col = 23,
                ruleId = "underscore-numeric-literal",
                detail = "Numeric literals should be delimited with '_'"
            ),
            LintError(
                line = 4,
                col = 23,
                ruleId = "underscore-numeric-literal",
                detail = "Numeric literals should be delimited with '_'"
            ),
            LintError(
                line = 5,
                col = 23,
                ruleId = "underscore-numeric-literal",
                detail = "Numeric literals should be delimited with '_'"
            )
        )
    }

    @Test
    fun `non-hex digits should be autocorrected`() {
        assertThat(
            UnderscoreNumericLiteralRule().format(
                """
                val floatVal = 1000000f
                val numericLiteral = 12345_678
                val numericLiteral2 = 12345_678L
                val numericLiteral3 = 12345678
                val numericLiteral4 = 12345678u
                val numericLiteral5 = 12345678uL
                val numericLiteral6 = 12345678.1312313
                """.trimIndent()
            )
        ).isEqualTo(
            """
            val floatVal = 1_000_000f
            val numericLiteral = 12_345_678
            val numericLiteral2 = 12_345_678L
            val numericLiteral3 = 12_345_678
            val numericLiteral4 = 12_345_678u
            val numericLiteral5 = 12_345_678uL
            val numericLiteral6 = 12_345_678.1312313
            """.trimIndent()
        )
    }
}
