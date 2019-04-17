package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoBlankLineBeforeRbraceRuleTest {

    @Test
    fun testLint() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoBlankLineBeforeRbraceRule().diffFileLint(
                "spec/no-blank-line-before-rbrace/lint.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoBlankLineBeforeRbraceRule().diffFileFormat(
                "spec/no-blank-line-before-rbrace/format.kt.spec",
                "spec/no-blank-line-before-rbrace/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
