package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.MaxLineLengthRule.Companion.ignoreBackTickedIdentifierProperty
import com.pinterest.ktlint.ruleset.standard.MaxLineLengthRule.Companion.ignoreStringProperty
import com.pinterest.ktlint.test.EditorConfigOverride
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class MaxLineLengthRuleTest {
    @Test
    fun testLint() {
        assertThat(
            MaxLineLengthRule().diffFileLint(
                "spec/max-line-length/lint.kt.spec",
                EditorConfigOverride.from(maxLineLengthProperty to 80)
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
                EditorConfigOverride.from(maxLineLengthProperty to 40)
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "max-line-length", "Exceeded max line length (40)")
            )
        )
    }

    @Test
    fun testErrorSuppressionOnTokensBetweenBackticks() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                @Test
                fun `Some too long test description between backticks`() {
                    println("teeeeeeeeeeeeeeeeeeeeeeeext")
                }
                """.trimIndent(),
                EditorConfigOverride.from(
                    maxLineLengthProperty to 40,
                    ignoreBackTickedIdentifierProperty to true
                )
            )
        ).isEqualTo(
            listOf(
                // Note that no error was generated on line 2 with the long fun name but on another line
                LintError(3, 1, "max-line-length", "Exceeded max line length (40)")
            )
        )
    }

    @Test
    fun testReportLongLinesAfterExcludingTokensBetweenBackticks() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                @ParameterizedTest
                fun `Some too long test description between backticks`(looooooooongParameterName: String) {
                    println("teeeeeeeeext")
                }
                """.trimIndent(),
                EditorConfigOverride.from(maxLineLengthProperty to 40)
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "max-line-length", "Exceeded max line length (40)")
            )
        )
    }

    @Test
    fun testErrorSuppressionOnString() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                fun main() {
                    val longString = "this is a very very very very long long string on a single line"
                }
                """.trimIndent(),
                EditorConfigOverride.from(
                    maxLineLengthProperty to 40,
                    ignoreStringProperty to true
                )
            )
        ).isEmpty()
    }

    @Test
    fun testReportLongLinesAfterIgnoringStringWhenNotLastToken() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                @Annotation(
                    "this is a very very very very long long string on a single line"
                )
                fun test1(
                    arg: String = "this is a very very very very long long string on a single line"
                ) {
                    val longString = "this is a very very very very long long string on a single line"
                }

                @Annotation("this is a very very very very long long string on a single line")
                fun test2(arg: String = "this is a very very very very long long string on a single line") {
                    val longString = "this is a very very very very long long string on a single line" // With a comment
                }
                """.trimIndent(),
                EditorConfigOverride.from(
                    maxLineLengthProperty to 40,
                    ignoreStringProperty to true
                )
            )
        ).isEqualTo(
            listOf(
                // Note that no error was generated on lines 2, 5 and 7 as the String was the last token of the lines
                LintError(10, 1, "max-line-length", "Exceeded max line length (40)"),
                LintError(11, 1, "max-line-length", "Exceeded max line length (40)"),
                LintError(12, 1, "max-line-length", "Exceeded max line length (40)"),
            )
        )
    }

    @Test
    fun testReportLongLinesAfterExcludingString() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                fun main() {
                    val longString = "this is a very very very very long long string on a single line"
                }
                """.trimIndent(),
                EditorConfigOverride.from(maxLineLengthProperty to 40)
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "max-line-length", "Exceeded max line length (40)")
            )
        )
    }

    @Test
    fun testErrorSuppressionOnTokensBetweenBackticksAndString() {
        assertThat(
            MaxLineLengthRule().lint(
                """
                fun main() {
                    val `backTickedProperty` = "some very very very very long string on a single line"
                }
                """.trimIndent(),
                EditorConfigOverride.from(
                    maxLineLengthProperty to 40,
                    ignoreBackTickedIdentifierProperty to true,
                    ignoreStringProperty to true,
                )
            )
        ).isEmpty()
    }

    @Test
    fun testLintOff() {
        assertThat(
            MaxLineLengthRule().diffFileLint(
                "spec/max-line-length/lint-off.kt.spec",
                EditorConfigOverride.from(maxLineLengthProperty to "off")
            )
        ).isEmpty()
    }

    @Test
    fun testRangeSearch() {
        for (i in 0 until 10) {
            assertThat(RangeTree((0..i).toList()).query(Int.MIN_VALUE, Int.MAX_VALUE).toString())
                .isEqualTo((0..i).toList().toString())
        }
        assertThat(RangeTree(emptyList()).query(1, 5).toString()).isEqualTo("[]")
        assertThat(RangeTree((5 until 10).toList()).query(1, 5).toString()).isEqualTo("[]")
        assertThat(RangeTree((5 until 10).toList()).query(3, 7).toString()).isEqualTo("[5, 6]")
        assertThat(RangeTree((5 until 10).toList()).query(7, 12).toString()).isEqualTo("[7, 8, 9]")
        assertThat(RangeTree((5 until 10).toList()).query(10, 15).toString()).isEqualTo("[]")
        assertThat(RangeTree(listOf(1, 5, 10)).query(3, 4).toString()).isEqualTo("[]")
    }
}
