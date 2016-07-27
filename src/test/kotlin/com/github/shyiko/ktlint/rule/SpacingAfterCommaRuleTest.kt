package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAfterCommaRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAfterCommaRule().lint("fun main() { x(1,3); x(1, 3); println(\",\") }"))
            .isEqualTo(listOf(
                LintError(1, 18, "rule-id", "Missing spacing after \",\"")
            ))
        assertThat(SpacingAfterCommaRule().lint(
            """
            enum class E {
                A, B,C
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 10, "rule-id", "Missing spacing after \",\"")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAfterCommaRule().format("fun main() { x(1,3); x(1, 3) }"))
            .isEqualTo("fun main() { x(1, 3); x(1, 3) }")
    }

}

