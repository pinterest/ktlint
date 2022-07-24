package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class NoUnitReturnRuleTest {
    private val noUnitReturnRuleAssertThat = assertThatRule { NoUnitReturnRule() }

    @Test
    fun `Given a function that does not have a return type then do no return a lint error`() {
        val code =
            """
            fun foo() {}
            """.trimIndent()
        noUnitReturnRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function that does have a return type other than Unit then do no return a lint error`() {
        val code =
            """
            fun foo(): String = "foo"
            """.trimIndent()
        noUnitReturnRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function that does have Unit as return type then do return a lint error`() {
        val code =
            """
            fun foo(): Unit {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {}
            """.trimIndent()
        noUnitReturnRuleAssertThat(code)
            .hasLintViolation(1, 12, "Unnecessary \"Unit\" return type")
            .isFormattedAs(formattedCode)
    }

    @Disabled("To be fixed")
    @Test
    fun `Given a function that does have Unit as return type and is followed by a comment before the body then do return a lint error`() {
        val code =
            """
            fun foo1(): Unit = bar()
            fun foo2(): Unit // Some comment
                = bar()
            fun foo3(): Unit /* Some comment */
                = bar()
            fun foo4(): Unit
                /* Some comment */
                = bar()
            fun bar() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo1() = bar()
            fun foo2() // Some comment
                = bar()
            fun foo3() /* Some comment */
                = bar()
            fun foo4()
                /* Some comment */
                = bar()
            fun bar() {}
            """.trimIndent()
        noUnitReturnRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Unnecessary \"Unit\" return type"),
                LintViolation(2, 13, "Unnecessary \"Unit\" return type"),
                LintViolation(3, 13, "Unnecessary \"Unit\" return type"),
                LintViolation(5, 13, "Unnecessary \"Unit\" return type")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function having a parameter of type Unit then do not return a lint error`() {
        val code =
            """
            fun foo(bar: Unit) {}
            """.trimIndent()
        noUnitReturnRuleAssertThat(code).hasNoLintViolations()
    }
}
