package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoLineBreakAfterElseRuleTest {
    private val noLineBreakAfterElseRuleAssertThat = assertThatRule { NoLineBreakAfterElseRule() }

    @Test
    fun `Given an else-if with a linebreak between the else and if then do return a lint error`() {
        val code =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else
                if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        noLineBreakAfterElseRuleAssertThat(code)
            .hasLintViolation(5, 1, "Unexpected line break after \"else\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a valid if-else-if statement then do not return lint errors`() {
        val code =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        noLineBreakAfterElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a valid if-else statement then do not return lint errrors`() {
        val code =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        noLineBreakAfterElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a new line between the else and the opening brace of the block then do return a lint error`() {
        val code =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else
                {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        noLineBreakAfterElseRuleAssertThat(code)
            .hasLintViolation(5, 1, "Unexpected line break after \"else\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-else statement not using blocks around the branch(es) then do not return a lint errors`() {
        val code =
            """
            fun funA() {
                if (conditionA()) doSomething() else doAnotherThing()
                if (conditionA())
                    doSomething()
                if (conditionA()) {
                    doSomething()
                } else
                    doAnotherThing()
                if (conditionA())
                    doSomething()
                else {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        noLineBreakAfterElseRuleAssertThat(code).hasNoLintViolations()
    }
}
