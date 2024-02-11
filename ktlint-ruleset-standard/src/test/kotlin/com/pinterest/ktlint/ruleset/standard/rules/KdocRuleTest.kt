package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KdocRuleTest {
    private val kdocRuleAssertThat = assertThatRule { KdocRule() }

    @ParameterizedTest(name = "foo: {0}")
    @ValueSource(
        strings = [
            "class Foo",
            "enum class Foo",
            "interface Foo",
            "fun foo()",
            "val foo: Foo",
            "object foo: Foo",
            "typealias FooBar = (Foo) -> Bar",
        ],
    )
    fun `A KDoc is allowed`(annotatedConstruct: String) {
        val code =
            """
            /**
             * Some Foo Kdoc
             */
             $annotatedConstruct
            """.trimIndent()
        kdocRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `A KDoc is allowed on a class parameter`() {
        val code =
            """
            class Foo(
                /**
                 * Some bar Kdoc
                 */
                 val bar: Bar
             )
            """.trimIndent()
        kdocRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `A KDoc is allowed on a secondary class constructor`() {
        val code =
            """
            class Foo {
                /**
                 * Some bar KDoc
                 */
                constructor() : this()
             }
            """.trimIndent()
        kdocRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `A KDoc is allowed on a class property`() {
        val code =
            """
            class Foo {
                /**
                 * Some bar KDoc
                 */
                val bar: Bar
             }
            """.trimIndent()
        kdocRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `A KDoc is allowed on an enum entry`() {
        val code =
            """
            enum class Foo {
                /**
                 * Some bar Kdoc
                 */
                 BAR
             }
            """.trimIndent()
        kdocRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `A dangling KDoc is not allowed`() {
        val code =
            """
            /**
             * Some Foo Kdoc
             */
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(1, 1, "A dangling toplevel KDoc is not allowed")
    }

    @Test
    fun `A KDoc comment in between code elements on the same line should start and end on a new line but is not autocorrected`() {
        val code =
            """
            val foo /** Some KDoc comment */ = "foo"
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(1, 9, "A KDoc is allowed only at start of 'property'")
    }

    @Test
    fun `Given a KDoc comment containing a new line and the block is preceded and followed by other code elements then raise lint errors but do not autocorrect`() {
        val code =
            """
            val foo /**
            some KDoc comment
            */ = "foo"
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(1, 9, "A KDoc is allowed only at start of 'property'")
    }

    @Test
    fun `Given a kdoc as child of type argument list`() {
        val code =
            """
            val fooBar: FooBar<
                /** some comment */
                Foo, Bar>
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed inside 'type_argument_list'")
    }

    @Test
    fun `Given a kdoc inside a type projection`() {
        val code =
            """
            fun Foo<out /** some comment */ Any>.foo() {}
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(1, 13, "A KDoc is not allowed inside 'type_projection'")
    }

    @Test
    fun `Given a kdoc as child of type parameter list`() {
        val code =
            """
            class Foo<
                /** some comment */
                Bar>
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed inside 'type_parameter_list'")
    }

    @Test
    fun `Given a kdoc inside a type parameter`() {
        val code =
            """
            class Foo<in /** some comment */ Bar>
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(1, 14, "A KDoc is not allowed inside 'type_parameter'")
    }

    @Test
    fun `Given a kdoc as child of value argument list`() {
        val code =
            """
            val foo = foo(
                /** some comment */
            )
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed inside 'value_argument_list'")
    }

    @Test
    fun `Given a kdoc inside a value argument`() {
        val code =
            """
            val foo = foo(
                bar /** some comment */ = "bar"
            )
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 9, "A KDoc is not allowed inside 'value_argument'")
    }

    @Test
    fun `Given a comment inside value parameter ast node`() {
        val code =
            """
            class Foo1(
                val bar:
                    // some comment
                    Bar
            )
            class Foo2(
                val bar: /* some comment */ Bar
            )
            class Foo3(
                val bar: /** some comment */ Bar
            )
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(10, 14, "A KDoc is allowed only at start of 'value_parameter'")
    }

    @Test
    fun `Given a kdoc as child of value parameter list`() {
        val code =
            """
            class Foo(
                /** some comment */
            )
            """.trimIndent()
        kdocRuleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, "A KDoc is not allowed inside 'value_parameter_list'")
    }
}
