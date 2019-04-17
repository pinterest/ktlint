package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

const val ruleId = "no-line-break-before-assignment"

class NoLineBreakBeforeAssignmentRuleTest {
    @Test
    fun testAllPartsOnSameLineIsValid() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakBeforeAssignmentRule().lint(
                """
                val valA = ""
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLineBreakAfterAssignmentIsValid() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoLineBreakBeforeAssignmentRule().lint(
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
            com.pinterest.ktlint.ruleset.standard.NoLineBreakBeforeAssignmentRule().lint(
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
            com.pinterest.ktlint.ruleset.standard.NoLineBreakBeforeAssignmentRule().lint(
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
            com.pinterest.ktlint.ruleset.standard.NoLineBreakBeforeAssignmentRule().format(
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
}
