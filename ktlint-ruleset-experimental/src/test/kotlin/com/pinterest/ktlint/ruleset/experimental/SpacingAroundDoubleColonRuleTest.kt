package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundDoubleColonRuleTest {

    @Test
    fun testLint() {
        assertThat(
            SpacingAroundDoubleColonRule().diffFileLint("spec/spacing-around-double-colon/lint.kt.spec")
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundDoubleColonRule().diffFileFormat(
                "spec/spacing-around-double-colon/format.kt.spec",
                "spec/spacing-around-double-colon/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
