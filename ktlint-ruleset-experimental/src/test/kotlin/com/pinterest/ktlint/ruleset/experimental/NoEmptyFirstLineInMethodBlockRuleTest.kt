package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class NoEmptyFirstLineInMethodBlockRuleTest {
    private val noEmptyFirstLineInMethodBlockRuleAssertThat = NoEmptyFirstLineInMethodBlockRule().assertThat()

    @Test
    fun `Given a block in which the first line is not blank`() {
        val code =
            """
            fun bar() {
               val a = 2
            }
            """.trimIndent()
        noEmptyFirstLineInMethodBlockRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function in which the first line is blank`() {
        val code =
            """
            fun bar() {

               val a = 2
            }
            """.trimIndent()
        val formattedFunction =
            """
            fun bar() {
               val a = 2
            }
            """.trimIndent()
        noEmptyFirstLineInMethodBlockRuleAssertThat(code)
            .hasLintViolation(2, 1, "First line in a method block should not be empty")
            .isFormattedAs(formattedFunction)
    }

    @Test
    fun `Given an if-statement in a function in which the first line is blank`() {
        val code =
            """
            fun foo() {
                if (false) {

                    1
                } else if (true) {

                    2
                } else {

                    3
                }
            }
            """.trimIndent()
        val formattedFunction =
            """
            fun foo() {
                if (false) {
                    1
                } else if (true) {
                    2
                } else {
                    3
                }
            }
            """.trimIndent()
        noEmptyFirstLineInMethodBlockRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "First line in a method block should not be empty"),
                LintViolation(6, 1, "First line in a method block should not be empty"),
                LintViolation(9, 1, "First line in a method block should not be empty")
            ).isFormattedAs(formattedFunction)
    }

    @Test
    fun `Given a class declaration starting with an empty line is allowed`() {
        val code =
            """
            class A {

                fun bar() {
                   val a = 2
                }
            }
            """.trimIndent()
        noEmptyFirstLineInMethodBlockRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 655 - Given an anonymous object starting with a blank line is allowed`() {
        val code =
            """
            fun fooBuilder() = object : Foo {

                override fun foo() {
                    TODO()
                }
            }
            """.trimIndent()
        noEmptyFirstLineInMethodBlockRuleAssertThat(code).hasNoLintViolations()
    }
}
