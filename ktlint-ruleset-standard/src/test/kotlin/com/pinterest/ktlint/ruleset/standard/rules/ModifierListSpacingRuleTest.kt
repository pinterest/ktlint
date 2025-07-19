package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ModifierListSpacingRuleTest {
    private val modifierListSpacingRuleAssertThat = assertThatRule { ModifierListSpacingRule() }

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
                LintViolation(3, 33, "Single whitespace expected after modifier"),
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
                LintViolation(6, 12, "Single whitespace expected after modifier"),
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
                LintViolation(3, 11, "Single whitespace expected after modifier"),
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
                LintViolation(1, 13, "Single whitespace or newline expected after annotation"),
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
                LintViolation(1, 6, "Single newline expected after annotation"),
                LintViolation(3, 6, "Single newline expected after annotation"),
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

    @Test
    fun `Given a class with an annotated super type call entry`() {
        val code =
            """
            class Foo(
                bar: Bar,
            ) : @Unused
                FooBar()
            """.trimIndent()
        modifierListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 3065 - Given a context parameter on same line as function then wrap to new line after context parameter`() {
        val code =
            """
            class Bar {
                context(Foo) fun foo()
                context(_: Foo) fun foo()

                context(Foooooooooooooooo<Foo, Bar>) fun fooBar()
                context(_: Foooooooooooooooo<Foo, Bar>) fun fooBar()
            }
            """.trimIndent()
        val formattedCode =
            """
            class Bar {
                context(Foo)
                fun foo()
                context(_: Foo)
                fun foo()

                context(Foooooooooooooooo<Foo, Bar>)
                fun fooBar()
                context(_: Foooooooooooooooo<Foo, Bar>)
                fun fooBar()
            }
            """.trimIndent()
        modifierListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 17, "Single newline expected after context receiver list"),
                LintViolation(3, 20, "Single newline expected after context receiver list"),
                LintViolation(5, 41, "Single newline expected after context receiver list"),
                LintViolation(6, 44, "Single newline expected after context receiver list"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotation, a context parameter, and other modifiers on a single line in incorrect order`() {
        val code =
            """
            @Suppress("DEPRECATED") open context(_: Foo) public fun foo() {}
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("DEPRECATED")
            context(_: Foo)
            public open fun foo() {}
            """.trimIndent()
        modifierListSpacingRuleAssertThat(code)
            .addAdditionalRuleProvider { ModifierOrderRule() }
            .addAdditionalRuleProvider { AnnotationRule() }
            .hasLintViolation(1, 45, "Single newline expected after context receiver list")
            .isFormattedAs(formattedCode)
    }
}
