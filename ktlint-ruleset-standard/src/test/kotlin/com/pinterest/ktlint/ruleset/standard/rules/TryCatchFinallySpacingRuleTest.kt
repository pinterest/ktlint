package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TryCatchFinallySpacingRuleTest {
    private val tryCatchRuleAssertThat = KtLintAssertThat.assertThatRule { TryCatchFinallySpacingRule() }

    @Test
    fun `Given try - catch which is formatted correctly then do not report violation`() {
        val code =
            """
            val foo = try {
                // do something
            } catch (exception: Exception) {
                "catch"
            }
            """.trimIndent()
        tryCatchRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given try - finally which is formatted correctly then do not report violation`() {
        val code =
            """
            val foo = try {
                // do something
            } finally {
                // do something else
            }
            """.trimIndent()
        tryCatchRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
            .hasNoLintViolations()
    }

    @Test
    fun `Given try - catch - finally which is formatted correctly then do not report violation`() {
        val code =
            """
            val foo = try {
                // do something
            } catch (exception: Exception) {
                "catch"
            } finally {
                // do something else
            }
            """.trimIndent()
        tryCatchRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
            .hasNoLintViolations()
    }

    @Nested
    inner class `Given a catch not preceded by a single space` {
        val formattedCode =
            """
            val foo = try {
                "try"
            } catch (exception: Exception) {
                "catch"
            }
            """.trimIndent()

        @Test
        fun `Given try - catch without whitespace before catch then reformat`() {
            val code =
                """
                val foo = try {
                    "try"
                }catch (exception: Exception) {
                    "catch"
                }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(3, 2, "A single space is required before 'catch'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given try - catch with multiple spaces before catch then reformat`() {
            val code =
                """
                val foo = try {
                    "try"
                }  catch (exception: Exception) {
                    "catch"
                }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(3, 4, "A single space is required before 'catch'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given try - catch with newline before catch then reformat`() {
            val code =
                """
                val foo = try {
                    "try"
                }
                catch (exception: Exception) {
                    "catch"
                }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(4, 1, "A single space is required before 'catch'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given try - catch on single line then reformat`() {
            val code =
                """
                val foo = try { "try" } catch (exception: Exception) { "catch" }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 16, "Expected a newline after '{'"),
                    LintViolation(1, 23, "Expected a newline before '}'"),
                    LintViolation(1, 55, "Expected a newline after '{'"),
                    LintViolation(1, 64, "Expected a newline before '}'"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a finally not preceded by a single space` {
        val formattedCode =
            """
            val foo = try {
                "try"
            } finally {
                "finally"
            }
            """.trimIndent()

        @Test
        fun `Given try - finally without whitespace before finally then reformat`() {
            val code =
                """
                val foo = try {
                    "try"
                }finally {
                    "finally"
                }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(3, 2, "A single space is required before 'finally'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given try - finally with multiple spaces before finally then reformat`() {
            val code =
                """
                val foo = try {
                    "try"
                }  finally {
                    "finally"
                }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(3, 4, "A single space is required before 'finally'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given try - finally with newline before finally then reformat`() {
            val code =
                """
                val foo = try {
                    "try"
                }
                finally {
                    "finally"
                }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(4, 1, "A single space is required before 'finally'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given try - finally on single line then reformat`() {
            val code =
                """
                val foo = try { "try" } finally { "finally" }
                """.trimIndent()
            tryCatchRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolations(
                    LintViolation(1, 16, "Expected a newline after '{'"),
                    LintViolation(1, 23, "Expected a newline before '}'"),
                    LintViolation(1, 34, "Expected a newline after '{'"),
                    LintViolation(1, 45, "Expected a newline before '}'"),
                ).isFormattedAs(formattedCode)
        }
    }
}
