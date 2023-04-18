package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.rules.NoEmptyFileRule.Companion.NO_EMPTY_FILE_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoEmptyFileRuleTest {
    private val noEmptyFileRuleAssertThat = assertThatRule { NoEmptyFileRule() }

    @Test
    fun `Given not empty kotlin file then ignore the rule for this file`() {
        val code =
            """
            package tmp
            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to true)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an empty kotlin file then do a return lint error`() {
        val fileName = "Tmp.kt"
        val code =
            """

            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/$fileName")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to true)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `$fileName` should not be empty")
    }

    @Test
    fun `Given an empty kotlin script file then do a return lint error`() {
        val fileName = "Tmp.kts"
        val code =
            """

            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/$fileName")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to true)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `$fileName` should not be empty")
    }

    @Test
    fun testLintOff() {
        val code =
            """

            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to false)
            .hasNoLintViolations()
    }
}
