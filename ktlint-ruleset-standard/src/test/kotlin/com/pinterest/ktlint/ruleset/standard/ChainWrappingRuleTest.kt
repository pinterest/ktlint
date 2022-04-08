package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChainWrappingRuleTest {
    private val chainWrappingRuleAssertThat = ChainWrappingRule().assertThat()

    @Test
    fun testLint() {
        assertThat(ChainWrappingRule().diffFileLint("spec/chain-wrapping/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            ChainWrappingRule().diffFileFormat(
                "spec/chain-wrapping/format.kt.spec",
                "spec/chain-wrapping/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `Issue 1055 - lint elvis operator and comment`() {
        val code =
            """
            fun test(): Int {
                val foo = foo()
                    ?: // Comment
                    return bar()
                return baz()
            }

            fun foo(): Int? = null
            fun bar(): Int = 1
            fun baz(): Int = 2
            """.trimIndent()
        chainWrappingRuleAssertThat(code).hasNoLintErrors()
    }

    // https://github.com/pinterest/ktlint/issues/1130
    @Test
    fun `format when conditions`() {
        val code =
            """
            fun test(foo: String?, bar: String?, baz: String?) {
                when {
                    foo != null &&
                        bar != null
                        && baz != null -> {
                    }
                    else -> {
                    }
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(foo: String?, bar: String?, baz: String?) {
                when {
                    foo != null &&
                        bar != null &&
                        baz != null -> {
                    }
                    else -> {
                    }
                }
            }
            """.trimIndent()
        chainWrappingRuleAssertThat(code)
            .hasLintErrors(
                LintError(5, 13, "chain-wrapping", "Line must not begin with \"&&\"")
            ).isFormattedAs(formattedCode)
    }
}
