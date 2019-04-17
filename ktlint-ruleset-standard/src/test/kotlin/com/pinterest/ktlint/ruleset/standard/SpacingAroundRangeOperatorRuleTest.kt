package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundRangeOperatorRuleTest {

    @Test
    fun testLint() {
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundRangeOperatorRule().diffFileLint("spec/range-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundRangeOperatorRule().diffFileFormat(
                "spec/range-spacing/format.kt.spec",
                "spec/range-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
