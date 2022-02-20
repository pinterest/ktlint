package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.FinalNewlineRule.Companion.insertNewLineProperty
import com.pinterest.ktlint.test.EditorConfigOverride
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(FeatureInAlphaState::class)
class FinalNewlineRuleTest {
    private val finalNewLineRule = FinalNewlineRule()

    @Test
    fun `Lint should ignore empty file`() {
        assertThat(
            finalNewLineRule.lint("", FINAL_NEW_LINE_REQUIRED)
        ).isEmpty()
    }

    @Test
    fun `Lint should by default fail on missing new line`() {
        assertThat(
            finalNewLineRule.lint(
                """
                fun name() {
                }
                """.trimIndent(),
                FINAL_NEW_LINE_REQUIRED
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "final-newline", "File must end with a newline (\\n)")
            )
        )
    }

    @Test
    fun `Lint should succeed by default when final newline is present`() {
        assertThat(
            finalNewLineRule.lint(
                """
                fun name() {
                }

                """.trimIndent(),
                FINAL_NEW_LINE_REQUIRED
            )
        ).isEmpty()
    }

    @Test
    fun `Should ignore several empty final lines`() {
        assertThat(
            finalNewLineRule.lint(
                """
                fun main() {
                }



                """.trimIndent(),
                FINAL_NEW_LINE_REQUIRED
            )
        ).isEmpty()
    }

    @Test
    fun `Should ignore empty file when final new line is disabled`() {
        assertThat(
            finalNewLineRule.lint(
                "",
                FINAL_NEW_LINE_NOT_REQUIRED
            )
        ).isEmpty()
    }

    @Test
    fun `Should ignore missing final new line when it is disabled`() {
        assertThat(
            finalNewLineRule.lint(
                """
                fun name() {
                }
                """.trimIndent(),
                FINAL_NEW_LINE_NOT_REQUIRED
            )
        ).isEmpty()
    }

    @Test
    fun `Should fail check if new-line is disabled, but file contains it`() {
        assertThat(
            finalNewLineRule.lint(
                """
                fun name() {
                }

                """.trimIndent(),
                FINAL_NEW_LINE_NOT_REQUIRED
            )
        ).isEqualTo(
            listOf(
                LintError(2, 2, "final-newline", "Redundant newline (\\n) at the end of file")
            )
        )
    }

    @Test
    fun `Should add final new line on format`() {
        assertThat(
            finalNewLineRule.format(
                """
                fun name() {
                }
                """.trimIndent(),
                FINAL_NEW_LINE_REQUIRED
            )
        ).isEqualTo(
            """
            fun name() {
            }

            """.trimIndent(),
            FINAL_NEW_LINE_REQUIRED
        )
    }

    @Test
    fun `Should remove final new line on format when it is disabled`() {
        assertThat(
            finalNewLineRule.format(
                """
                fun name() {
                }

                """.trimIndent(),
                FINAL_NEW_LINE_NOT_REQUIRED
            )
        ).isEqualTo(
            """
            fun name() {
            }
            """.trimIndent()
        )
    }

    private companion object {
        val FINAL_NEW_LINE_REQUIRED = EditorConfigOverride.from(
            insertNewLineProperty to true
        )
        val FINAL_NEW_LINE_NOT_REQUIRED = EditorConfigOverride.from(
            insertNewLineProperty to false
        )
    }
}
