package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.CodeStyleValue
import io.github.ktlint.core.rule.engine.core.api.editorconfig.RuleExecution
import io.github.ktlint.core.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import io.github.ktlint.core.test.KtLintAssertThat.Companion.assertThatRule
import io.github.ktlint.core.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KdocCommentFormatRuleTest {
    private val kdocCommentFormatRuleAssertThat = assertThatRule { KdocCommentFormatRule() }

    @Nested
    inner class `Given a well formed KDoc comment` {
        @Test
        fun `Given a well formed single-line KDoc comment then do not reformat`() {
            val code =
                """
                /** A group of *members* */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a well formed multi-line KDoc comment then do not reformat`() {
            val code =
                """
                /**
                 * A group of *members*.
                 *
                 * This class has no useful logic; it's just a documentation example.
                 *
                 * @param T the type of a member in this group.
                 * @property name the name of this group.
                 * @constructor Creates an empty group.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a well formed KDoc comment containing markdown, a link, a tag and a code block then do not reformat`() {
            val code =
                """
                /**
                 * A [Foo] which contains **markdown**.
                 *
                 * ```
                 * val foo = Foo()
                 *     .bar()
                 * ```
                 *
                 * @see Foo.bar
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a well formed KDoc comment on a nested function then do not reformat`() {
            val code =
                """
                class Foo {
                    /**
                     * Some bar KDoc.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a well formed KDoc comment at the start of the file then do not reformat`() {
            val code =
                """
                /**
                 * A file level KDoc.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a well formed KDoc comment with tab indentation then do not reformat`() {
            val code =
                "class Foo {\n" +
                    "\t/**\n" +
                    "\t * Some bar KDoc.\n" +
                    "\t */\n" +
                    "\tfun bar() {}\n" +
                    "}\n"
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a malformed opening or closing` {
        @Test
        fun `Given a KDoc comment starting with an additional asterisk then do report but not autocorrect`() {
            val code =
                """
                /***
                 * A group of *members*.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 4, "A KDoc comment should start with '/**' and not with additional asterisks")
        }

        @Test
        fun `Given a multi-line KDoc comment for which the body starts on the opening line then do report but not autocorrect`() {
            val code =
                """
                /** A group of *members*.
                 * This class has no useful logic; it's just a documentation example.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    1,
                    4,
                    "Opening '/**' of a multi-line KDoc comment should not be followed by any other text on the same line",
                )
        }

        @Test
        fun `Given a multi-line KDoc comment for which the body ends on the closing line then do report but not autocorrect`() {
            val code =
                """
                /**
                 * A group of *members*.
                 * This class has no useful logic; it's just a documentation example. */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    3,
                    71,
                    "Closing '*/' of a multi-line KDoc comment should not be preceded by any other text on the same line",
                )
        }
    }

    @Nested
    inner class `Given a single-line KDoc comment with incorrect spacing` {
        @Test
        fun `Given a single-line KDoc comment missing the leading space then reformat`() {
            val code =
                """
                /**A group of *members* */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /** A group of *members* */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(1, 1, "A single-line KDoc comment should start with '/** ' and end with ' */'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single-line KDoc comment missing the trailing space then reformat`() {
            val code =
                """
                /** A group of *members**/
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /** A group of *members* */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(1, 1, "A single-line KDoc comment should start with '/** ' and end with ' */'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single-line KDoc comment with multiple leading and trailing spaces then reformat`() {
            val code =
                """
                /**   A group of *members*   */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /** A group of *members* */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(1, 1, "A single-line KDoc comment should start with '/** ' and end with ' */'")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a multi-line KDoc comment with misaligned lines` {
        @Test
        fun `Given a leading asterisk which is not aligned with the opening then reformat`() {
            val code =
                """
                /**
                   * A group of *members*.
                 */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /**
                 * A group of *members*.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(2, 1, "Leading asterisk should align with the second asterisk of the KDoc opening '/**'")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a closing marker which is not aligned with the leading asterisks then reformat`() {
            val code =
                """
                /**
                 * A group of *members*.
                   */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /**
                 * A group of *members*.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(3, 1, "Closing '*/' should align with the leading asterisks of the KDoc comment")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a nested KDoc comment with a misaligned leading asterisk then reformat`() {
            val code =
                """
                class Foo {
                    /**
                       * Some bar KDoc.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            val formattedCode =
                """
                class Foo {
                    /**
                     * Some bar KDoc.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(3, 1, "Leading asterisk should align with the second asterisk of the KDoc opening '/**'")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a missing leading asterisk` {
        @Test
        fun `Given a continuation line without a leading asterisk which is indented at least as much as required then reformat`() {
            val code =
                """
                class Bar {
                    /**
                     * It starts well, but for some reason the line below does not start with the star sign, and is properly indented.
                        Some indented text not starting with a star.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            val formattedCode =
                """
                class Bar {
                    /**
                     * It starts well, but for some reason the line below does not start with the star sign, and is properly indented.
                     *    Some indented text not starting with a star.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(4, 9, "Each line of a multi-line KDoc comment should start with a leading asterisk")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a continuation line without a leading asterisk which is not indented at all then only report the violation`() {
            val code =
                """
                class Foo {
                    /**
                     * It starts well, but for some reason the line below does not start with the star sign, and is not properly indented.
                Some unindented text not starting with a star.
                     */
                    fun foo() {}
                }
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    4,
                    1,
                    "Each line of a multi-line KDoc comment should start with a leading asterisk",
                )
        }

        @Test
        fun `Given a continuation line without a leading asterisk which is indented with a tab then only report the violation`() {
            val code =
                "class Foo {\n" +
                    "    /**\n" +
                    "     * It starts well, but the line below does not start with the star sign.\n" +
                    "\t\tSome text indented with tabs.\n" +
                    "     */\n" +
                    "    fun foo() {}\n" +
                    "}\n"
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    4,
                    3,
                    "Each line of a multi-line KDoc comment should start with a leading asterisk",
                )
        }

        @Test
        fun `Given two consecutive continuation lines without a leading asterisk then reformat the safe cases individually`() {
            val code =
                """
                class Bar {
                    /**
                     * It starts well.
                        Some indented text not starting with a star.
                            and more indented text not starting with a star.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            val formattedCode =
                """
                class Bar {
                    /**
                     * It starts well.
                     *    Some indented text not starting with a star.
                     *        and more indented text not starting with a star.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 9, "Each line of a multi-line KDoc comment should start with a leading asterisk"),
                    LintViolation(5, 13, "Each line of a multi-line KDoc comment should start with a leading asterisk"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given an empty line inside a multi-line KDoc comment` {
        @Test
        fun `Given a well formed empty line then do not reformat`() {
            val code =
                """
                /**
                 * A group of *members*.
                 *
                 * More text.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given an empty line with trailing whitespace then reformat`() {
            val code =
                "/**\n" +
                    " * A group of *members*.\n" +
                    " * \n" +
                    " * More text.\n" +
                    " */\n" +
                    "class Foo\n"
            val formattedCode =
                "/**\n" +
                    " * A group of *members*.\n" +
                    " *\n" +
                    " * More text.\n" +
                    " */\n" +
                    "class Foo\n"
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(3, 3, "An empty line in a KDoc comment should not contain trailing whitespace after the leading asterisk")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a leading asterisk not followed by a single space` {
        @Test
        fun `Given a leading asterisk directly followed by content then reformat`() {
            val code =
                """
                /**
                 *A group of *members*.
                 */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /**
                 * A group of *members*.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .hasLintViolation(2, 3, "A leading asterisk in a KDoc comment should be followed by a single space")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given the rule is run together with other KDoc related rules` {
        @Test
        fun `Given a well formed KDoc comment then running with kdoc-wrapping and kdoc does not conflict`() {
            val code =
                """
                /**
                 * A group of *members*.
                 */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .addAdditionalRuleProviders({ KdocWrappingRule() }, { KdocRule() })
                .hasNoLintViolations()
        }

        @Test
        fun `Given a malformed KDoc comment then autocorrection is stable and does not loop`() {
            val code =
                """
                class Bar {
                    /**
                       * It starts well.
                        Some indented text not starting with a star.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            val formattedCode =
                """
                class Bar {
                    /**
                     * It starts well.
                     *    Some indented text not starting with a star.
                     */
                    fun bar() {}
                }
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code).isFormattedAs(formattedCode)
            // A second format run should not produce any further changes.
            kdocCommentFormatRuleAssertThat(formattedCode).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given the code style is not ktlint_official` {
        @Test
        fun `Given a malformed KDoc comment and code style android_studio then the rule can still be run explicitly`() {
            val code =
                """
                /**A group of *members* */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                /** A group of *members* */
                class Foo
                """.trimIndent()
            kdocCommentFormatRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to CodeStyleValue.android_studio)
                .withEditorConfigOverride(KDOC_COMMENT_FORMAT_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled)
                .hasLintViolation(1, 1, "A single-line KDoc comment should start with '/** ' and end with ' */'")
                .isFormattedAs(formattedCode)
        }
    }
}
