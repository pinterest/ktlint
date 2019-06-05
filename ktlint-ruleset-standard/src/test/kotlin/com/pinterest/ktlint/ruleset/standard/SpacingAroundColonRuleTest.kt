package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundColonRuleTest {

    @Test
    fun testLint() {
        assertThat(
            SpacingAroundColonRule().lint(
                """
                class A:B
                class A2 : B2
                fun main() {
                    var x:Boolean
                    var y: Boolean
                    call(object: DefaultConsumer(channel) { })
                }
                interface D
                interface C: D
                interface C2 : D
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 8, "colon-spacing", "Missing spacing around \":\""),
                LintError(4, 11, "colon-spacing", "Missing spacing after \":\""),
                LintError(6, 16, "colon-spacing", "Missing spacing before \":\""),
                LintError(9, 12, "colon-spacing", "Missing spacing before \":\"")
            )
        )
        assertThat(
            SpacingAroundColonRule().lint(
                """
                @file:JvmName("Foo")
                class Example(@field:Ann val foo: String, @get:Ann val bar: String)
                class Example {
                    @set:[Inject VisibleForTesting]
                    public var collaborator: Collaborator
                }
                fun @receiver:Fancy String.myExtension() { }
                """.trimIndent()
            )
        ).isEmpty()
        assertThat(
            SpacingAroundColonRule().lint(
                """
                fun main() {
                    val x = Foo::class
                }
                """.trimIndent()
            )
        ).isEmpty()
        assertThat(
            SpacingAroundColonRule().lint(
                """
                class A {
                    constructor() : this("")
                    constructor(s: String) {
                    }
                }
                class A {
                    @Deprecated("") @Throws(IOException::class, SecurityException::class)
                    protected abstract fun <T> f(
                        @Nullable thing: String, things: List<T>): Runnable where T : Runnable, T: Closeable
                }
                class A : B {
                    constructor(): super()
                    constructor(param: String) : super(param)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(9, 82, "colon-spacing", "Missing spacing before \":\""),
                LintError(12, 18, "colon-spacing", "Missing spacing before \":\"")
            )
        )
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundColonRule().format(
                """
                class A:B
                fun main() {
                    var x:Boolean
                    var y: Boolean
                }
                interface D
                interface C: D
                class F(param: String):D(param)
                class F2 constructor(param: String): D3(param)
                class F3 : D3 {
                    constructor():super()
                    constructor(param: String): super(param)

                    val x = object:D3 { }
                }
                fun <T> max(a: T, b: T) where T: Comparable<T>
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A : B
            fun main() {
                var x: Boolean
                var y: Boolean
            }
            interface D
            interface C : D
            class F(param: String) : D(param)
            class F2 constructor(param: String) : D3(param)
            class F3 : D3 {
                constructor() : super()
                constructor(param: String) : super(param)

                val x = object : D3 { }
            }
            fun <T> max(a: T, b: T) where T : Comparable<T>
            """.trimIndent()
        )
    }

    @Test
    fun testLintMethod() {
        assertThat(
            SpacingAroundColonRule().lint(
                """
                fun main() : String = "duck"
                fun duck(): String = "main"
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 12, "colon-spacing", "Unexpected spacing before \":\"")
            )
        )
    }

    @Test
    fun testFormatMethod() {
        assertThat(
            SpacingAroundColonRule().format(
                """
                fun main() : String = "duck"
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun main(): String = "duck"
            """.trimIndent()
        )
    }

    @Test
    fun testLintMethodParams() {
        assertThat(
            SpacingAroundColonRule().lint(
                """
                fun identity(value: String): String = value
                fun unformattedIdentity(value : String): String = value
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 31, "colon-spacing", "Unexpected spacing before \":\"")
            )
        )
    }

    @Test
    fun testFormatMethodParams() {
        assertThat(
            SpacingAroundColonRule().format(
                """
                fun validIdentity(value: String): String = value
                fun identity(value  : String): String = value
                fun oneSpaceIdentity(value : String): String = value
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun validIdentity(value: String): String = value
            fun identity(value: String): String = value
            fun oneSpaceIdentity(value: String): String = value
            """.trimIndent()
        )
    }

    @Test
    fun testLintGenericMethodParam() {
        assertThat(
            SpacingAroundColonRule().lint(
                """
                fun <T: Any> trueIdentity(value: T): T = value
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 7, "colon-spacing", "Missing spacing before \":\"")
            )
        )
    }

    @Test
    fun testFormatGenericMethodParam() {
        assertThat(
            SpacingAroundColonRule().format(
                """
                fun <T: Any> trueIdentity(value: T): T = value
                fun <T : Any> trueIdentity(value: T): T = value
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun <T : Any> trueIdentity(value: T): T = value
            fun <T : Any> trueIdentity(value: T): T = value
            """.trimIndent()
        )
    }

    @Test
    fun testFormatEOF() {
        assertThat(
            SpacingAroundColonRule().format(
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
            )
        ).isEqualTo(
            """
            class X :
                Y,
                Z
            class A // comment
                : B
            class A /*

            */
                : B
            val xmlFormatter:
                String = ""
            """.trimIndent()
        )
    }
}
