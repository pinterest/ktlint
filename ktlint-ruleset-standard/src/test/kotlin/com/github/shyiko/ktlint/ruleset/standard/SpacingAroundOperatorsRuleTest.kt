package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundOperatorsRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundOperatorsRule().diffFileLint("spec/op-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundOperatorsRule().diffFileFormat(
            "spec/op-spacing/format.kt.spec",
            "spec/op-spacing/format-expected.kt.spec"
        )).isEmpty()
    }
}
