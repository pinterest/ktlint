package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundParensRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundParensRule().diffFileLint("spec/paren-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundParensRule().diffFileFormat(
                "spec/paren-spacing/format.kt.spec",
                "spec/paren-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
