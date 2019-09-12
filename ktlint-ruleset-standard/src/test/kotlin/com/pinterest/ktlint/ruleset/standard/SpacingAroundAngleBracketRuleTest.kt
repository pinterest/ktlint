package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class SpacingAroundAngleBracketRuleTest {

    @Test
    @Ignore("https://github.com/pinterest/ktlint/issues/580")
    fun testLint() {
        assertThat(SpacingAroundRangeOperatorRule().diffFileLint("spec/angle-bracket-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    @Ignore("https://github.com/pinterest/ktlint/issues/580")
    fun testFormat() {
        assertThat(
            SpacingAroundRangeOperatorRule().diffFileFormat(
                "spec/angle-bracket-spacing/format.kt.spec",
                "spec/angle-bracket-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
