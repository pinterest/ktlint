package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundParensRuleTest {
    private val spacingAroundParensRuleAssertThat = assertThatRule { SpacingAroundParensRule() }

    @Test
    fun `Issue 369 - Call to super`() {
        val code =
            """
            open class Bar
            class Foo : Bar {
                constructor(string: String) : super ()
            }
            """.trimIndent()
        val formattedCode =
            """
            open class Bar
            class Foo : Bar {
                constructor(string: String) : super()
            }
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(3, 40, "Unexpected spacing before \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a variable declaration with unexpected spacing around the opening parenthesis of the expression`() {
        val code =
            """
            val foo1 = ( (1 + 2) / 3)
            val foo2 = ((1 + 2) / 3)
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = ((1 + 2) / 3)
            val foo2 = ((1 + 2) / 3)
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 13, "Unexpected spacing after \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a variable declaration with unexpected spacing around the closing parenthesis of the expression`() {
        val code =
            """
            val foo1 = ((1 + 2) / 3 )
            val foo2 = ((1 + 2) / 3)
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = ((1 + 2) / 3)
            val foo2 = ((1 + 2) / 3)
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 24, "Unexpected spacing before \")\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call to a function with an unexpected space between the function name and the opening parenthesis`() {
        val code =
            """
            val foo = fn ("foo")
            """.trimIndent()
        val formattedCode =
            """
            val foo = fn("foo")
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 13, "Unexpected spacing before \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call to a function with an unexpected space before the first parameter`() {
        val code =
            """
            val foo1 = fn ( "foo")
            val foo2 = fn( "foo")
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = fn("foo")
            val foo2 = fn("foo")
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 15, "Unexpected spacing around \"(\""),
                LintViolation(2, 15, "Unexpected spacing after \"(\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call to a function with an unexpected space after the last parameter`() {
        val code =
            """
            val foo = fn("foo" )
            """.trimIndent()
        val formattedCode =
            """
            val foo = fn("foo")
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 19, "Unexpected spacing before \")\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotation with a space before the opening parenthesis`() {
        val code =
            """
            @Deprecated ("Foo")
            val foo = "foo"
            """.trimIndent()
        val formattedCode =
            """
            @Deprecated("Foo")
            val foo = "foo"
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 12, "Unexpected spacing before \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function call without parameters and space between the parenthesis`() {
        val code =
            """
            val foo = fn( )
            """.trimIndent()
        val formattedCode =
            """
            val foo = fn()
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 14, "Unexpected spacing after \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function call without parameters and the closing parenthesis on a separate line`() {
        val code =
            """
            val foo = fn(
                )
            """.trimIndent()
        val formattedCode =
            """
            val foo = fn()
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 14, "Unexpected spacing after \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some correctly spaced expressions`() {
        val code =
            """
            val foo1 = mapOf(
                "key" to (v ?: "")
            )

            val foo2 = ((1 + 2) / 3)
            val foo3 = (
                (1 + 2) / 3
                )
            val foo4 = fn((1 + 2) / 3)
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function call then allow a space after the opening parenthesis when followed by a comment`() {
        val code =
            """
            fun main() {
                System.out.println( /** 123 */
                    "test kdoc"
                )
                System.out.println( /* 123 */
                    "test comment block"
                )
                System.out.println( // 123
                    "test single comment"
                )
            }
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an annotated function type then allow a space before the opening parenthesis`() {
        val code =
            """
            val myFun1: @Foo () -> Unit = {}
            val myFun2: @Foo() () -> Unit = {}
            val typeParameter1: (@Foo () -> Unit) -> Unit = { {} }
            val typeParameter2: (@Foo() () -> Unit) -> Unit = { { } }
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given class with an explicit constructor`() {
        val code =
            """
            class SomeClass constructor ()
            """.trimIndent()
        val formattedCode =
            """
            class SomeClass constructor()
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(1, 28, "Unexpected spacing before \"(\"")
            .isFormattedAs(formattedCode)
    }
}
