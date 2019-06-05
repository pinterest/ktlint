package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class NoLineBreakAfterElseRuleTest {

    @Test
    fun testViolationForLineBreakBetweenElseAndIf() {
        Assertions.assertThat(
            NoLineBreakAfterElseRule().lint(
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
            NoLineBreakAfterElseRule().format(
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
            NoLineBreakAfterElseRule().lint(
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
            NoLineBreakAfterElseRule().lint(
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
            NoLineBreakAfterElseRule().lint(
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
            NoLineBreakAfterElseRule().lint(
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
            NoLineBreakAfterElseRule().lint(
                """
                fun funA() {
                    if (conditionA()) doSomething() else doAnotherThing()
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
