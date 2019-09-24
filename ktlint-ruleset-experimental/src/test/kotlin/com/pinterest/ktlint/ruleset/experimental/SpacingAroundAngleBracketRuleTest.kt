package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundAngleBracketRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundAngleBracketsRule().diffFileLint("spec/angle-bracket-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundAngleBracketsRule().diffFileFormat(
                "spec/angle-bracket-spacing/format.kt.spec",
                "spec/angle-bracket-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
