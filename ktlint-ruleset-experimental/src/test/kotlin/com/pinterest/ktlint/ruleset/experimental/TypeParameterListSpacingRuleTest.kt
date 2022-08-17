package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class TypeParameterListSpacingRuleTest {
    private val typeParameterListSpacingRuleAssertThat = assertThatRule { TypeParameterListSpacingRule() }

    @Test
    fun `Given a type parameter list followed by a comment then it can be ignored as it will be handled by the discouraged-comment-location rule`() {
        val code =
            """
            fun <T> // some-comment but it also applies to a block comment or KDoc
            foo1(t: T) = "some-result"
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a type parameter list in a function declaration not preceded or followed by a space then add a single white space`() {
        val code =
            """
            fun<T> foo1(t: T) = "some-result"
            fun <T>foo2(t: T) = "some-result"
            fun<T>foo3(t: T) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo1(t: T) = "some-result"
            fun <T> foo2(t: T) = "some-result"
            fun <T> foo3(t: T) = "some-result"
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 4, "Expected a single space"),
                LintViolation(2, 8, "Expected a single space"),
                LintViolation(3, 4, "Expected a single space"),
                LintViolation(3, 7, "Expected a single space"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type parameter list in a function declaration preceded or followed by too many spaces then the redundant spaces are removed`() {
        val code =
            """
            fun  <T> foo1(t: T) = "some-result"
            fun <T>  foo2(t: T) = "some-result"
            fun  <T>  foo3(t: T) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo1(t: T) = "some-result"
            fun <T> foo2(t: T) = "some-result"
            fun <T> foo3(t: T) = "some-result"
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 4, "Expected a single space"),
                LintViolation(2, 8, "Expected a single space"),
                LintViolation(3, 4, "Expected a single space"),
                LintViolation(3, 9, "Expected a single space"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type parameter list in a function declaration preceded or followed by a newline then replace with a single space`() {
        val code =
            """
            fun
            <T> foo1(t: T) = "some-result"
            fun <T>
            foo2(t: T) = "some-result"
            fun
            <T>
            foo3(t: T) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo1(t: T) = "some-result"
            fun <T> foo2(t: T) = "some-result"
            fun <T> foo3(t: T) = "some-result"
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 4, "Expected a single space instead of newline"),
                LintViolation(3, 8, "Expected a single space instead of newline"),
                LintViolation(5, 4, "Expected a single space instead of newline"),
                LintViolation(6, 4, "Expected a single space instead of newline"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a type parameter list and type reference not separated by space then add the missing space`() {
        val code =
            """
            fun main() {
                fun <T>List<T>.head() {}
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                fun <T> List<T>.head() {}
            }
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolation(2, 12, "Expected a single space")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class or interface definition with a type parameter list followed by multiple spaces then the redundant spaces are removed`() {
        val code =
            """
            class Bar<T>  {
                val bar: T? = null
            }
            interface foo<T>  {
                fun bar(t: T)
            }
            """.trimIndent()
        val formattedCode =
            """
            class Bar<T> {
                val bar: T? = null
            }
            interface foo<T> {
                fun bar(t: T)
            }
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Expected a single space"),
                LintViolation(4, 17, "Expected a single space"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class or interface definition with a type parameter list followed by a newline then replace the newline with a space`() {
        val code =
            """
            class Bar<T>
            {
                val bar: T? = null
            }
            interface foo<T>
            {
                fun bar(t: T)
            }
            """.trimIndent()
        val formattedCode =
            """
            class Bar<T> {
                val bar: T? = null
            }
            interface foo<T> {
                fun bar(t: T)
            }
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Expected a single space instead of newline"),
                LintViolation(5, 17, "Expected a single space instead of newline"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class definition with a type parameter list followed by one space and a compound constructor then do not remove the space`() {
        val code =
            """
            class Foo1<Bar> constructor() {}
            class Foo2<Bar> actual constructor() {}
            class Foo3<Bar> private constructor() {}
            class Foo4<Bar> internal constructor() {}
            class Foo5<Bar> @FooBar constructor() {}
            class Foo6<Bar> @FooBar internal constructor() {}
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class definition with a type parameter list followed by too many spaces and a compound constructor then replace with single space`() {
        val code =
            """
            class Foo1<Bar>  constructor() {}
            class Foo2<Bar>  actual constructor() {}
            class Foo3<Bar>  private constructor() {}
            class Foo4<Bar>  internal constructor() {}
            class Foo5<Bar>  @FooBar constructor() {}
            class Foo6<Bar>  @FooBar internal constructor() {}
            """.trimIndent()
        val formattedCode =
            """
            class Foo1<Bar> constructor() {}
            class Foo2<Bar> actual constructor() {}
            class Foo3<Bar> private constructor() {}
            class Foo4<Bar> internal constructor() {}
            class Foo5<Bar> @FooBar constructor() {}
            class Foo6<Bar> @FooBar internal constructor() {}
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 16, "Expected a single space"),
                LintViolation(2, 16, "Expected a single space"),
                LintViolation(3, 16, "Expected a single space"),
                LintViolation(4, 16, "Expected a single space"),
                LintViolation(5, 16, "Expected a single space"),
                LintViolation(6, 16, "Expected a single space"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class definition with a type parameter list followed by newlines and a compound constructor then replace with single space`() {
        val code =
            """
            class Foo1<Bar>
                constructor() {}
            class Foo2<Bar>
                actual constructor() {}
            class Foo3<Bar>
                private constructor() {}
            class Foo4<Bar>
                internal constructor() {}
            class Foo5<Bar>
                @FooBar constructor() {}
            class Foo6<Bar>
                @FooBar internal constructor() {}
            """.trimIndent()
        val formattedCode =
            """
            class Foo1<Bar> constructor() {}
            class Foo2<Bar> actual constructor() {}
            class Foo3<Bar> private constructor() {}
            class Foo4<Bar> internal constructor() {}
            class Foo5<Bar> @FooBar constructor() {}
            class Foo6<Bar> @FooBar internal constructor() {}
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 16, "Expected a single space instead of newline"),
                LintViolation(3, 16, "Expected a single space instead of newline"),
                LintViolation(5, 16, "Expected a single space instead of newline"),
                LintViolation(7, 16, "Expected a single space instead of newline"),
                LintViolation(9, 16, "Expected a single space instead of newline"),
                LintViolation(11, 16, "Expected a single space instead of newline"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class definition with a type parameter list followed by a parameter list then the redundant spaces are removed`() {
        val code =
            """
            class Bar <T> (val t: T)
            """.trimIndent()
        val formattedCode =
            """
            class Bar<T>(val t: T)
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 10, "No whitespace expected at this position"),
                LintViolation(1, 14, "No whitespace expected at this position"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Give a type parameter list containing a space after the opening angle bracket then remove it`() {
        val code =
            """
            fun < T> foo(): T {}
            class Bar< T>(val t: T)
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo(): T {}
            class Bar<T>(val t: T)
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 6, "No whitespace expected at this position"),
                LintViolation(2, 11, "No whitespace expected at this position"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Give a type parameter list containing a space before the closing angle bracket then remove it`() {
        val code =
            """
            fun <T > foo(): T {}
            class Bar<T >(val t: T)
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo(): T {}
            class Bar<T>(val t: T)
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 7, "No whitespace expected at this position"),
                LintViolation(2, 12, "No whitespace expected at this position"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a typealias definition with a type parameter list followed by a parameter list then the redundant spaces are removed`() {
        val code =
            """
            typealias Bar  <T>  = () -> T
            """.trimIndent()
        val formattedCode =
            """
            typealias Bar<T> = () -> T
            """.trimIndent()
        typeParameterListSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 14, "No whitespace expected at this position"),
                LintViolation(1, 19, "Expected a single space"),
            ).isFormattedAs(formattedCode)
    }
}
