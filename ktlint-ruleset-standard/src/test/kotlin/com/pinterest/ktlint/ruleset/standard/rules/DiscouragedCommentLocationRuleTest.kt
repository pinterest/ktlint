package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DiscouragedCommentLocationRuleTest {
    private val discouragedCommentLocationRuleAssertThat = assertThatRule { DiscouragedCommentLocationRule() }

    @Nested
    inner class `Given a comment after a type parameter then report a discouraged comment location` {
        @Test
        fun `Given an EOL comment`() {
            val code =
                """
                fun <T> // some comment
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
        }

        @Test
        fun `Given an EOL comment on a newline`() {
            val code =
                """
                fun <T>
                // some comment
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
        }

        @Test
        fun `Given a block comment`() {
            val code =
                """
                fun <T> /* some comment */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
        }

        @Test
        fun `Given a block comment on a newline`() {
            val code =
                """
                fun <T>
                /* some comment */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
        }

        @Test
        fun `Given a KDOC comment`() {
            val code =
                """
                fun <T> /** some comment */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 9, "No comment expected at this location")
        }

        @Test
        fun `Given a KDOC comment on a newline`() {
            val code =
                """
                fun <T>
                /**
                  * some comment
                  */
                foo(t: T) = "some-result"
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 1, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a comment between IF CONDITION and THEN` {
        @Test
        fun `Given EOL comment on same line as CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true) // some comment
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "No comment expected at this location")
        }

        @Test
        fun `Given EOL comment on line below CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true)
                        // some comment
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 9, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on same line as CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true) /* some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on line below CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true)
                        /* some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 9, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on same line as CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true) /** some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 15, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on line below CONDITION`() {
            val code =
                """
                fun foo() {
                    if (true)
                        /** some comment */
                        bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 9, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a comment between THEN and ELSE` {
        @Test
        fun `Given EOL comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo() // some comment
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 15, "No comment expected at this location")
        }

        @Test
        fun `Given EOL comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    // some comment
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 5, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo() /* some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 15, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    /* some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 5, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo() /** some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(3, 15, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    /** some comment */
                    else bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 5, "No comment expected at this location")
        }
    }

    @Nested
    inner class `Given a comment between ELSE KEYWORD and ELSE block` {
        @Test
        fun `Given EOL comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else // some comment
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 10, "No comment expected at this location")
        }

        @Test
        fun `Given EOL comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else
                    // some comment
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(5, 5, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else /* some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 10, "No comment expected at this location")
        }

        @Test
        fun `Given block comment on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else
                    /* some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(5, 5, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on same line as THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else /** some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(4, 10, "No comment expected at this location")
        }

        @Test
        fun `Given KDOC on line below THEN`() {
            val code =
                """
                fun foobar() {
                    if (true)
                        foo()
                    else
                    /** some comment */
                    if (false) bar()
                }
                """.trimIndent()
            discouragedCommentLocationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(5, 5, "No comment expected at this location")
        }
    }
}
