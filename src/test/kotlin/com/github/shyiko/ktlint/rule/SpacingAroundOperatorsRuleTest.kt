package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
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
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(4, 15, "rule-id", "Missing spacing around \"-\""),
            LintError(4, 17, "rule-id", "Missing spacing around \"*\""),
            LintError(15, 10, "rule-id", "Missing spacing before \"=\"")
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
