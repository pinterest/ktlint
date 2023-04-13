package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CommentWrappingRuleTest {
    private val commentWrappingRuleAssertThat = assertThatRule { CommentWrappingRule() }

    @Test
    fun `Given a single line block comment then replace it with an EOL comment`() {
        val code =
            """
            fun bar() {
                /* Some comment */
            }
            """.trimIndent()
        val formattedCode =
            """
            fun bar() {
                // Some comment
            }
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
            .hasLintViolation(2, 5, "A single line block comment must be replaced with an EOL comment")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multi line block comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /*
             * Some comment
             */
            """.trimIndent()
        commentWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given a block comment followed by a code element on the same line` {
        @Test
        fun `Given a comment followed by a property and separated with space`() {
            val code =
                """
                /* Some comment */ val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                /* Some comment */
                val foo = "foo"
                """.trimIndent()
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 20, "A block comment may not be followed by any other element on that same line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a comment followed by a property but not separated with space`() {
            val code =
                """
                /* Some comment */val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                /* Some comment */
                val foo = "foo"
                """.trimIndent()
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 19, "A block comment may not be followed by any other element on that same line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a comment followed by a function and separated with space`() {
            val code =
                """
                /* Some comment */ fun foo() = "foo"
                """.trimIndent()
            val formattedCode =
                """
                /* Some comment */
                fun foo() = "foo"
                """.trimIndent()
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 20, "A block comment may not be followed by any other element on that same line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a comment followed by a function but not separated with space`() {
            val code =
                """
                /* Some comment */fun foo() = "foo"
                """.trimIndent()
            val formattedCode =
                """
                /* Some comment */
                fun foo() = "foo"
                """.trimIndent()
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 19, "A block comment may not be followed by any other element on that same line")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given some code code followed by a block comment on the same line` {
        @Test
        fun `Given a comment followed by a property and separated with space`() {
            val code =
                """
                val foo = "foo" /* Some comment */
                """.trimIndent()
            val formattedCode =
                """
                val foo = "foo" // Some comment
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 17, "A single line block comment after a code element on the same line must be replaced with an EOL comment")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a comment followed by a property but not separated with space`() {
            val code =
                """
                val foo = "foo"/* Some comment */
                """.trimIndent()
            val formattedCode =
                """
                val foo = "foo" // Some comment
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 16, "A single line block comment after a code element on the same line must be replaced with an EOL comment")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a comment followed by a function and separated with space`() {
            val code =
                """
                fun foo() = "foo" /* Some comment */
                """.trimIndent()
            val formattedCode =
                """
                fun foo() = "foo" // Some comment
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 19, "A single line block comment after a code element on the same line must be replaced with an EOL comment")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a comment followed by a function but not separated with space`() {
            val code =
                """
                fun foo() = "foo"/* Some comment */
                """.trimIndent()
            val formattedCode =
                """
                fun foo() = "foo" // Some comment
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            commentWrappingRuleAssertThat(code)
                .hasLintViolation(1, 18, "A single line block comment after a code element on the same line must be replaced with an EOL comment")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a block comment containing a newline which is preceded by another element on the same line then raise lint error but do not autocorrect`() {
        val code =
            """
            val foo = "foo" /* Some comment
                             * with a newline
                             */
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        commentWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 17, "A block comment after any other element on the same line must be separated by a new line")
    }

    @Test
    fun `Given a block comment that does not contain a newline and which is after some code om the same line is changed to an EOL comment`() {
        val code =
            """
            val foo = "foo" /* Some comment */
            """.trimIndent()
        val formattedCode =
            """
            val foo = "foo" // Some comment
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        commentWrappingRuleAssertThat(code)
            .hasLintViolation(1, 17, "A single line block comment after a code element on the same line must be replaced with an EOL comment")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a block comment in between code elements on the same line then raise lint error but do not autocorrect`() {
        val code =
            """
            val foo /* some comment */ = "foo"
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        commentWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "A block comment in between other elements on the same line is disallowed")
    }

    @Test
    fun `Given a block comment containing a new line and the block is preceded and followed by other code elements then raise lint errors but do not autocorrect`() {
        val code =
            """
            val foo /*
            some comment
            */ = "foo"
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        commentWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "A block comment starting on same line as another element and ending on another line before another element is disallowed")
    }

    @Test
    fun `Given a block comment which is indented then keep that indent when wrapping the line`() {
        val code =
            """
            fun bar() {
                /* Some comment */ val foo = "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun bar() {
                /* Some comment */
                val foo = "foo"
            }
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .hasLintViolation(2, 24, "A block comment may not be followed by any other element on that same line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line block containing a block comment then do not reformat`() {
        val code =
            """
            val foo = { /* no-op */ }
            """.trimIndent()
        commentWrappingRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
            .hasNoLintViolations()
    }
}
