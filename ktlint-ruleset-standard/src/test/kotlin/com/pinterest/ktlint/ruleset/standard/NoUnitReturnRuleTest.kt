package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoUnitReturnRuleTest {

    @Test
    fun testLint() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoUnitReturnRule().diffFileLint(
                "spec/no-unit-return/lint.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoUnitReturnRule().diffFileFormat(
                "spec/no-unit-return/format.kt.spec",
                "spec/no-unit-return/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
