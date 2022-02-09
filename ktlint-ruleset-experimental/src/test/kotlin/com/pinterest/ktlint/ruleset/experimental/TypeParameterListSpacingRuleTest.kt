package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TypeParameterListSpacingRuleTest {
    /* Enable once https://github.com/pinterest/ktlint/pull/1365 is merged
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
    */

    @Test
    fun `Given a type parameter list not followed by whitespace then add a single white space`() {
        val code =
            """
            fun <T>foo(t: T) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 8, "type-parameter-list-spacing", "Expected a single space")
        )
        assertThat(TypeParameterListSpacingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a type parameter list followed by multiple spaces then the redundant spaces are removed`() {
        val code =
            """
            fun <T>  foo(t: T) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun <T> foo(t: T) = "some-result"
            """.trimIndent()
        assertThat(TypeParameterListSpacingRule().lint(code)).containsExactly(
            LintError(1, 8, "type-parameter-list-spacing", "Expected a single space")
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
}
