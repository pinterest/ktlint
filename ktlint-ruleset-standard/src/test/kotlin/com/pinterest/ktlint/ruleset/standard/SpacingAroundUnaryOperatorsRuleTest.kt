package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundUnaryOperatorsRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundUnaryOperatorsRule().diffFileLint("spec/unary-op-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundUnaryOperatorsRule().diffFileFormat(
                "spec/unary-op-spacing/format.kt.spec",
                "spec/unary-op-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
