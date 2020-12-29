package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

const val ruleId = "no-line-break-before-assignment"

class NoLineBreakBeforeAssignmentRuleTest {
    @Test
    fun testAllPartsOnSameLineIsValid() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().lint(
                """
                val valA = ""
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLineBreakAfterAssignmentIsValid() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().lint(
                """
                val valA =
                      ""
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLineBreakBeforeAssignmentIsViolation() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().lint(
                """
                val valA
                      = ""
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 7, ruleId, "Line break before assignment is not allowed")
            )
        )
    }

    @Test
    fun testViolationInFunction() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().lint(
                """
                fun funA()
                      = ""
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 7, ruleId, "Line break before assignment is not allowed")
            )
        )
    }

    @Test
    fun testFixViolationByRemovingLineBreakFromLeftAndPutItOnRightSide() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun funA()
                      = ""
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun funA() =
                  ""
            """.trimIndent()
        )
    }

    @Test
    fun `test assignment with no following space issue #693`() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun a()
                        =f()
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun a() =
                    f()
            """.trimIndent()
        )
    }

    @Test
    fun `test assignment with comment 1`() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun sum(a: Int, b: Int): Int
                    // comment
                    = a + b
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun sum(a: Int, b: Int): Int =
                // comment
                a + b
            """.trimIndent()
        )
    }

    @Test
    fun `test assignment with comment 2`() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun sum(a: Int, b: Int): Int
                    // comment
                    =a + b
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun sum(a: Int, b: Int): Int =
                // comment
                a + b
            """.trimIndent()
        )
    }

    @Test
    fun `test assignment with comment 3`() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun sum(a: Int, b: Int): Int // comment
                    = a + b
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun sum(a: Int, b: Int): Int = // comment
                a + b
            """.trimIndent()
        )
    }

    @Test
    fun `test assignment with comment 4`() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun sum(a: Int, b: Int): Int// comment
                    = a + b
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun sum(a: Int, b: Int): Int = // comment
                a + b
            """.trimIndent()
        )
    }

    // https://github.com/pinterest/ktlint/issues/1039
    @Test
    fun `test default arguments`() {
        assertThat(
            NoLineBreakBeforeAssignmentRule().format(
                """
                fun test(b: Boolean?
                = null): Int = 3
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun test(b: Boolean? =
            null): Int = 3
            """.trimIndent()
        )
    }
}
