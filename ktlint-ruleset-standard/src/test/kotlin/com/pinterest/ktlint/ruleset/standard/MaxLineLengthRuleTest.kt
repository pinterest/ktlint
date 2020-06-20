package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MaxLineLengthRuleTest {

    @Test
    fun testLint() {
        assertThat(
            MaxLineLengthRule().diffFileLint(
                "spec/max-line-length/lint.kt.spec",
                userData = mapOf("max_line_length" to "80")
            )
        ).isEmpty()
    }

    @Test
    fun testErrorSuppression() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                fun main(vaaaaaaaaaaaaaaaaaaaaaaar: String) { // ktlint-disable max-line-length
                    println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
                /* ktlint-disable max-line-length */
                    println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
                }
                """.trimIndent(),
                userData = mapOf("max_line_length" to "40")
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "max-line-length", "Exceeded max line length (40)")
            )
        )
    }

    @Test
    fun testLintOff() {
        assertThat(
            MaxLineLengthRule().diffFileLint(
                "spec/max-line-length/lint-off.kt.spec",
                userData = mapOf("max_line_length" to "off")
            )
        ).isEmpty()
    }

    @Test
    fun testRangeSearch() {
        for (i in 0 until 10) {
            assertThat(RangeTree((0..i).asSequence().toList()).query(Int.MIN_VALUE, Int.MAX_VALUE).toString())
                .isEqualTo((0..i).asSequence().toList().toString())
        }
        assertThat(RangeTree(emptyList()).query(1, 5).toString()).isEqualTo("[]")
        assertThat(RangeTree((5 until 10).asSequence().toList()).query(1, 5).toString()).isEqualTo("[]")
        assertThat(RangeTree((5 until 10).asSequence().toList()).query(3, 7).toString()).isEqualTo("[5, 6]")
        assertThat(RangeTree((5 until 10).asSequence().toList()).query(7, 12).toString()).isEqualTo("[7, 8, 9]")
        assertThat(RangeTree((5 until 10).asSequence().toList()).query(10, 15).toString()).isEqualTo("[]")
        assertThat(RangeTree(listOf(1, 5, 10)).query(3, 4).toString()).isEqualTo("[]")
    }
}
