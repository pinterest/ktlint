package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.SpacingAroundOperatorsRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundOperatorsRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundOperatorsRule().lint(
            """
            import a.b.*
            fun main() {
                val v = 0 - 1 * 2
                val v1 = 0-1*2
                val v2 = -0 - 1
                val v3 = v * 2
                i++
                val y = +1
                var x = 1 in 3..4
                val b = 1 < 2
                fun(a = true)
                val res = ArrayList<LintError>()
                fn(*arrayOfNulls<Any>(0 * 1))
                fun <T>List<T>.head() {}
                val a= ""
                d *= 1
                call(*v)
                open class A<T> {
                    open fun x() {}
                }
                class B<T> : A<T>() {
                    override fun x() = super<A>.x()
                }
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(4, 15, "op-spacing", "Missing spacing around \"-\""),
            LintError(4, 17, "op-spacing", "Missing spacing around \"*\""),
            LintError(15, 10, "op-spacing", "Missing spacing before \"=\"")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundOperatorsRule().format(
            """
            fun main() {
                val v1 = 0-1*2
                val v2 = -0-1
                val v3 = v*2
                i++
                val y = +1
                var x = 1 in 3..4
            }
            """.trimIndent()
        )).isEqualTo(
            """
            fun main() {
                val v1 = 0 - 1 * 2
                val v2 = -0 - 1
                val v3 = v * 2
                i++
                val y = +1
                var x = 1 in 3..4
            }
            """.trimIndent()
        )
    }

}
