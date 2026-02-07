package com.pinterest.ktlint.ruleset.standard.rules

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
    fun `Given class with superclass constructor call`() {
        val code =
            """
            open class Bar(param: String)
            class Foo : Bar ("test")
            """.trimIndent()
        val formattedCode =
            """
            open class Bar(param: String)
            class Foo : Bar("test")
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(2, 16, "Unexpected spacing before \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given class with superclass constructor call with type parameter`() {
        val code =
            """
            open class Bar<T>(param: T)
            class Foo : Bar<String> ("test")
            """.trimIndent()
        val formattedCode =
            """
            open class Bar<T>(param: T)
            class Foo : Bar<String>("test")
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolation(2, 24, "Unexpected spacing before \"(\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function type inside a type projection then do not remove space before the opening parenthesis`() {
        val code =
            """
            val foo: Map<Foo, (Foo) -> Foo> = emptyMap()
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
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
                LintViolation(2, 15, "Unexpected spacing after \"(\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call to a function with an unexpected space after the last parameter`() {
        val code =
            """
            val foo = fn("foo" )
            val foo = fn( )
            """.trimIndent()
        val formattedCode =
            """
            val foo = fn("foo")
            val foo = fn()
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 19, "Unexpected spacing before \")\""),
                LintViolation(2, 14, "Unexpected spacing after \"(\""),
            ).isFormattedAs(formattedCode)
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

    @Test
    fun `Issue 2943x - Given unexpected newline before RPAR in condition`() {
        val code =
            """
            fun foo() {
                if (true
                ) {
                    // do something
                } else {
                    // do something else
                }
                while (true
                ) {
                    // do something
                }
                do while (true
                ) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                } else {
                    // do something else
                }
                while (true) {
                    // do something
                }
                do while (true) {
                    // do something
                }
            }
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Unexpected spacing before \")\""),
                LintViolation(8, 16, "Unexpected spacing before \")\""),
                LintViolation(12, 19, "Unexpected spacing before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2943 - Given unexpected newline after LPAR in condition`() {
        val code =
            """
            fun foo() {
                if (
                true) {
                    // do something
                } else {
                    // do something else
                }
                while (
                    true) {
                    // do something
                }
                do while (
                    true) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    // do something
                } else {
                    // do something else
                }
                while (true) {
                    // do something
                }
                do while (true) {
                    // do something
                }
            }
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 9, "Unexpected spacing after \"(\""),
                LintViolation(8, 12, "Unexpected spacing after \"(\""),
                LintViolation(12, 15, "Unexpected spacing after \"(\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 3227 - Given an EOL comment before RPAR then do not remove the newline`() {
        val code =
            """
            fun main() {
                1.plus(1 // test
                ).plus(2 // test2
                ).plus(42)
            }
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function call with multiple arguments and an EOL comment on the last one then do not remove the newline before RPAR`() {
        val code =
            """
            val foo = fn(
                1, 2 // comment
            )
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function call with an EOL comment immediately after LPAR then do not remove the newline after it`() {
        val code =
            """
            val foo = fn( // comment
                1
            )
            """.trimIndent()
        spacingAroundParensRuleAssertThat(code).hasNoLintViolations()
    }
}
