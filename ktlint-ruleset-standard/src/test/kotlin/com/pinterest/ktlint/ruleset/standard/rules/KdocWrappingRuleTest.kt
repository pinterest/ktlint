package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class KdocWrappingRuleTest {
    private val kdocWrappingRuleAssertThat = assertThatRule { KdocWrappingRule() }

    @Test
    fun `Given a single line KDoc comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /** Some KDoc comment */
            """.trimIndent()
        kdocWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multi line KDoc comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /**
             * Some KDoc comment
             */
            """.trimIndent()
        kdocWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a KDoc comment followed by a code element on the same line as the KDoc comment ended then split the elements with a new line`() {
        val code =
            """
            /** Some KDoc comment 1 */ val foo1 = "foo1"
            /** Some KDoc comment 2 */val foo2 = "foo2"
            /** Some KDoc comment 3 */ fun foo3() = "foo3"
            /** Some KDoc comment 4 */fun foo4() = "foo4"
            """.trimIndent()
        val formattedCode =
            """
            /** Some KDoc comment 1 */
            val foo1 = "foo1"
            /** Some KDoc comment 2 */
            val foo2 = "foo2"
            /** Some KDoc comment 3 */
            fun foo3() = "foo3"
            /** Some KDoc comment 4 */
            fun foo4() = "foo4"
            """.trimIndent()
        kdocWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, "A KDoc comment may not be followed by any other element on that same line"),
                LintViolation(2, 27, "A KDoc comment may not be followed by any other element on that same line"),
                LintViolation(3, 27, "A KDoc comment may not be followed by any other element on that same line"),
                LintViolation(4, 27, "A KDoc comment may not be followed by any other element on that same line"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `A KDoc comment containing a newline should start on a new line but is not autocorrected`() {
        val code =
            """
            val foo = "foo" /** Some KDoc comment
                             * with a newline
                             */
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        kdocWrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 17, "A KDoc comment after any other element on the same line must be separated by a new line")
    }

    @Test
    fun `Given a KDoc comment which is indented then keep that indent when wrapping the line`() {
        val code =
            """
            fun bar() {
                /** Some KDoc comment */ val foo = "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun bar() {
                /** Some KDoc comment */
                val foo = "foo"
            }
            """.trimIndent()
        kdocWrappingRuleAssertThat(code)
            .hasLintViolation(2, 29, "A KDoc comment may not be followed by any other element on that same line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2535 - Given a class with a KDoc on the primary constructor instead of on the class name`() {
        val code =
            // Code sample below contains a code smell. The KDoc is at the wrong position.
            """
            class ClassA
                /**
                 * some comment
                 */(paramA: String)
            """.trimIndent()
        val formattedCode =
            // Code sample below contains a code smell. The KDoc is at the wrong position. The formatted code is not intended to show
            // that this code is well formatted according to good coding practices. It merely is used to validate that the newline is
            // inserted at the correct position in the AST so that no exception will be thrown in the ParameterListWrappingRule.
            """
            class ClassA
                /**
                 * some comment
                 */
                (paramA: String)
            """.trimIndent()
        kdocWrappingRuleAssertThat(code)
            .addAdditionalRuleProvider { ParameterListWrappingRule() }
            .hasLintViolation(4, 8, "A KDoc comment may not be followed by any other element on that same line")
            .isFormattedAs(formattedCode)
    }
}
