package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundUnaryOperatorRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundUnaryOperatorRule().diffFileLint("spec/unary-op-spacing/lint.kt.spec"))
            .isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundUnaryOperatorRule().diffFileFormat(
                srcPath = "spec/unary-op-spacing/format.kt.spec",
                expectedPath = "spec/unary-op-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
