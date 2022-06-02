package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundUnaryOperatorRuleTest {
    private val spacingAroundUnaryOperatorRuleAssertThat = SpacingAroundUnaryOperatorRule().assertThat()

    @Test
    fun `Given the '++' operator`() {
        val code =
            """
            fun foo1(i: Int) = i++
            fun foo2(i: Int) = i ++
            fun foo3(i: Int) = ++i
            fun foo4(i: Int) = ++ i
            fun foo5(i: Int) = ++
                i
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(i: Int) = i++
            fun foo2(i: Int) = i++
            fun foo3(i: Int) = ++i
            fun foo4(i: Int) = ++i
            fun foo5(i: Int) = ++i
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 21, "Unexpected spacing in i ++"),
                LintViolation(4, 22, "Unexpected spacing in ++ i"),
                LintViolation(5, 22, "Unexpected spacing in ++\\n    i")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given the '--' operator`() {
        val code =
            """
            fun foo1(i: Int) = i--
            fun foo2(i: Int) = i --
            fun foo3(i: Int) = --i
            fun foo4(i: Int) = -- i
            fun foo5(i: Int) = --
                i
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(i: Int) = i--
            fun foo2(i: Int) = i--
            fun foo3(i: Int) = --i
            fun foo4(i: Int) = --i
            fun foo5(i: Int) = --i
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 21, "Unexpected spacing in i --"),
                LintViolation(4, 22, "Unexpected spacing in -- i"),
                LintViolation(5, 22, "Unexpected spacing in --\\n    i")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given the '-' operator`() {
        val code =
            """
            val foo1 = -1
            val foo2 = - 1
            val foo3 = -
                1
            val foo4 = -1 in -100..-1
            val foo5 = - 1 in -100..-1
            val foo6 = -1 in - 100..-1
            val foo7 = -1 in -100..- 1
            val foo8 = -
                1 in -100..-1
            val foo9 = -1 in -
                100..-1
            val foo10 = -1 in -100..-
                1
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = -1
            val foo2 = -1
            val foo3 = -1
            val foo4 = -1 in -100..-1
            val foo5 = -1 in -100..-1
            val foo6 = -1 in -100..-1
            val foo7 = -1 in -100..-1
            val foo8 = -1 in -100..-1
            val foo9 = -1 in -100..-1
            val foo10 = -1 in -100..-1
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Unexpected spacing in - 1"),
                LintViolation(3, 13, "Unexpected spacing in -\\n    1"),
                LintViolation(6, 13, "Unexpected spacing in - 1"),
                LintViolation(7, 19, "Unexpected spacing in - 100"),
                LintViolation(8, 25, "Unexpected spacing in - 1"),
                LintViolation(9, 13, "Unexpected spacing in -\\n    1"),
                LintViolation(11, 19, "Unexpected spacing in -\\n    100"),
                LintViolation(13, 26, "Unexpected spacing in -\\n    1")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given the '!' operator`() {
        val code =
            """
            fun foo1(i: Int) = !i
            fun foo2(i: Int) = ! i
            fun foo3(i: Int) = !
                i
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(i: Int) = !i
            fun foo2(i: Int) = !i
            fun foo3(i: Int) = !i
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 21, "Unexpected spacing in ! i"),
                LintViolation(3, 21, "Unexpected spacing in !\\n    i")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given the '!!' operator`() {
        val code =
            """
            val foo1 = "foo"!!.length
            val foo2 = "foo" !!.length
            val foo3 = "foo"!! .length
            val foo4 = "foo"!!
                .length
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = "foo"!!.length
            val foo2 = "foo"!!.length
            val foo3 = "foo"!! .length
            val foo4 = "foo"!!
                .length
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 17, "Unexpected spacing in \"foo\" !!")
                // TODO: "foo3" should also be disallowed
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some calculation using multiple unary operators`() {
        val code =
            """
            fun foo1(i: Int) = - (-- i) + 1 + 1
            fun foo2(i: Int) = - (++ i) + 1 + 1
            fun foo3(i: Int) = + 1 - 1 + (- 1)
            fun foo4(): Int {
                var f = 0
                if (- 1 < - f && + f > - 10) {
                    f += -1 + 2 + - 3 - 4 + (- 4)
                }
                return f
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(i: Int) = -(--i) + 1 + 1
            fun foo2(i: Int) = -(++i) + 1 + 1
            fun foo3(i: Int) = +1 - 1 + (-1)
            fun foo4(): Int {
                var f = 0
                if (-1 < -f && +f > -10) {
                    f += -1 + 2 + -3 - 4 + (-4)
                }
                return f
            }
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 21, "Unexpected spacing in - (-- i)"),
                LintViolation(1, 25, "Unexpected spacing in -- i"),
                LintViolation(2, 21, "Unexpected spacing in - (++ i)"),
                LintViolation(2, 25, "Unexpected spacing in ++ i"),
                LintViolation(3, 21, "Unexpected spacing in + 1"),
                LintViolation(3, 32, "Unexpected spacing in - 1"),
                LintViolation(6, 10, "Unexpected spacing in - 1"),
                LintViolation(6, 16, "Unexpected spacing in - f"),
                LintViolation(6, 23, "Unexpected spacing in + f"),
                LintViolation(6, 29, "Unexpected spacing in - 10"),
                LintViolation(7, 24, "Unexpected spacing in - 3"),
                LintViolation(7, 35, "Unexpected spacing in - 4")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some comment before or after the unary '-' operator`() {
        val code =
            """
            val foo1 = - /* comment */ (1 + 1)
            val foo2 = /* comment */- 1
            val foo3 = - 1/* comment */ + 1
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = - /* comment */ (1 + 1)
            val foo2 = /* comment */-1
            val foo3 = -1/* comment */ + 1
            """.trimIndent()
        spacingAroundUnaryOperatorRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 26, "Unexpected spacing in - 1"),
                LintViolation(3, 13, "Unexpected spacing in - 1")
            ).isFormattedAs(formattedCode)
    }
}
