package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.SpacingAroundCurlyRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundCurlyRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundCurlyRule().lint("fun emit() { }")).isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun emit() {}")).isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { val v = if (true){return 0} }"))
            .isEqualTo(listOf(
                LintError(1, 31, "curly-spacing", "Missing spacing around \"{\""),
                LintError(1, 40, "curly-spacing", "Missing spacing before \"}\"")
            ))
        assertThat(SpacingAroundCurlyRule().lint("fun main() { val v = if (true) { return 0 } }"))
            .isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { fn({a -> a}, 0) }"))
            .isEqualTo(listOf(
                LintError(1, 18, "curly-spacing", "Missing spacing after \"{\""),
                LintError(1, 24, "curly-spacing", "Missing spacing before \"}\"")
            ))
        assertThat(SpacingAroundCurlyRule().lint("fun main() { fn({ a -> a }, 0) }"))
            .isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { fn({}, 0) && fn2({ }, 0) }"))
            .isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("fun main() { find { it.default ?: false }?.phone }"))
            .isEmpty()
        assertThat(SpacingAroundCurlyRule().lint("""
            fun main() {
                emptyList<String>().find { true } !!.hashCode()
                emptyList<String>().find { true }!!.hashCode()
            }
            """.trimIndent()))
            .isEqualTo(listOf(
                LintError(2, 37, "curly-spacing", "Unexpected space after \"}\"")
            ))
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
                f({ if (true) {r.add(v)}; r})
                emptyList<String>().find { true }!!.hashCode()
                emptyList<String>().find { true } !!.hashCode()
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
                f({ if (true) { r.add(v) }; r })
                emptyList<String>().find { true }!!.hashCode()
                emptyList<String>().find { true }!!.hashCode()
            }
            """.trimIndent()
        )
    }

}
