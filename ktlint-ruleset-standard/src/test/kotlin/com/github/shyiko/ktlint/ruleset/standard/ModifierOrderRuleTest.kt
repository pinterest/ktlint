package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ModifierOrderRuleTest {

    @Test
    fun testLint() {
        // pretty much every line below should trip an error
        assertThat(ModifierOrderRule().lint(
            """
            abstract open class A { // open is here for test purposes only, otherwise it's redundant
                open protected val v = ""
                open suspend internal fun f(v: Any): Any = ""
                lateinit public var lv: String
                tailrec abstract fun findFixPoint(x: Double = 1.0): Double
            }

            class B : A() {
                override public val v = ""
                override suspend fun f(v: Any): Any = ""
                override tailrec fun findFixPoint(x: Double): Double
                    = if (x == Math.cos(x)) x else findFixPoint(Math.cos(x))

                companion object {
                   const internal val V = ""
                }
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 1, "modifier-order", "Incorrect modifier order (should be \"open abstract\")"),
            LintError(2, 5, "modifier-order", "Incorrect modifier order (should be \"protected open\")"),
            LintError(3, 5, "modifier-order", "Incorrect modifier order (should be \"internal open suspend\")"),
            LintError(4, 5, "modifier-order", "Incorrect modifier order (should be \"public lateinit\")"),
            LintError(5, 5, "modifier-order", "Incorrect modifier order (should be \"abstract tailrec\")"),
            LintError(9, 5, "modifier-order", "Incorrect modifier order (should be \"public override\")"),
            LintError(10, 5, "modifier-order", "Incorrect modifier order (should be \"suspend override\")"),
            LintError(11, 5, "modifier-order", "Incorrect modifier order (should be \"tailrec override\")"),
            LintError(15, 8, "modifier-order", "Incorrect modifier order (should be \"internal const\")")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(ModifierOrderRule().format(
            """
            abstract open class A { // open is here for test purposes only, otherwise it's redundant
                open protected val v = ""
                open suspend internal fun f(v: Any): Any = ""
                lateinit public var lv: String
                tailrec abstract fun findFixPoint(x: Double = 1.0): Double
            }

            class B : A() {
                override public val v = ""
                override suspend fun f(v: Any): Any = ""
                override tailrec fun findFixPoint(x: Double): Double
                    = if (x == Math.cos(x)) x else findFixPoint(Math.cos(x))

                companion object {
                   const internal val V = ""
                }
            }
            """
        )).isEqualTo(
            """
            open abstract class A { // open is here for test purposes only, otherwise it's redundant
                protected open val v = ""
                internal open suspend fun f(v: Any): Any = ""
                public lateinit var lv: String
                abstract tailrec fun findFixPoint(x: Double = 1.0): Double
            }

            class B : A() {
                public override val v = ""
                suspend override fun f(v: Any): Any = ""
                tailrec override fun findFixPoint(x: Double): Double
                    = if (x == Math.cos(x)) x else findFixPoint(Math.cos(x))

                companion object {
                   internal const val V = ""
                }
            }
            """
        )
    }

}
