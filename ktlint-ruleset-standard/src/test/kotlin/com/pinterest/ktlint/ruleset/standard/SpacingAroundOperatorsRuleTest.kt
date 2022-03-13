package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpacingAroundOperatorsRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundOperatorsRule().diffFileLint("spec/op-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundOperatorsRule().diffFileFormat(
                "spec/op-spacing/format.kt.spec",
                "spec/op-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `Given a type argument list, containing a wildcard type then do not require space around the wildcard`() {
        val code =
            """
            val foo = Map<PropertyType<*>>
            """.trimIndent()
        assertThat(SpacingAroundOperatorsRule().lint(code)).isEmpty()
        assertThat(SpacingAroundOperatorsRule().format(code)).isEqualTo(code)
    }
}
