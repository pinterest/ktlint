package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundRangeOperatorRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundRangeOperatorRule().diffFileLint("spec/range-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundRangeOperatorRule().diffFileFormat(
                "spec/range-spacing/format.kt.spec",
                "spec/range-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
