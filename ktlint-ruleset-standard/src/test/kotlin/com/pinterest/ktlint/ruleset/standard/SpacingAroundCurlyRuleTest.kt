package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SpacingAroundCurlyRuleTest {
    private val spacingAroundCurlyRuleAssertThat = assertThatRule { SpacingAroundCurlyRule() }

    @Test
    fun `Given some function without spaces around the curly braces`() {
        val code =
            """
            fun foo(){println("bar")}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() { println("bar") }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 10, "Missing spacing around \"{\""),
                LintViolation(1, 25, "Missing spacing before \"}\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some if statement without space around the curly braces`() {
        val code =
            """
            val foo = if(true){0}else{1}
            """.trimIndent()
        val formattedCode =
            """
            val foo = if(true) { 0 } else { 1 }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 19, "Missing spacing around \"{\""),
                LintViolation(1, 21, "Missing spacing around \"}\""),
                LintViolation(1, 26, "Missing spacing around \"{\""),
                LintViolation(1, 28, "Missing spacing before \"}\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an empty block then no space is required before the closing curly brace`() {
        val code =
            """
            fun foo() = {}
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an anonymous function`() {
        val code =
            """
            val foo1 = bar1@{ }
            val foo2 = bar2@{}
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a function with a lambda parameter value`() {
        val code =
            """
            val foo1 = foo({a -> a})
            val foo2 = foo({ })
            val foo3 = foo({})
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = foo({ a -> a })
            val foo2 = foo({ })
            val foo3 = foo({})
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolations(
                // TODO: Col offset is not correct
                LintViolation(1, 17, "Missing spacing after \"{\""),
                LintViolation(1, 23, "Missing spacing before \"}\""),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class Lamda {
        @Test
        fun `Given a lambda missing space around opening curly brace`() {
            val code =
                """
                val foo = find{true }?.bar
                """.trimIndent()
            val formattedCode =
                """
                val foo = find { true }?.bar
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(1, 15, "Missing spacing around \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a lambda missing space before opening curly brace`() {
            val code =
                """
                val foo = find{ true }?.bar
                """.trimIndent()
            val formattedCode =
                """
                val foo = find { true }?.bar
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(1, 15, "Missing spacing before \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a lambda missing space after opening curly brace`() {
            val code =
                """
                val foo = find {true }?.bar
                """.trimIndent()
            val formattedCode =
                """
                val foo = find { true }?.bar
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(1, 17, "Missing spacing after \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a lambda missing space before closing curly brace`() {
            val code =
                """
                val foo = find { true}?.bar
                """.trimIndent()
            val formattedCode =
                """
                val foo = find { true }?.bar
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(1, 22, "Missing spacing before \"}\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a lambda having unexpected space after closing curly brace`() {
            val code =
                """
                val foo = find { true } ?.bar
                """.trimIndent()
            val formattedCode =
                """
                val foo = find { true }?.bar
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(1, 23, "Unexpected space after \"}\"")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Issue 596 - Given a lambda as array index then no space is allowed between the closing curly brace and closing bracket`() {
        val code =
            """
            fun <T> Array<T>.getFoo(): T = this[this.count { it == "foo" } ]
            """.trimIndent()
        val formattedCode =
            """
            fun <T> Array<T>.getFoo(): T = this[this.count { it == "foo" }]
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            // TODO: Col offset is not correct
            .hasLintViolation(1, 62, "Unexpected space after \"}\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given curly braces inside a string template`() {
        val code =
            """
            data class Foo(val bar: String)
            val foo = Foo("foobar").also {
                println("Bar = ${'$'}{it.bar}") // In real code the $ would not have been escaped
            }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given some try-catch statements`() {
        val code =
            """
            fun foo(bar: String) = {}
            val foo1 = try{foo("bar")}catch (e: Exception){}
            val foo2 = {bar: String -> try{foo(bar)}catch (e: Exception){}}
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: String) = {}
            val foo1 = try { foo("bar") } catch (e: Exception) {}
            val foo2 = { bar: String -> try { foo(bar) } catch (e: Exception) {} }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 15, "Missing spacing around \"{\""),
                LintViolation(2, 26, "Missing spacing around \"}\""),
                LintViolation(2, 47, "Missing spacing before \"{\""),
                // TODO: Col offset is not correct
                LintViolation(3, 13, "Missing spacing after \"{\""),
                LintViolation(3, 31, "Missing spacing around \"{\""),
                LintViolation(3, 40, "Missing spacing around \"}\""),
                LintViolation(3, 61, "Missing spacing before \"{\""),
                // TODO: Col offset is not correct
                LintViolation(3, 63, "Missing spacing after \"}\""),
                LintViolation(3, 63, "Missing spacing before \"}\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some scope functions`() {
        val code =
            """
            val foo = "a".let{}.apply{}.also{}
            """.trimIndent()
        val formattedCode =
            """
            val foo = "a".let {}.apply {}.also {}
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 18, "Missing spacing before \"{\""),
                LintViolation(1, 26, "Missing spacing before \"{\""),
                LintViolation(1, 33, "Missing spacing before \"{\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda resulting in a map from which the value is extract via an index lookup then no space is allowed before the index`() {
        val code =
            """
            val foo1 = l.groupBy { it }[key]
            val foo2 = l.groupBy { it } [key]
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = l.groupBy { it }[key]
            val foo2 = l.groupBy { it }[key]
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolation(2, 27, "Unexpected space after \"}\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a lambda resulting in object of a class having an invoke function which is called then no space is allowed before call of the invoke function`() {
        val code =
            """
            val foo1 = l.groupBy { it }(key)
            val foo2 = l.groupBy { it } (key)
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = l.groupBy { it }(key)
            val foo2 = l.groupBy { it }(key)
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolation(2, 27, "Unexpected space after \"}\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a double colon operator after instantiation of an object of the Any class then no space is allowed after the closing curly brace`() {
        val code =
            """
            val foo1 = object : Any() {}::class.java.classLoader
            val foo2 = object : Any() {} ::class.java.classLoader
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = object : Any() {}::class.java.classLoader
            val foo2 = object : Any() {}::class.java.classLoader
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolation(2, 28, "Unexpected space after \"}\"")
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class OpeningCurlyBraceOnSameLineAsControllingStatement {
        @Test
        fun `Given a class with the opening curly brace on a new line`() {
            val code =
                """
                class A
                {
                }
                """.trimIndent()
            val formattedCode =
                """
                class A {
                }
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unexpected newline before \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a class with a companion object having the opening curly brace on a new line`() {
            val code =
                """
                class A {
                    companion object
                    {
                    }
                }
                """.trimIndent()
            val formattedCode =
                """
                class A {
                    companion object {
                    }
                }
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(3, 5, "Unexpected newline before \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an interface with the opening curly brace on a new line`() {
            val code =
                """
                interface A
                {
                }
                """.trimIndent()
            val formattedCode =
                """
                interface A {
                }
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unexpected newline before \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an if-else-statement with the opening curly brace on a new line`() {
            val code =
                """
                val foo = if (true)
                {
                } else
                {
                }
                """.trimIndent()
            val formattedCode =
                """
                val foo = if (true) {
                } else {
                }
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected newline before \"{\""),
                    LintViolation(4, 1, "Unexpected newline before \"{\""),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a do-while-statement with the opening curly brace on a new line`() {
            val code =
                """
                fun main() {
                    do
                    {
                    } while (true)
                }
                """.trimIndent()
            val formattedCode =
                """
                fun main() {
                    do {
                    } while (true)
                }
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(3, 5, "Unexpected newline before \"{\"")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some function with the opening curly brace on a new line`() {
            val code =
                """
                fun foo()
                {
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                }
                """.trimIndent()
            spacingAroundCurlyRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unexpected newline before \"{\"")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given some block with a label`() {
        val code =
            """
            class A { private val shouldEjectBlock = block@ { (pathProgress ?: return@block false) >= 0.85 } }
            """.trimIndent()
        val formattedCode =
            """
            class A { private val shouldEjectBlock = block@{ (pathProgress ?: return@block false) >= 0.85 } }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolation(1, 49, "Unexpected space before \"{\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a new line after lambda return type`() {
        val code =
            """
            fun magicNumber1(): () -> Int = { 37 }
            fun magicNumber2(): () -> Int =
                { 42 }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an EOL comment before opening curly brace of function`() {
        val code =
            """
            class Foo1()// a comment (originally) not preceded by a space
            {
            }
            class Foo2() // a comment (originally) preceded by a space
            {
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo1() { // a comment (originally) not preceded by a space
            }
            class Foo2() { // a comment (originally) preceded by a space
            }
            """.trimIndent()
        spacingAroundCurlyRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected newline before \"{\""),
                LintViolation(5, 1, "Unexpected newline before \"{\""),
            ).isFormattedAs(formattedCode)
    }
}
