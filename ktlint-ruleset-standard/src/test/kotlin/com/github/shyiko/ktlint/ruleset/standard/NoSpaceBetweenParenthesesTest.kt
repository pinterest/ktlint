package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoSpaceBetweenParenthesesTest {
    @Test
    fun testFailWhenEncounterSpace() {
        assertThat(NoSpaceBetweenParentheses().lint("fun main( ) {}"))
            .isEqualTo(listOf(
                LintError(1, 10, "no-space-between-parentheses", "Unexpected space between parentheses")
            ))
    }

    @Test
    fun testNoFailureWhenOnlyParentheses() {
        assertThat(NoSpaceBetweenParentheses().lint("fun main() {}"))
            .isEmpty()
    }

    @Test
    fun testNoFailureWhenMultilineDeclaration() {
        assertThat(NoSpaceBetweenParentheses().lint(
            """
                fun main(
                  val a: String
                ) {}
             """
        )).isEmpty()
    }

    @Test
    fun testAutoFormat() {
        assertThat(NoSpaceBetweenParentheses().format("fun main( ) {}"))
            .isEqualTo("fun main() {}")
    }
}
