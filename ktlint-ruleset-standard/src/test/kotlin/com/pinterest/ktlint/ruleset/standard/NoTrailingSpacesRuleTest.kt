package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.SPACE
import org.junit.jupiter.api.Test

class NoTrailingSpacesRuleTest {
    private val noTrailingSpacesRuleAssertThat = assertThatRule { NoTrailingSpacesRule() }

    @Test
    fun `Given some statements followed by a trailing space then do return lint errors`() {
        val code =
            """
            fun main() {$SPACE
                val a = 1$SPACE
            $SPACE
                $SPACE
            }$SPACE
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                val a = 1


            }
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Trailing space(s)"),
                LintViolation(2, 14, "Trailing space(s)"),
                LintViolation(3, 1, "Trailing space(s)"),
                LintViolation(4, 1, "Trailing space(s)"),
                LintViolation(5, 2, "Trailing space(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some trailing spaces inside EOL comments then do return lint errors`() {
        val code =
            """
            //$SPACE
            // Some comment$SPACE
            class Foo {
                //$SPACE$SPACE
                // Some comment$SPACE$SPACE
                fun bar() = "foobar"
            }
            """.trimIndent()
        val formattedCode =
            """
            //
            // Some comment
            class Foo {
                //
                // Some comment
                fun bar() = "foobar"
            }
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 3, "Trailing space(s)"),
                LintViolation(2, 16, "Trailing space(s)"),
                LintViolation(4, 7, "Trailing space(s)"),
                LintViolation(5, 20, "Trailing space(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some trailing spaces inside block comments then do return lint errors`() {
        val code =
            """
            /*$SPACE
             * Some comment$SPACE
             */
            class Foo {
                /*$SPACE$SPACE
                 * Some comment$SPACE$SPACE
                 */
                fun bar() = "foobar"
            }
            """.trimIndent()
        val formattedCode =
            """
            /*
             * Some comment
             */
            class Foo {
                /*
                 * Some comment
                 */
                fun bar() = "foobar"
            }
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 3, "Trailing space(s)"),
                LintViolation(2, 16, "Trailing space(s)"),
                LintViolation(5, 7, "Trailing space(s)"),
                LintViolation(6, 20, "Trailing space(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some trailing spaces inside KDoc then do return lint errors`() {
        val code =
            """
            /**$SPACE
             * Some comment$SPACE
             *$SPACE
             */
            class Foo {
                /**$SPACE$SPACE
                 * Some comment$SPACE$SPACE
                 *$SPACE$SPACE
                 */
                fun bar() = "foobar"
            }
            """.trimIndent()
        val formattedCode =
            """
            /**
             * Some comment
             *
             */
            class Foo {
                /**
                 * Some comment
                 *
                 */
                fun bar() = "foobar"
            }
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 4, "Trailing space(s)"),
                LintViolation(2, 16, "Trailing space(s)"),
                LintViolation(3, 3, "Trailing space(s)"),
                LintViolation(6, 8, "Trailing space(s)"),
                LintViolation(7, 20, "Trailing space(s)"),
                LintViolation(8, 7, "Trailing space(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1334 - trailing spaces should not delete indent of the next line`() {
        val code =
            """
            class Foo {
                // something
            $SPACE
                /**
                 * Some KDoc
                 */
                val bar: String
            $SPACE
                val foo = "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                // something

                /**
                 * Some KDoc
                 */
                val bar: String

                val foo = "foo"
            }
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Trailing space(s)"),
                LintViolation(8, 1, "Trailing space(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1376 - trailing spaces should not delete blank line inside kdoc`() {
        val code =
            """
            /**
             Paragraph 1 which should be followed by a blank line.


             Paragraph 2 which should have a blank line before it.
             */
            class MyClass
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1376 - trailing spaces should be removed from blank line inside kdoc`() {
        val code =
            """
            /**
             Paragraph 1 which should be followed by a blank line.
             $SPACE
             $SPACE$SPACE
             Paragraph 2 which should have a blank line before it.
             */
            class MyClass
            """.trimIndent()
        val formattedCode =
            """
            /**
             Paragraph 1 which should be followed by a blank line.


             Paragraph 2 which should have a blank line before it.
             */
            class MyClass
            """.trimIndent()
        noTrailingSpacesRuleAssertThat(code)
            .hasLintViolation(3, 1, "Trailing space(s)")
            .isFormattedAs(formattedCode)
    }
}
