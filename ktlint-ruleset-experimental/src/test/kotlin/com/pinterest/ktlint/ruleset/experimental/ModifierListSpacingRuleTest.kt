package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ModifierListSpacingRuleTest {
    private val modifierListSpacingRuleAssertThat = ModifierListSpacingRule().assertThat()

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
        modifierListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 9, "Single whitespace expected after modifier"),
                LintViolation(3, 14, "Single whitespace expected after modifier"),
                LintViolation(3, 24, "Single whitespace expected after modifier"),
                LintViolation(3, 33, "Single whitespace expected after modifier")
            ).isFormattedAs(formattedCode)
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
        modifierListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 9, "Single whitespace expected after modifier"),
                LintViolation(4, 14, "Single whitespace expected after modifier"),
                LintViolation(5, 13, "Single whitespace expected after modifier"),
                LintViolation(6, 12, "Single whitespace expected after modifier")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1414 - Given a function with an annotation array omn a separate line then do not reformat`() {
        val code =
            """
            @Throws(RuntimeException::class)
            @[One Two Three]
            fun foo(): String {
                return "foo"
            }
            """.trimIndent()
        modifierListSpacingRuleAssertThat(code).hasNoLintViolations()
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
        modifierListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 15, "Single whitespace expected after modifier"),
                LintViolation(3, 11, "Single whitespace expected after modifier")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Annotation modifiers may be followed by a newline or a space`() {
        val code =
            """
            @Foo1 @Foo2
            class Bar {}
            """.trimIndent()
        modifierListSpacingRuleAssertThat(code).hasNoLintViolations()
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
        modifierListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 6, "Single whitespace or newline expected after annotation"),
                LintViolation(1, 13, "Single whitespace or newline expected after annotation")
            ).isFormattedAs(formattedCode)
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
        modifierListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 6, "Single whitespace or newline expected after annotation"),
                LintViolation(3, 6, "Single whitespace or newline expected after annotation")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given annotations that correctly indented then do no emit warnings`() {
        val code =
            """
            @Foo1
            @Foo2
            class Bar {}
            """.trimIndent()
        modifierListSpacingRuleAssertThat(code).hasNoLintViolations()
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
        modifierListSpacingRuleAssertThat(code).hasNoLintViolations()
    }
}
