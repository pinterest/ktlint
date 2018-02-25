package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class MaxLineLengthRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(MaxLineLengthRule(), userData = mapOf("max_line_length" to "80"))
    }

    @Test
    fun testErrorSupression() {
        assertThat(MaxLineLengthRule().lint(
            """
            fun main(vaaaaaaaaaaaaaaaaaaaaaaar: String) { // ktlint-disable max-line-length
                println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
            /* ktlint-disable max-line-length */
                println("teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext")
            }
            """.trimIndent(),
            userData = mapOf("max_line_length" to "40")
        )).isEqualTo(listOf(
            LintError(2, 1, "max-line-length", "Exceeded max line length (40)")
        ))
    }

    @Test
    fun testLintOff() {
        testLintUsingResource(MaxLineLengthRule(), userData = mapOf("max_line_length" to "off"), qualifier = "off")
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
