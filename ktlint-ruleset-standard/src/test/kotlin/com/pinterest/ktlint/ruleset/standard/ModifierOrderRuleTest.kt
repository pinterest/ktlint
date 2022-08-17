package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ModifierOrderRuleTest {
    private val modifierOrderRuleAssertThat = assertThatRule { ModifierOrderRule() }

    @Test
    fun `Given class modifiers in incorrect order`() {
        val code =
            """
            abstract @Deprecated open class A { // open is here for test purposes only, otherwise it's redundant
            }
            """.trimIndent()
        val formattedCode =
            """
            @Deprecated open abstract class A { // open is here for test purposes only, otherwise it's redundant
            }
            """.trimIndent()
        modifierOrderRuleAssertThat(code)
            .hasLintViolation(1, 1, "Incorrect modifier order (should be \"@Annotation... open abstract\")")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given function modifiers in an open abstract class`() {
        val code =
            """
            abstract class A {
                open protected val v = ""
                open suspend internal fun f(v: Any): Any = ""
                lateinit protected var lv: String
            }
            """.trimIndent()
        val formattedCode =
            """
            abstract class A {
                protected open val v = ""
                internal open suspend fun f(v: Any): Any = ""
                protected lateinit var lv: String
            }
            """.trimIndent()
        modifierOrderRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 5, "Incorrect modifier order (should be \"protected open\")"),
                LintViolation(3, 5, "Incorrect modifier order (should be \"internal open suspend\")"),
                LintViolation(4, 5, "Incorrect modifier order (should be \"protected lateinit\")"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with a tailrec and protected modifier in incorrect order`() {
        val code =
            """
            class A {
                tailrec protected fun foo(bar: String): String = foo(bar.substringBeforeLast("\n"))
            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
                protected tailrec fun foo(bar: String): String = foo(bar.substringBeforeLast("\n"))
            }
            """.trimIndent()
        modifierOrderRuleAssertThat(code)
            .hasLintViolation(2, 5, "Incorrect modifier order (should be \"protected tailrec\")")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some overrides with modifiers in incorrect order`() {
        val code =
            """
            class Bar : Foo() {
                override public val v = ""
                suspend override fun f(v: Any): Any = ""
                tailrec override fun foo(bar: String): String = foo(bar.substringBeforeLast(" "))
                override @Annotation fun getSomething() = ""
                suspend @Annotation override public @Woohoo(data = "woohoo") fun doSomething() = ""
            }
            """.trimIndent()
        val formattedCode =
            """
            class Bar : Foo() {
                public override val v = ""
                override suspend fun f(v: Any): Any = ""
                override tailrec fun foo(bar: String): String = foo(bar.substringBeforeLast(" "))
                @Annotation override fun getSomething() = ""
                @Annotation @Woohoo(data = "woohoo") public override suspend fun doSomething() = ""
            }
            """.trimIndent()
        modifierOrderRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 5, "Incorrect modifier order (should be \"public override\")"),
                LintViolation(3, 5, "Incorrect modifier order (should be \"override suspend\")"),
                LintViolation(4, 5, "Incorrect modifier order (should be \"override tailrec\")"),
                LintViolation(5, 5, "Incorrect modifier order (should be \"@Annotation... override\")"),
                LintViolation(6, 5, "Incorrect modifier order (should be \"@Annotation... public override suspend\")"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a suspend function with annotations and modifiers in incorrect order`() {
        val code =
            """
            @A
            @B(v = [
                "foo",
                "baz",
                "bar"
            ])
            @C
            suspend public fun returnsSomething() = ""
            """.trimIndent()
        val formattedCode =
            """
            @A
            @B(v = [
                "foo",
                "baz",
                "bar"
            ])
            @C
            public suspend fun returnsSomething() = ""
            """.trimIndent()
        modifierOrderRuleAssertThat(code)
            .hasLintViolation(1, 1, "Incorrect modifier order (should be \"@Annotation... public suspend\")")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a companion object with a variable having modifiers in incorrect order`() {
        val code =
            """
            class Foo {
                companion object {
                   const internal val V = ""
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                companion object {
                   internal const val V = ""
                }
            }
            """.trimIndent()
        modifierOrderRuleAssertThat(code)
            .hasLintViolation(3, 8, "Incorrect modifier order (should be \"internal const\")")
            .isFormattedAs(formattedCode)
    }
}
