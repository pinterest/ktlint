package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TypeParameterListSpacingRuleTest {
    @Test
    fun `Given a type parameter list followed by a comment then it can be ignored as it will be handled by the discouraged-comment-location rule`() {
        val code =
            """
            fun <T> // some-comment but it also applies to a block comment or KDoc
            foo1(t: T) = "some-result"
            """.trimIndent()
        assertThat(
            listOf(DiscouragedCommentLocationRule(), TypeParameterListSpacingRule()).lint(code)
        ).containsExactly(
            LintError(1, 9, "discouraged-comment-location", "No comment expected at this location")
        )
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 4, "type-parameter-list-spacing", "Expected a single space"),
            LintError(2, 8, "type-parameter-list-spacing", "Expected a single space"),
            LintError(3, 4, "type-parameter-list-spacing", "Expected a single space"),
            LintError(3, 7, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 4, "type-parameter-list-spacing", "Expected a single space"),
            LintError(2, 8, "type-parameter-list-spacing", "Expected a single space"),
            LintError(3, 4, "type-parameter-list-spacing", "Expected a single space"),
            LintError(3, 9, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 4, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(3, 8, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(5, 4, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(6, 4, "type-parameter-list-spacing", "Expected a single space instead of newline")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(2, 12, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 13, "type-parameter-list-spacing", "Expected a single space"),
            LintError(4, 17, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 13, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(5, 17, "type-parameter-list-spacing", "Expected a single space instead of newline")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).isEmpty()
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(code)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "type-parameter-list-spacing", "Expected a single space"),
            LintError(2, 16, "type-parameter-list-spacing", "Expected a single space"),
            LintError(3, 16, "type-parameter-list-spacing", "Expected a single space"),
            LintError(4, 16, "type-parameter-list-spacing", "Expected a single space"),
            LintError(5, 16, "type-parameter-list-spacing", "Expected a single space"),
            LintError(6, 16, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 16, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(3, 16, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(5, 16, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(7, 16, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(9, 16, "type-parameter-list-spacing", "Expected a single space instead of newline"),
            LintError(11, 16, "type-parameter-list-spacing", "Expected a single space instead of newline")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 10, "type-parameter-list-spacing", "No whitespace expected at this position"),
            LintError(1, 14, "type-parameter-list-spacing", "No whitespace expected at this position")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 6, "type-parameter-list-spacing", "No whitespace expected at this position"),
            LintError(2, 11, "type-parameter-list-spacing", "No whitespace expected at this position")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 7, "type-parameter-list-spacing", "No whitespace expected at this position"),
            LintError(2, 12, "type-parameter-list-spacing", "No whitespace expected at this position")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 14, "type-parameter-list-spacing", "No whitespace expected at this position"),
            LintError(1, 19, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
    }
}
