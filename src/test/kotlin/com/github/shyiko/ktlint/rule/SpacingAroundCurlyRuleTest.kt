package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundCurlyRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundCurlyRule().lint("fun emit() { }")).isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun emit() {}")).isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { val v = if (true){return 0} }"))
            .isEqualTo(listOf(
                LintError(1, 31, "rule-id", "Missing spacing around \"{\""),
                LintError(1, 40, "rule-id", "Missing spacing before \"}\"")
            ))
        assertThat(SpacingAroundCurlyRule().lint("fun main() { val v = if (true) { return 0 } }"))
            .isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { fn({a -> a}, 0) }"))
            .isEqualTo(listOf(
                LintError(1, 18, "rule-id", "Missing spacing after \"{\""),
                LintError(1, 24, "rule-id", "Missing spacing before \"}\"")
            ))
        assertThat(SpacingAroundCurlyRule().lint("fun main() { fn({ a -> a }, 0) }"))
            .isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { fn({}, 0) && fn2({ }, 0) }"))
            .isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundCurlyRule().format(
            """
            fun main() {
                val v = if (true){return ""}
                val v = if (true) { return "" }
                fn({a -> a}, 0)
                fn({ a -> a }, 0)
                fn({},{}, {}, 0)
                fn({ }, 0)
                fn({ a -> try{a()}catch (e: Exception){null} }, 0)
                try{call()}catch (e: Exception){}
                call({}, {})
                a.let{}.apply({})
            }
            """.trimIndent()
        )).isEqualTo(
            """
            fun main() {
                val v = if (true) { return "" }
                val v = if (true) { return "" }
                fn({ a -> a }, 0)
                fn({ a -> a }, 0)
                fn({}, {}, {}, 0)
                fn({ }, 0)
                fn({ a -> try { a() } catch (e: Exception) { null } }, 0)
                try { call() } catch (e: Exception) {}
                call({}, {})
                a.let {}.apply({})
            }
            """.trimIndent()
        )
    }

}
