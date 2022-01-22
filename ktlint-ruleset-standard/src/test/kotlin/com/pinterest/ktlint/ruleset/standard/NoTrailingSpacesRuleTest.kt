package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoTrailingSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoTrailingSpacesRule().lint("fun main() {\n    val a = 1\n\n \n} "))
            .isEqualTo(
                listOf(
                    LintError(4, 1, "no-trailing-spaces", "Trailing space(s)"),
                    LintError(5, 2, "no-trailing-spaces", "Trailing space(s)")
                )
            )
    }

    @Test
    fun testFormat() {
        assertThat(NoTrailingSpacesRule().format("fun main() {\n    val a = 1 \n  \n \n} "))
            .isEqualTo("fun main() {\n    val a = 1\n\n\n}")
    }

    @Test
    fun `trailing spaces inside line comments`() {
        val code =
            """
            //${" "}
            // Some comment${" "}
            class Foo {
                //${" "}${" "}
                // Some comment${" "}${" "}
                fun bar() = "foobar"
            }
            """.trimIndent()
        val expectedCode =
            """
            //
            // Some comment
            class Foo {
                //
                // Some comment
                fun bar() = "foobar"
            }
            """.trimIndent()

        assertThat(
            NoTrailingSpacesRule().format(code)
        ).isEqualTo(expectedCode)
        assertThat(
            NoTrailingSpacesRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(1, 3, "no-trailing-spaces", "Trailing space(s)"),
                LintError(2, 16, "no-trailing-spaces", "Trailing space(s)"),
                LintError(4, 7, "no-trailing-spaces", "Trailing space(s)"),
                LintError(5, 20, "no-trailing-spaces", "Trailing space(s)")
            )
        )
    }

    @Test
    fun `format trailing spaces inside block comments`() {
        val code =
            """
            /*${" "}
             * Some comment${" "}
             */
            class Foo {
                /*${" "}${" "}
                 * Some comment${" "}${" "}
                 */
                fun bar() = "foobar"
            }
            """.trimIndent()
        val expectedCode =
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

        assertThat(
            NoTrailingSpacesRule().format(code)
        ).isEqualTo(expectedCode)
        assertThat(
            NoTrailingSpacesRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(1, 3, "no-trailing-spaces", "Trailing space(s)"),
                LintError(2, 16, "no-trailing-spaces", "Trailing space(s)"),
                LintError(5, 7, "no-trailing-spaces", "Trailing space(s)"),
                LintError(6, 20, "no-trailing-spaces", "Trailing space(s)")
            )
        )
    }

    @Test
    fun `trailing spaces inside KDoc`() {
        val code =
            """
            /**${" "}
             * Some comment${" "}
             *${" "}
             */
            class Foo {
                /**${" "}${" "}
                 * Some comment${" "}${" "}
                 *${" "}${" "}
                 */
                fun bar() = "foobar"
            }
            """.trimIndent()
        val codeExpected =
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

        assertThat(
            NoTrailingSpacesRule().format(code)
        ).isEqualTo(codeExpected)
        assertThat(
            NoTrailingSpacesRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(1, 4, "no-trailing-spaces", "Trailing space(s)"),
                LintError(2, 16, "no-trailing-spaces", "Trailing space(s)"),
                LintError(3, 3, "no-trailing-spaces", "Trailing space(s)"),
                LintError(6, 8, "no-trailing-spaces", "Trailing space(s)"),
                LintError(7, 20, "no-trailing-spaces", "Trailing space(s)"),
                LintError(8, 7, "no-trailing-spaces", "Trailing space(s)")
            )
        )
    }
}
