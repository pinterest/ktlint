package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.rules.NoEmptyFileRule.Companion.NO_EMPTY_FILE_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoEmptyFileRuleTest {
    private val noEmptyFileRuleAssertThat = assertThatRule { NoEmptyFileRule() }

    @Test
    fun `Given non-empty kotlin file then ignore the rule for this file`() {
        val code =
            """
            package tmp
            fun main(){
                println("Hello world")
            }
            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to true)
            .hasNoLintViolations()
    }

    @Test
    fun `Given an empty kotlin file then do a return lint error`() {
        val code = EMPTY_FILE
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to true)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kt` should not be empty")
    }

    @Test
    fun `Given an empty kotlin script file then do a return lint error`() {
        val code = EMPTY_FILE
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kts")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to true)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kts` should not be empty")
    }

    @Test
    fun `Given empty kotlin file when lint disable then ignore the rule for this file`() {
        val code = EMPTY_FILE
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .withEditorConfigOverride(NO_EMPTY_FILE_PROPERTY to false)
            .hasNoLintViolations()
    }

    private companion object {
        private const val EMPTY_FILE = ""
    }
}
