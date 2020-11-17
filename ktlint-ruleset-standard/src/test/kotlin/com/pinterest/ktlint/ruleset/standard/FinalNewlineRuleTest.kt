package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.FinalNewlineRule.Companion.insertNewLineProperty
import com.pinterest.ktlint.test.EditorConfigTestRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(FeatureInAlphaState::class)
class FinalNewlineRuleTest {

    @get:Rule
    val editorConfigTestRule = EditorConfigTestRule()
    private val finalNewLineRule = FinalNewlineRule()

    @Test
    fun `Lint should ignore empty file`() {
        assertThat(
            finalNewLineRule.lint("")
        ).isEmpty()
    }

    @Test
    fun `Lint should by default fail on missing new line`() {
        assertThat(
            finalNewLineRule.lint(
                """
                fun name() {
                }
                """.trimIndent()
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

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `Should ignore several empty final lines for scripts`() {
        assertThat(
            finalNewLineRule.lint(
                script = true,
                text =
                """
                fun main() {
                }



                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `Should ignore empty file when final new line is disabled`() {
        val testFile = disableFinalNewLine()

        assertThat(
            finalNewLineRule.lint(
                testFile.absolutePath,
                ""
            )
        ).isEmpty()
    }

    @Test
    fun `Should ignore missing final new line when it is disabled`() {
        val testFile = disableFinalNewLine()

        assertThat(
            finalNewLineRule.lint(
                testFile.absolutePath,
                """
                fun name() {
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `Should fail check if new-line is disabled, but file contains it`() {
        val testFile = disableFinalNewLine()

        assertThat(
            finalNewLineRule.lint(
                testFile.absolutePath,
                """
                fun name() {
                }

                """.trimIndent()
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
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun name() {
            }

            """.trimIndent()
        )
    }

    @Test
    fun `Should remove final new line on format when it is disabled`() {
        val testFile = disableFinalNewLine()

        assertThat(
            finalNewLineRule.format(
                testFile.absolutePath,
                """
                fun name() {
                }

                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun name() {
            }
            """.trimIndent()
        )
    }

    private fun disableFinalNewLine(): File = editorConfigTestRule
        .writeToEditorConfig(
            mapOf(insertNewLineProperty.type to false.toString())
        )
}
