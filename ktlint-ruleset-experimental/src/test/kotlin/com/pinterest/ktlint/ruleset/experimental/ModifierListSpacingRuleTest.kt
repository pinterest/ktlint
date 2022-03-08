package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModifierListSpacingRuleTest {
    @Test
    fun `Given a function preceded by multiple modifiers separated by multiple space then remove redundant spaces`() {
        val code =
            """
            abstract  class Foo {
                @Throws(RuntimeException::class)
                protected  abstract  suspend  fun execute()
            }
            """.trimIndent()
        val formattedCode =
            """
            abstract class Foo {
                @Throws(RuntimeException::class)
                protected abstract suspend fun execute()
            }
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).containsExactly(
            LintError(1, 9, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(3, 14, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(3, 24, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(3, 33, "modifier-list-spacing", "Single whitespace expected after modifier")
        )
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function preceded by multiple modifiers separated by newlines then remove redundant spaces`() {
        val code =
            """
            abstract
            class Foo {
                @Throws(RuntimeException::class)
                protected
                abstract
                suspend
                fun execute()
            }
            """.trimIndent()
        val formattedCode =
            """
            abstract class Foo {
                @Throws(RuntimeException::class)
                protected abstract suspend fun execute()
            }
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).containsExactly(
            LintError(1, 9, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(4, 14, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(5, 13, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(6, 12, "modifier-list-spacing", "Single whitespace expected after modifier")
        )
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a modifier list followed by multiple space then remove the redundant spaces`() {
        val code =
            """
            fun foo(vararg  bar) = "some-result"
            fun foo(
                vararg
                bar
            ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(vararg bar) = "some-result"
            fun foo(
                vararg bar
            ) = "some-result"
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).containsExactly(
            LintError(1, 15, "modifier-list-spacing", "Single whitespace expected after modifier"),
            LintError(3, 11, "modifier-list-spacing", "Single whitespace expected after modifier")
        )
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Annotation modifiers may be followed by a newline or a space`() {
        val code =
            """
            @Foo1 @Foo2
            class Bar {}
            """.trimIndent()
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Annotation modifiers may not be followed by multiple spaces`() {
        val code =
            """
            @Foo1  @Foo2  class Bar {}
            """.trimIndent()
        val formattedCode =
            """
            @Foo1 @Foo2 class Bar {}
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).containsExactly(
            LintError(1, 6, "modifier-list-spacing", "Single whitespace or newline expected after annotation"),
            LintError(1, 13, "modifier-list-spacing", "Single whitespace or newline expected after annotation")
        )
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Annotation modifiers may not be followed by multiple newlines`() {
        val code =
            """
            @Foo1

            @Foo2

            class Bar {}
            """.trimIndent()
        val formattedCode =
            """
            @Foo1
            @Foo2
            class Bar {}
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).containsExactly(
            LintError(1, 6, "modifier-list-spacing", "Single whitespace or newline expected after annotation"),
            LintError(3, 6, "modifier-list-spacing", "Single whitespace or newline expected after annotation")
        )
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given annotations that correctly indented then do no emit warnings`() {
        val code =
            """
            @Foo1
            @Foo2
            class Bar {}
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).isEmpty()
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given annotations followed by comments that correctly indented then do no emit warnings`() {
        val code =
            """
            @Foo1 // some-comment
            @Foo2
            /**
              * Some comment
              */
            @Foo3
            class Bar {}
            """.trimIndent()
        assertThat(ModifierListSpacingRule().lint(code)).isEmpty()
        assertThat(ModifierListSpacingRule().format(code)).isEqualTo(code)
    }
}
