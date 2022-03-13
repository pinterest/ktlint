package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KdocWrappingRuleTest {
    @Test
    fun `Given a single line KDoc comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /** Some KDoc comment */
            """.trimIndent()
        assertThat(KdocWrappingRule().lint(code)).isEmpty()
        assertThat(KdocWrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a multi line KDoc comment that start starts and end on a separate line then do not reformat`() {
        val code =
            """
            /**
             * Some KDoc comment
             */
            """.trimIndent()
        assertThat(KdocWrappingRule().lint(code)).isEmpty()
        assertThat(KdocWrappingRule().format(code)).isEqualTo(code)
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
        assertThat(KdocWrappingRule().lint(code)).containsExactly(
            LintError(1, 27, "kdoc-wrapping", "A KDoc comment may not be followed by any other element on that same line"),
            LintError(2, 27, "kdoc-wrapping", "A KDoc comment may not be followed by any other element on that same line"),
            LintError(3, 27, "kdoc-wrapping", "A KDoc comment may not be followed by any other element on that same line"),
            LintError(4, 27, "kdoc-wrapping", "A KDoc comment may not be followed by any other element on that same line")
        )
        assertThat(KdocWrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `A KDoc comment containing a newline should start on a new line but is not autocorrected`() {
        val code =
            """
            val foo = "foo" /** Some KDoc comment
                             * with a newline
                             */
            """.trimIndent()
        assertThat(KdocWrappingRule().lint(code)).containsExactly(
            LintError(1, 17, "kdoc-wrapping", "A KDoc comment after any other element on the same line must be separated by a new line")
        )
        assertThat(KdocWrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `A KDoc comment in between code elements on the same line should start and end on a new line but is only partially autocorrected`() {
        val code =
            """
            val foo /** Some KDoc comment */ = "foo"
            """.trimIndent()
        assertThat(KdocWrappingRule().lint(code)).containsExactly(
            LintError(1, 9, "kdoc-wrapping", "A KDoc comment in between other elements on the same line is disallowed")
        )
        assertThat(KdocWrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a KDoc comment containing a new line and the block is preceded and followed by other code elements then raise lint errors but do not autocorrect`() {
        val code =
            """
            val foo /**
            some KDoc comment
            */ = "foo"
            """.trimIndent()
        assertThat(KdocWrappingRule().lint(code)).containsExactly(
            LintError(1, 9, "kdoc-wrapping", "A KDoc comment starting on same line as another element and ending on another line before another element is disallowed")
        )
        assertThat(KdocWrappingRule().format(code)).isEqualTo(code)
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
        assertThat(KdocWrappingRule().lint(code)).containsExactly(
            LintError(2, 29, "kdoc-wrapping", "A KDoc comment may not be followed by any other element on that same line")
        )
        assertThat(KdocWrappingRule().format(code)).isEqualTo(formattedCode)
    }
}
