package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundColonRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundColonRule().lint(
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
        )).isEqualTo(listOf(
            LintError(1, 8, "colon-spacing", "Missing spacing around \":\""),
            LintError(4, 11, "colon-spacing", "Missing spacing after \":\""),
            LintError(6, 16, "colon-spacing", "Missing spacing before \":\""),
            LintError(9, 12, "colon-spacing", "Missing spacing before \":\"")
        ))
        assertThat(SpacingAroundColonRule().lint(
            """
            @file:JvmName("Foo")
            class Example(@field:Ann val foo: String, @get:Ann val bar: String)
            class Example {
                @set:[Inject VisibleForTesting]
                public var collaborator: Collaborator
            }
            fun @receiver:Fancy String.myExtension() { }
            """.trimIndent()
        )).isEmpty()
        assertThat(SpacingAroundColonRule().lint(
            """
            fun main() {
                val x = Foo::class
            }
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundColonRule().format(
            """
            class A:B
            fun main() {
                var x:Boolean
                var y: Boolean
            }
            interface D
            interface C: D
            """.trimIndent()
        )).isEqualTo(
            """
            class A : B
            fun main() {
                var x: Boolean
                var y: Boolean
            }
            interface D
            interface C : D
            """.trimIndent()
        )
    }

    @Test
    fun testLintMethod() {
        assertThat(SpacingAroundColonRule().lint(
            """
            fun main() : String = "duck"
            fun duck(): String = "main"
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 12, "colon-spacing", "Unexpected spacing before \":\"")
        ))
    }

    @Test
    fun testFormatMethod() {
        assertThat(SpacingAroundColonRule().format(
            """
            fun main() : String = "duck"
            """.trimIndent()
        )).isEqualTo(
            """
            fun main(): String = "duck"
            """.trimIndent())
    }

    @Test
    fun testLintMethodParams() {
        assertThat(SpacingAroundColonRule().lint(
            """
            fun identity(value: String): String = value
            fun unformattedIdentity(value : String): String = value
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 31, "colon-spacing", "Unexpected spacing before \":\"")
        ))
    }

    @Test
    fun testFormatMethodParams() {
        assertThat(SpacingAroundColonRule().format(
            """
            fun validIdentity(value: String): String = value
            fun identity(value  : String): String = value
            fun oneSpaceIdentity(value : String): String = value
            """.trimIndent()
        )).isEqualTo(
            """
            fun validIdentity(value: String): String = value
            fun identity(value: String): String = value
            fun oneSpaceIdentity(value: String): String = value
            """.trimIndent())
    }

    @Test
    fun testLintGenericMethodParam() {
        assertThat(SpacingAroundColonRule().lint(
            """
            fun <T: Any> trueIdentity(value: T): T = value
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 7, "colon-spacing", "Missing spacing before \":\"")
        ))
    }

    @Test
    fun testFormatGenericMethodParam() {
        assertThat(SpacingAroundColonRule().format(
            """
            fun <T: Any> trueIdentity(value: T): T = value
            fun <T : Any> trueIdentity(value: T): T = value
            """.trimIndent()
        )).isEqualTo(
            """
            fun <T : Any> trueIdentity(value: T): T = value
            fun <T : Any> trueIdentity(value: T): T = value
            """.trimIndent())
    }
}
