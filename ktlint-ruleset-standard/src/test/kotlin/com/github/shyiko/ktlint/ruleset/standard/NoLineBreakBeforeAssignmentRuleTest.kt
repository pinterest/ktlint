package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

const val ruleId = "no-line-break-before-assignment"

class NoLineBreakBeforeAssignmentRuleTest {
    @Test
    fun testAllPartsOnSameLineIsValid() {
        assertThat(NoLineBreakBeforeAssignmentRule().lint(
            """
              val valA = ""
              """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testLineBreakAfterAssignmentIsValid() {
        assertThat(NoLineBreakBeforeAssignmentRule().lint(
            """
                val valA =
                      ""
              """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testLineBreakBeforeAssignmentIsViolation() {
        assertThat(NoLineBreakBeforeAssignmentRule().lint(
            """
              val valA
                    = ""
              """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 7, ruleId, "Line break before assignment is not allowed")
        ))
    }

    @Test
    fun testViolationInFunction() {
        assertThat(NoLineBreakBeforeAssignmentRule().lint(
            """
              fun funA()
                    = ""
              """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 7, ruleId, "Line break before assignment is not allowed")
        ))
    }

    @Test
    fun testFixViolationByRemovingLineBreakFromLeftAndPutItOnRightSide() {
        assertThat(NoLineBreakBeforeAssignmentRule().format(
            """
              fun funA()
                    = ""
              """.trimIndent()
        )).isEqualTo(
            """
              fun funA() =
                    ""
              """.trimIndent()
        )
    }
}
