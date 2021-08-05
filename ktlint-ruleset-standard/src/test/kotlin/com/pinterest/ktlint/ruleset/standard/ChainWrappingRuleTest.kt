package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ChainWrappingRuleTest {

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

    // https://github.com/pinterest/ktlint/issues/1055
    @Test
    fun `lint elvis operator and comment`() {
        assertThat(
            ChainWrappingRule().lint(
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
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/1130
    @Test
    fun `format when conditions`() {
        assertThat(
            ChainWrappingRule().format(
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
            )
        ).isEqualTo(
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
        )
    }
}
