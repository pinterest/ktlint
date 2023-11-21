package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoSingleLineBlockCommentRuleTest {
    private val noSingleLineBlockCommentRuleAssertThat =
        assertThatRuleBuilder { NoSingleLineBlockCommentRule() }
            .addAdditionalRuleProvider { CommentWrappingRule() }
            .build()

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
        noSingleLineBlockCommentRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasLintViolation(2, 5, "Replace the block comment with an EOL comment")
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
        noSingleLineBlockCommentRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single line block comment that is to be wrapped before replacing it with an EOL comment`() {
        val code =
            """
            /* Some comment */ val foo = "foo"
            """.trimIndent()
        val formattedCode =
            """
            // Some comment
            val foo = "foo"
            """.trimIndent()
        noSingleLineBlockCommentRuleAssertThat(code)
            .hasLintViolationForAdditionalRule(1, 20, "A block comment may not be followed by any other element on that same line")
            // Can not check for the lint violation below as it will only be thrown while formatting with comment wrapping
            //   A single line block comment must be replaced with an EOL comment
            .isFormattedAs(formattedCode)
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
            noSingleLineBlockCommentRuleAssertThat(code)
                .hasLintViolation(1, 17, "Replace the block comment with an EOL comment")
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
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            noSingleLineBlockCommentRuleAssertThat(code)
                .hasLintViolation(1, 16, "Replace the block comment with an EOL comment")
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
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            noSingleLineBlockCommentRuleAssertThat(code)
                .hasLintViolation(1, 19, "Replace the block comment with an EOL comment")
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
            noSingleLineBlockCommentRuleAssertThat(code)
                .hasLintViolation(1, 18, "Replace the block comment with an EOL comment")
                .isFormattedAs(formattedCode)
        }
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
        noSingleLineBlockCommentRuleAssertThat(code)
            .hasLintViolation(1, 17, "Replace the block comment with an EOL comment")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line block comment in between code elements on the same line does not raise a lint error`() {
        val code =
            """
            val foo /* some comment */ = "foo"
            """.trimIndent()
        noSingleLineBlockCommentRuleAssertThat(code).hasNoLintViolationsExceptInAdditionalRules()
    }

    @Test
    fun `Given a single line block containing a block comment then do not reformat`() {
        val code =
            """
            val foo = { /* no-op */ }
            """.trimIndent()
        noSingleLineBlockCommentRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official)
            .hasNoLintViolations()
    }
}
