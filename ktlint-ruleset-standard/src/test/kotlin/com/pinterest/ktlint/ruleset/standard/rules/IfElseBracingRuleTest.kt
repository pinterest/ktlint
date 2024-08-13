package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class IfElseBracingRuleTest {
    private val multiLineIfElseRuleAssertThat = assertThatRule { IfElseBracingRule() }

    @ParameterizedTest(name = "CodeStyleValue: {0}")
    @EnumSource(
        value = CodeStyleValue::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["ktlint_official"],
    )
    fun `Given another code style then ktlint_official and IF with inconsistent bracing of the branches`(codeStyle: CodeStyleValue) {
        val code =
            """
            fun foo() {
                if (true) {
                    doSomething()
                } else doSomethingElse()
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to codeStyle)
            .hasNoLintViolations()
    }

    @ParameterizedTest(name = "CodeStyleValue: {0}")
    @EnumSource(
        value = CodeStyleValue::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["ktlint_official"],
    )
    fun `Given another code style then ktlint_official, and the rule has been enabled explicitly, and IF with inconsistent bracing of the branches`(
        codeStyle: CodeStyleValue,
    ) {
        val code =
            """
            fun foo() {
                if (true) {
                    doSomething()
                } else doSomethingElse()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    doSomething()
                } else {
                    doSomethingElse()
                }
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        multiLineIfElseRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to codeStyle)
            .withEditorConfigOverride(IF_ELSE_BRACING_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled)
            .hasLintViolation(4, 12, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces")
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given an IF without ELSE` {
        @Test
        fun `Given a then-with-braces and no else-branch then do not reformat`() {
            val code =
                """
                fun foo() {
                    if (true) {
                        doSomething()
                    }
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasNoLintViolations()
        }

        @Test
        fun `Given a then-without-braces and no else-branch then do not reformat`() {
            val code =
                """
                fun foo() {
                    if (true) doSomething()
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given an IF with ELSE` {
        @Test
        fun `Given a then-without-braces and an else-without-braces then do not reformat`() {
            val code =
                """
                fun foo() {
                    if (true)
                        doSomething()
                    else doSomethingElse()
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasNoLintViolations()
        }

        @Test
        fun `Given a then-with-braces and an else-without-braces then reformat`() {
            val code =
                """
                fun foo() {
                    if (true) {
                        doSomething()
                    } else doSomethingElse()
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    if (true) {
                        doSomething()
                    } else {
                        doSomethingElse()
                    }
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(4, 12, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a then-without-braces and an else-with-braces then reformat`() {
            val code =
                """
                fun foo() {
                    if (true) doSomething() else {
                        doSomethingElse()
                    }
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    if (true) {
                        doSomething()
                    } else {
                        doSomethingElse()
                    }
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(
                    2,
                    15,
                    "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces",
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a then-with-braces and an else-with-braces then do not reformat`() {
            val code =
                """
                fun foo() {
                    if (true) {
                        doSomething()
                    } else {
                        doSomethingElse()
                    }
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given an IF with ELSE-IF` {
        private val formattedCode =
            """
            fun foo(value: Int) {
                if (value > 0) {
                    doSomething()
                } else if (value < 0) {
                    doSomethingElse()
                } else {
                    doSomethingElse2()
                }
            }
            """.trimIndent()

        @Test
        fun `Given all branches without-braces then do not reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0)
                        doSomething()
                    else if (value < 0) doSomethingElse()
                    else doSomethingElse2()
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasNoLintViolations()
        }

        @Test
        fun `Given if { -- } else if -- else -- then reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0) {
                        doSomething()
                    } else if (value < 0)
                        doSomethingElse()
                    else
                        doSomethingElse2()
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolations(
                    LintViolation(5, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces"),
                    LintViolation(7, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given if -- else if { -- } else -- then reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0)
                        doSomething()
                    else if (value < 0) {
                        doSomethingElse()
                    } else
                        doSomethingElse2()
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolations(
                    LintViolation(3, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces"),
                    LintViolation(7, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given if -- else if -- else { -- } then reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0)
                        doSomething()
                    else if (value < 0)
                        doSomethingElse()
                    else {
                        doSomethingElse2()
                    }
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolations(
                    LintViolation(3, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces"),
                    LintViolation(5, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given if { -- } else if { -- } else -- then reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0) {
                        doSomething()
                    } else if (value < 0) {
                        doSomethingElse()
                    } else
                        doSomethingElse2()
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(7, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given if { -- } else if -- else { -- } then reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0) {
                        doSomething()
                    } else if (value < 0)
                        doSomethingElse()
                    else {
                        doSomethingElse2()
                    }
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(5, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given if -- else if { -- } else { -- } then reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0)
                        doSomething()
                    else if (value < 0) {
                        doSomethingElse()
                    } else {
                        doSomethingElse2()
                    }
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolation(3, 9, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given all branches with braces then do not reformat`() {
            val code =
                """
                fun foo(value: Int) {
                    if (value > 0) {
                        doSomething()
                    } else if (value < 0) {
                        doSomethingElse()
                    } else {
                        doSomethingElse2()
                    }
                }
                """.trimIndent()
            multiLineIfElseRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasNoLintViolations()
        }
    }

    @Test
    fun `Issue 2135 - Given ktlint_official code style and an if statement with and empty THEN block then do not throw a null pointer exception`() {
        val code =
            """
            val foo = if (false) else { bar() }
            """.trimIndent()
        val formattedCode =
            """
            val foo = if (false) {} else { bar() }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        multiLineIfElseRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
            .hasLintViolation(1, 22, "All branches of the if statement should be wrapped between braces if at least one branch is wrapped between braces")
            .isFormattedAs(formattedCode)
    }
}
