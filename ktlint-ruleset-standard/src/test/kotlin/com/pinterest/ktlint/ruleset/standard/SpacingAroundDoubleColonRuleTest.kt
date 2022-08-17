package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundDoubleColonRuleTest {
    private val spacingAroundDoubleColonRuleAssertThat = assertThatRule { SpacingAroundDoubleColonRule() }

    @Test
    fun `Given some variable declarations holding a class literal reference`() {
        val code =
            """
            class Foo
            val foo1 = Foo::class
            val foo2 = Foo ::class
            val foo3 = Foo:: class
            val foo4 = Foo :: class
            val foo5 = Foo::
                class
            """.trimIndent()
        val formattedCode =
            """
            class Foo
            val foo1 = Foo::class
            val foo2 = Foo::class
            val foo3 = Foo::class
            val foo4 = Foo::class
            val foo5 = Foo::class
            """.trimIndent()
        spacingAroundDoubleColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 15, "Unexpected spacing before \"::\""),
                LintViolation(4, 17, "Unexpected spacing after \"::\""),
                LintViolation(5, 16, "Unexpected spacing around \"::\""),
                LintViolation(6, 17, "Unexpected spacing after \"::\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some callable reference expressions on a basic type`() {
        val code =
            """
            fun foo(string: String) = string == "foo"
            val predicateA: (String) -> Boolean = :: foo
            val predicateB: (String) -> Boolean =  ::foo
            val predicateC: (String) -> Boolean =  :: foo
            val predicateD: (String) -> Boolean = ::foo
            val predicateE: (String) -> Boolean =
                ::foo
            val predicateF: (String) -> Boolean = ::
                foo
            """.trimIndent()
        val formattedCode =
            """
            fun foo(string: String) = string == "foo"
            val predicateA: (String) -> Boolean = ::foo
            val predicateB: (String) -> Boolean = ::foo
            val predicateC: (String) -> Boolean = ::foo
            val predicateD: (String) -> Boolean = ::foo
            val predicateE: (String) -> Boolean =
                ::foo
            val predicateF: (String) -> Boolean = ::foo
            """.trimIndent()
        spacingAroundDoubleColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 41, "Unexpected spacing after \"::\""),
                LintViolation(3, 38, "Unexpected spacing before \"::\""),
                LintViolation(4, 40, "Unexpected spacing around \"::\""),
                LintViolation(8, 41, "Unexpected spacing after \"::\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some callable reference expressions on a complex type`() {
        val code =
            """
            val isEmptyStringList: List<String>.() -> Boolean = List<String> :: isEmpty
            val isNotEmptyStringList: List<String>.() -> Boolean = List<String>::isNotEmpty
            """.trimIndent()
        val formattedCode =
            """
            val isEmptyStringList: List<String>.() -> Boolean = List<String>::isEmpty
            val isNotEmptyStringList: List<String>.() -> Boolean = List<String>::isNotEmpty
            """.trimIndent()
        spacingAroundDoubleColonRuleAssertThat(code)
            .hasLintViolation(1, 66, "Unexpected spacing around \"::\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-statement invoking a callable reference expression`() {
        val code =
            """
            fun foo(string: String) = string == "foo"
            fun main() {
                if (true == ::foo.invoke("")) {
                    // do stuff
                }
            }
            """.trimIndent()
        spacingAroundDoubleColonRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some callable reference expressions passed as function argument`() {
        val code =
            """
            class Foo
            fun bar(foo: KFunction0<Foo>) = println(foo())
            fun main() {
                bar(::Foo)
                bar(:: Foo)
                bar(
                    ::
                    Foo
                )
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo
            fun bar(foo: KFunction0<Foo>) = println(foo())
            fun main() {
                bar(::Foo)
                bar(::Foo)
                bar(
                    ::Foo
                )
            }
            """.trimIndent()
        spacingAroundDoubleColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 11, "Unexpected spacing after \"::\""),
                LintViolation(7, 11, "Unexpected spacing after \"::\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some callable reference expressions passed in chained method`() {
        val code =
            """
            fun foo(string: String) = string == "foo"
            fun main() {
                listOf("foo", "bar", "foobar")
                    .map(String::length)
                    .map(String:: length)
                    .map(String ::length)
                    .map(String :: length)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(string: String) = string == "foo"
            fun main() {
                listOf("foo", "bar", "foobar")
                    .map(String::length)
                    .map(String::length)
                    .map(String::length)
                    .map(String::length)
            }
            """.trimIndent()
        spacingAroundDoubleColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 22, "Unexpected spacing after \"::\""),
                LintViolation(6, 20, "Unexpected spacing before \"::\""),
                LintViolation(7, 21, "Unexpected spacing around \"::\""),
            ).isFormattedAs(formattedCode)
    }
}
