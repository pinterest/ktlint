package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundOperatorsRuleTest {

    @Test
    fun testLint() {
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundOperatorsRule().diffFileLint("spec/op-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundOperatorsRule().diffFileFormat(
                "spec/op-spacing/format.kt.spec",
                "spec/op-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
