package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundParensRuleTest {

    @Test
    fun testLint() {
        assertThat(com.pinterest.ktlint.ruleset.standard.SpacingAroundParensRule().diffFileLint("spec/paren-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.SpacingAroundParensRule().diffFileFormat(
                "spec/paren-spacing/format.kt.spec",
                "spec/paren-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
