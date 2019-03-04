package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ChainWrappingRuleTest {

    @Test
    fun testLint() {
        assertThat(ChainWrappingRule().diffFileLint("spec/chain-wrapping/lint.kt.spec")).isEmpty()
    }


    @Test
    fun testFormat() {
        assertThat(ChainWrappingRule().diffFileFormat(
            "spec/chain-wrapping/format.kt.spec",
            "spec/chain-wrapping/format-expected.kt.spec"
        )).isEmpty()
    }
}
