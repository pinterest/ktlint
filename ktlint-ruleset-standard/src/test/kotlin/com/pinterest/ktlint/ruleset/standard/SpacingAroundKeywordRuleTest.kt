package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundKeywordRuleTest {

    @Test
    fun testLint() {
        assertThat(
            SpacingAroundKeywordRule().lint(
                """
                fun main() {
                    if(true) {}
                    while(true) {}
                    do {} while(true)

                    if (true) {}
                    else {}

                    try { "".trim() }
                    catch (e: Exception) {}
                    finally {}

                    if (true) {
                    } else {}
                    if (true) {} else {}
                    if (true)
                        println()
                    else
                        entry.value

                    try {
                        "".trim()
                    } catch (e: Exception) {
                    } finally {}
                    try { "".trim() } catch (e: Exception) {} finally {}

                    val v = 0
                    when (v) {
                        1 -> println("")
                        else -> println("")
                    }

                    if (V)
                        V.let { "" }
                    else
                        V
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 7, "keyword-spacing", "Missing spacing after \"if\""),
                LintError(3, 10, "keyword-spacing", "Missing spacing after \"while\""),
                LintError(4, 16, "keyword-spacing", "Missing spacing after \"while\""),
                LintError(7, 5, "keyword-spacing", "Unexpected newline before \"else\""),
                LintError(10, 5, "keyword-spacing", "Unexpected newline before \"catch\""),
                LintError(11, 5, "keyword-spacing", "Unexpected newline before \"finally\"")
            )
        )
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundKeywordRule().format(
                """
                fun main() {
                    if(true) {}
                    if (true) {}
                    while(true) {}
                    do {} while(true)

                    if (true) {}
                    else {}
                    if (true) {
                    }
                    else {}

                    try { "".trim() }
                    catch (e: Exception) {}
                    finally {}
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun main() {
                if (true) {}
                if (true) {}
                while (true) {}
                do {} while (true)

                if (true) {} else {}
                if (true) {
                } else {}

                try { "".trim() } catch (e: Exception) {} finally {}
            }
            """.trimIndent()
        )
    }

    @Test
    fun getterAndSetterFunction() {
        assertThat(
            SpacingAroundKeywordRule().format(
                """
            var x: String
			    get () {
				    return ""
			    }
			    private set (value) {
				    x = value
			    }
            """.trimIndent()
            )
        ).isEqualTo(
            """
            var x: String
			    get() {
				    return ""
			    }
			    private set(value) {
				    x = value
			    }
            """.trimIndent()
        )
    }

    @Test
    fun visibilityOrInjectProperty() {
        assertThat(
            SpacingAroundKeywordRule().lint(
                """
        var setterVisibility: String = "abc"
            private set
        var setterWithAnnotation: Any? = null
            @Inject set
        var setterOnNextLine: String
            private set
            (value) { setterOnNextLine = value}
            """
            )
        ).isEqualTo(
            listOf(
                LintError(7, 21, "keyword-spacing", "Unexpected spacing after \"set\"")
            )
        )
    }

    @Test
    fun keywordInKDoc() {
        assertThat(
            SpacingAroundKeywordRule().lint(
                """
                 /**
                 * Convenience wrapper around [Mockito.when] to avoid special `when` notation.
                 */
                fun <T> whenever(call: T): OngoingStubbing<T> = Mockito.`when`(call)
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun elseAfterWhenBrace() {
        assertThat(
            SpacingAroundKeywordRule().lint(
                """
                fun test(b: Boolean, s: String): Int {
                    return if (b)
                        when (s) {
                            "A" -> 1
                            "B" -> 2
                            else -> 3
                        }
                    else 4
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
