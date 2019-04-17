package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.testng.annotations.Test

class NoLineBreakAfterElseRuleTest {

    @Test
    fun testViolationForLineBreakBetweenElseAndIf() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().lint(
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
            )
        ).isEqualTo(
            listOf(
                LintError(5, 1, "no-line-break-after-else", "Unexpected line break after \"else\"")
            )
        )
    }

    @Test
    fun testFixViolationForLineBreakBetweenElseAndIf() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().format(
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
            )
        ).isEqualTo(
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testValidElseIf() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().lint(
                """
                fun funA() {
                    if (conditionA()) {
                        doSomething()
                    } else if (conditionB()) {
                        doAnotherThing()
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testValidSimpleElse() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().lint(
                """
                fun funA() {
                    if (conditionA()) {
                        doSomething()
                    } else {
                        doAnotherThing()
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testViolationForLineBreakBetweenElseAndBracket() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().lint(
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
            )
        ).isEqualTo(
            listOf(
                LintError(5, 1, "no-line-break-after-else", "Unexpected line break after \"else\"")
            )
        )
    }

    @Test
    fun testViolationWhenBracketOmitted() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().lint(
                """
                fun funA() {
                    if (conditionA())
                        doSomething()
                    else
                        doAnotherThing()
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testValidWhenBracketOmitted() {
        Assertions.assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakAfterElseRule().lint(
                """
                fun funA() {
                    if (conditionA()) doSomething() else doAnotherThing()
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
