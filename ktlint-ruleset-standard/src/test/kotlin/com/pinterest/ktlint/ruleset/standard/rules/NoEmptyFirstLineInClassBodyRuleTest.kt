package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class NoEmptyFirstLineInClassBodyRuleTest {
    private val noEmptyFirstLineInClassBodyRuleAssertThat = KtLintAssertThat.assertThatRule { NoEmptyFirstLineInClassBodyRule() }

    @Test
    fun `Given a class body that does not start with a blank line`() {
        val code =
            """
            class Foo {
                val foo = "foo"
            }
            """.trimIndent()
        noEmptyFirstLineInClassBodyRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a class without body`() {
        val code =
            """
            class Foo

            val foo = "foo"
            """.trimIndent()
        noEmptyFirstLineInClassBodyRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a class body starting with a blank line`() {
        val code =
            """
            class Foo {

                val foo = "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                val foo = "foo"
            }
            """.trimIndent()
        noEmptyFirstLineInClassBodyRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(2, 1, "Class body should not start with blank line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a nested class body starting with a blank line`() {
        val code =
            """
            class Bar {
                class Foo {

                    val foo = "foo"
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Bar {
                class Foo {
                    val foo = "foo"
                }
            }
            """.trimIndent()
        noEmptyFirstLineInClassBodyRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(3, 1, "Class body should not start with blank line")
            .isFormattedAs(formattedCode)
    }

    @ParameterizedTest(name = "Code style: {0}")
    @EnumSource(
        value = CodeStyleValue::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["ktlint_official"],
    )
    fun `Given a non-ktlint-official code style and a class body starting with a blank line then do not return a lint violation`(
        codeStyleValue: CodeStyleValue,
    ) {
        val code =
            """
            class Foo {

                val foo = "foo"
            }
            """.trimIndent()
        noEmptyFirstLineInClassBodyRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to codeStyleValue)
            .hasNoLintViolations()
    }
}
