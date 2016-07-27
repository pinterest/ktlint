package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
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
            LintError(1, 8, "rule-id", "Missing spacing around \":\""),
            LintError(4, 11, "rule-id", "Missing spacing after \":\""),
            LintError(6, 16, "rule-id", "Missing spacing before \":\""),
            LintError(9, 12, "rule-id", "Missing spacing before \":\"")
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

}
