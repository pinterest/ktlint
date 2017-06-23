package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class IndentationRuleTest {

    @Test
    fun testRule() {
        assertThat(IndentationRule().lint(
            """
            /**
             * _
             */
            fun main() {
                val a = 0
                    val b = 0
                if (a == 0) {
                    println(a)
                }
                val b = builder().setX().setY()
                    .build()
               val c = builder("long_string" +
                    "")
            }

            class A {
                var x: String
                    get() = ""
                    set(v: String) { x = v }
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(12, 1, "indent", "Unexpected indentation (3) (it should be multiple of 4)")
        ))
    }

    @Test
    fun testVerticallyAlignedParametersDoNotTriggerAnError() {
        assertThat(IndentationRule().lint(
            """
            data class D(val a: Any,
                         @Test val b: Any,
                         val c: Any = 0) {
            }

            data class D2(
                val a: Any,
                val b: Any,
                val c: Any
            ) {
            }

            fun f(val a: Any,
                  val b: Any,
                  val c: Any) {
            }

            fun f2(
                val a: Any,
                val b: Any,
                val c: Any
            ) {
            }
            """.trimIndent()
        )).isEmpty()
        assertThat(IndentationRule().lint(
            """
            class A(
               //
            ) {}
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 1, "indent", "Unexpected indentation (3) (it should be multiple of 4)")
        ))
    }

    @Test
    fun testWithCustomIndentSize() {
        assertThat(IndentationRule().lint(
            """
            /**
             * _
             */
            fun main() {
              val v = ""
              println(v)
            }

            class A {
              var x: String
                get() = ""
                set(v: String) { x = v }
            }
            """.trimIndent(),
            mapOf("indent_size" to "2")
        )).isEmpty()
    }

    @Test
    fun testErrorWithCustomIndentSize() {
        assertThat(IndentationRule().lint(
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent(),
            mapOf("indent_size" to "3")
        )).isEqualTo(listOf(
            LintError(3, 1, "indent", "Unexpected indentation (4) (it should be multiple of 3)")
        ))
    }
}
