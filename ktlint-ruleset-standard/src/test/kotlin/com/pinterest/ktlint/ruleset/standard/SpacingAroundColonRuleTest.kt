package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundColonRuleTest {
    private val spacingAroundColonRuleAssertThat = assertThatRule { SpacingAroundColonRule() }

    @Test
    fun `Given some child class declarations`() {
        val code =
            """
            class A:B
            class A2 : B2
            """.trimIndent()
        val formattedCode =
            """
            class A : B
            class A2 : B2
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(1, 8, "Missing spacing around \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some interfaces`() {
        val code =
            """
            interface D
            interface C: D
            interface C2 : D
            """.trimIndent()
        val formattedCode =
            """
            interface D
            interface C : D
            interface C2 : D
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(2, 12, "Missing spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some variable declarations inside a function`() {
        val code =
            """
            fun main() {
                var x:Boolean
                var y: Boolean
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                var x: Boolean
                var y: Boolean
            }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(2, 11, "Missing spacing after \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some object creation as parameter value in function without a space before the colon`() {
        val code =
            """
            interface Foo
            fun main() {
                bar(object: Foo {})
            }
            """.trimIndent()
        val formattedCode =
            """
            interface Foo
            fun main() {
                bar(object : Foo {})
            }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(3, 15, "Missing spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some annotation use-site targets`() {
        // https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets
        val code =
            """
            @file:JvmName("Foo")
            class Example(@field:Ann val foo: String, @get:Ann val bar: String)
            class Example {
                @set:[Inject VisibleForTesting]
                public var collaborator: Collaborator
            }
            fun @receiver:Fancy String.myExtension() { }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a double colon operator then do not add spaces`() {
        val code =
            """
            fun main() {
                val x = Foo::class
            }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun testLint4() {
        val code =
            """
            class A {
                @Deprecated("") @Throws(IOException::class, SecurityException::class)
                protected abstract fun <T> f(
                    @Nullable thing: String, things: List<T>): Runnable where T : Runnable, T: Closeable
            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
                @Deprecated("") @Throws(IOException::class, SecurityException::class)
                protected abstract fun <T> f(
                    @Nullable thing: String, things: List<T>): Runnable where T : Runnable, T : Closeable
            }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(4, 82, "Missing spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some classes missing spaces around the colon before the super type`() {
        val code =
            """
            open class C1(string: String)
            class C2(string: String):C1(string)
            class C3 constructor(string: String):C1(string)
            class C4:C1 {
                constructor():this("")
                constructor(param: String):super(param)

                val c1 = object:C1("foo") { }
                val c2 = C4()
            }
            """.trimIndent()
        val formattedCode =
            """
            open class C1(string: String)
            class C2(string: String) : C1(string)
            class C3 constructor(string: String) : C1(string)
            class C4 : C1 {
                constructor() : this("")
                constructor(param: String) : super(param)

                val c1 = object : C1("foo") { }
                val c2 = C4()
            }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 25, "Missing spacing around \":\""),
                LintViolation(3, 37, "Missing spacing around \":\""),
                LintViolation(4, 9, "Missing spacing around \":\""),
                LintViolation(5, 18, "Missing spacing around \":\""),
                LintViolation(6, 31, "Missing spacing around \":\""),
                LintViolation(8, 20, "Missing spacing around \":\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type constraint missing a space before the colon`() {
        val code =
            """
            fun <T> max(a: T, b: T) where T: Comparable<T>
            """.trimIndent()
        val formattedCode =
            """
            fun <T> max(a: T, b: T) where T : Comparable<T>
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(1, 32, "Missing spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function declaration with an unexpected space before the colon before the return type`() {
        val code =
            """
            fun foo() : String = "foo"
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String = "foo"
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(1, 11, "Unexpected spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function parameter with an unexpected space before the colon`() {
        val code =
            """
            fun foo(bar : String) {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: String) {}
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            // TODO: Offset col is not correct.
            .hasLintViolation(1, 13, "Unexpected spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a generic function type parameter without space before the colon`() {
        val code =
            """
            fun <T: Any> trueIdentity(value: T): T = value
            """.trimIndent()
        val formattedCode =
            """
            fun <T : Any> trueIdentity(value: T): T = value
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolation(1, 7, "Missing spacing before \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun testFormatEOF() {
        val code =
            """
            class X
                : Y,
                Z
            class A // comment
                : B
            class A /*

            */
                : B
            val xmlFormatter
                : String = ""
            """.trimIndent()
        val formattedCode =
            """
            class X :
                Y,
                Z
            class A : // comment
                B
            class A : /*

            */
                B
            val xmlFormatter: String =
                ""
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 8, "Unexpected newline before \":\""),
                LintViolation(4, 19, "Unexpected newline before \":\""),
                LintViolation(8, 3, "Unexpected newline before \":\""),
                LintViolation(10, 17, "Unexpected newline before \":\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1057 - Given some declaration with an unexpected newline before the colon`() {
        val code =
            """
            fun test() {
                val v1
                    : Int = 1

                val v2 // comment
                    : Int = 1

                val v3
                    // comment
                    : Int = 1

                fun f1()
                    : Int = 1

                fun f2() // comment
                    : Int = 1

                fun f3()
                    // comment
                    : Int = 1

                fun g1()
                    : Int {
                    return 1
                }

                fun g2() // comment
                    : Int {
                    return 1
                }

                fun g3()
                    // comment
                    : Int {
                    return 1
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test() {
                val v1: Int =
                    1

                val v2: Int = // comment
                    1

                val v3: Int =
                    // comment
                    1

                fun f1(): Int =
                    1

                fun f2(): Int = // comment
                    1

                fun f3(): Int =
                    // comment
                    1

                fun g1(): Int {
                    return 1
                }

                fun g2(): Int { // comment
                    return 1
                }

                fun g3(): Int {
                    // comment
                    return 1
                }
            }
            """.trimIndent()
        spacingAroundColonRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 11, "Unexpected newline before \":\""),
                LintViolation(5, 22, "Unexpected newline before \":\""),
                LintViolation(9, 19, "Unexpected newline before \":\""),
                LintViolation(12, 13, "Unexpected newline before \":\""),
                LintViolation(15, 24, "Unexpected newline before \":\""),
                LintViolation(19, 19, "Unexpected newline before \":\""),
                LintViolation(22, 13, "Unexpected newline before \":\""),
                LintViolation(27, 24, "Unexpected newline before \":\""),
                LintViolation(33, 19, "Unexpected newline before \":\""),
            ).isFormattedAs(formattedCode)
    }
}
