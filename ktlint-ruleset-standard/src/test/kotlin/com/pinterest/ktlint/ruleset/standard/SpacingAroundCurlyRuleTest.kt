package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundCurlyRuleTest {

    @Test
    fun testLint() {
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun emit() { }")).isEmpty()
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun emit() { val a = a@{ } }")).isEmpty()
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun emit() {}")).isEmpty()
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun main() { val v = if (true){return 0} }"))
            .isEqualTo(
                listOf(
                    LintError(1, 31, "curly-spacing", "Missing spacing around \"{\""),
                    LintError(1, 40, "curly-spacing", "Missing spacing before \"}\"")
                )
            )
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun main() { val v = if (true) { return 0 } }"))
            .isEmpty()
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun main() { fn({a -> a}, 0) }"))
            .isEqualTo(
                listOf(
                    LintError(1, 18, "curly-spacing", "Missing spacing after \"{\""),
                    LintError(1, 24, "curly-spacing", "Missing spacing before \"}\"")
                )
            )
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun main() { fn({ a -> a }, 0) }"))
            .isEmpty()
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun main() { fn({}, 0) && fn2({ }, 0) }"))
            .isEmpty()
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint("fun main() { find { it.default ?: false }?.phone }"))
            .isEmpty()
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint(
                """
                fun main() {
                    emptyList<String>().find { true } !!.hashCode()
                    emptyList<String>().find { true }!!.hashCode()
                }
                """.trimIndent()
            )
        )
            .isEqualTo(
                listOf(
                    LintError(2, 37, "curly-spacing", "Unexpected space after \"}\"")
                )
            )
    }

    @Test
    fun testLintStringTemplate() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().lint(
                """fun main() { emit(node.startOffset, "Line must not end with \"${'$'}{node.text}\"", true) }"""
                    .trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().format(
                """
                fun main() {
                    val v = if (true){return ""}
                    val v = if (true) { return "" }
                    fn({a -> a}, 0)
                    fn({ a -> a }, 0)
                    fn({},{}, {}, 0)
                    fn({ }, 0)
                    fn({ a -> try{a()}catch (e: Exception){null} }, 0)
                    foo.associateBy( { it.length } , { it } )
                    try{call()}catch (e: Exception){}
                    call({}, {})
                    a.let{}.apply({})
                    f({ if (true) {r.add(v)}; r})
                    emptyList<String>().find { true }!!.hashCode()
                    emptyList<String>().find { true } !!.hashCode()
                    l.groupBy { it }[key] + l.groupBy { it } [key]
                    l.groupBy { it }(key) + l.groupBy { it } (key)
                    object : Any() {}::class.java.classLoader
                    object : Any() {} ::class.java.classLoader
                    class A
                    {
                        companion object
                        {
                        }
                    }
                    interface A
                    {
                    }
                    if (true)
                    {
                    }
                    do
                    {
                    } while (true)
                    call(
                        { echo() },
                        { echo() },
                        //
                        { echo() }
                    )
                    val f =
                        { true }
                }
                class A { private val shouldEjectBlock = block@ { (pathProgress ?: return@block false) >= 0.85 } }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun main() {
                val v = if (true) { return "" }
                val v = if (true) { return "" }
                fn({ a -> a }, 0)
                fn({ a -> a }, 0)
                fn({}, {}, {}, 0)
                fn({ }, 0)
                fn({ a -> try { a() } catch (e: Exception) { null } }, 0)
                foo.associateBy({ it.length }, { it })
                try { call() } catch (e: Exception) {}
                call({}, {})
                a.let {}.apply({})
                f({ if (true) { r.add(v) }; r })
                emptyList<String>().find { true }!!.hashCode()
                emptyList<String>().find { true }!!.hashCode()
                l.groupBy { it }[key] + l.groupBy { it }[key]
                l.groupBy { it }(key) + l.groupBy { it }(key)
                object : Any() {}::class.java.classLoader
                object : Any() {}::class.java.classLoader
                class A {
                    companion object {
                    }
                }
                interface A {
                }
                if (true) {
                }
                do {
                } while (true)
                call(
                    { echo() },
                    { echo() },
                    //
                    { echo() }
                )
                val f =
                    { true }
            }
            class A { private val shouldEjectBlock = block@{ (pathProgress ?: return@block false) >= 0.85 } }
            """.trimIndent()
        )
    }

    @Test
    fun testNewLineAfterReturnTypeFails() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundCurlyRule().format(
                """
                fun foo(): String
                {
                    return "foo"
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun foo(): String {
                return "foo"
            }
            """.trimIndent()
        )
    }
}
