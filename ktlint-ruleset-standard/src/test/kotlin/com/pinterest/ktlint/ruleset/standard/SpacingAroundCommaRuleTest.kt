package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundCommaRuleTest {
    private val spacingAroundCommaRuleAssertThat = assertThatRule { SpacingAroundCommaRule() }

    @Test
    fun `Given some parameter list not having a parameter after the comma`() {
        val code =
            """
            val foo1 = Foo(1,3)
            val foo2 = Foo(1, 3)
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = Foo(1, 3)
            val foo2 = Foo(1, 3)
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            // TODO: Col offset is not correct
            .hasLintViolation(1, 18, "Missing spacing after \",\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some string containing a comma not followed by a space`() {
        val code =
            """
            val foo = "bar1,bar2"
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given some enumeration not having a space after each comma`() {
        val code =
            """
            enum class E {
                A, B,C
            }
            """.trimIndent()
        val formattedCode =
            """
            enum class E {
                A, B, C
            }
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            // TODO: Col offset is not correct
            .hasLintViolation(2, 10, "Missing spacing after \",\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some parameter list with an unexpected space before the comma`() {
        val code =
            """
            val foo = Foo(1 , 2)
            """.trimIndent()
        val formattedCode =
            """
            val foo = Foo(1, 2)
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasLintViolation(1, 16, "Unexpected spacing before \",\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some parameter list with an unexpected newline before the comma`() {
        val code =
            """
            fun fn(
                arg1: Int ,
                arg2: Int
                ,

                arg3: Int
            ) = Unit
            """.trimIndent()
        val formattedCode =
            """
            fun fn(
                arg1: Int,
                arg2: Int,

                arg3: Int
            ) = Unit
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 14, "Unexpected spacing before \",\""),
                LintViolation(3, 14, "Unexpected spacing before \",\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some parameter list with an EOL comment followed by comma on next line`() {
        val code =
            """
            fun fn(
                arg1: Int ,
                arg2: Int // some comment
                , arg3: Int
                // other comment
                , arg4: Int
            ) = Unit
            """.trimIndent()
        val formattedCode =
            """
            fun fn(
                arg1: Int,
                arg2: Int, // some comment
                arg3: Int,
                // other comment
                arg4: Int
            ) = Unit
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 14, "Unexpected spacing before \",\""),
                LintViolation(3, 30, "Unexpected spacing before \",\""),
                LintViolation(5, 21, "Unexpected spacing before \",\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a parameter list with a space between a block comment and the comma`() {
        val code =
            """
            val foo1 = Foo(1 /* comment */ , 3)
            val foo2 = Foo(1 /* comment */, 3)
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = Foo(1 /* comment */, 3)
            val foo2 = Foo(1 /* comment */, 3)
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasLintViolation(1, 31, "Unexpected spacing before \",\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotation parameter list with a trailing comma not followed by a space`() {
        val code =
            """
            @file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE",)
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an annotation parameter array with a trailing comma not followed by a space`() {
        val code =
            """
            @file:Suppress(["unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE",])
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a generic type parameter list with a trailing comma not followed by a space`() {
        val code =
            """
            fun <T, R,> test() = Unit

            fun foo() {
                test<Int, Double,>()
            }
            """.trimIndent()
        spacingAroundCommaRuleAssertThat(code)
            .hasNoLintViolations()
    }
}
