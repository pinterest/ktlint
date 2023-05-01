package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

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
            .hasNoLintViolations()
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Given an empty kotlin file then do a return lint error`() {
        val code = EMPTY_FILE
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kt` should not be empty")
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Given an empty kotlin script file then do a return lint error`() {
        val code = EMPTY_FILE
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kts")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kts` should not be empty")
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Given only package statement in kotlin file then do a return lint error`() {
        val code =
            """
            package path
            """.trimIndent()

        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kt` should not be empty")
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Given only import statement in kotlin file then do a return lint error`() {
        val code =
            """
            import sample.Hello
            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kt` should not be empty")
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Given only package and import statements in kotlin file then do a return lint error`() {
        val code =
            """
            package path
            import sample.Hello
            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File `/project/some/path/Tmp.kt` should not be empty")
    }

    @Test
    fun `Given non-empty kotlin file then ignore this file`() {
        val code =
            """
            package tmp
            fun main(){
                println("Hello world")
            }
            """.trimIndent()
        noEmptyFileRuleAssertThat(code)
            .asFileWithPath("/some/path/Tmp.kt")
            .hasNoLintViolations()
    }

    private companion object {
        private const val EMPTY_FILE = ""
    }
}
