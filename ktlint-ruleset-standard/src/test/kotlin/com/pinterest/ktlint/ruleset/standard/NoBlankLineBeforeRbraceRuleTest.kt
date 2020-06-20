package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoBlankLineBeforeRbraceRuleTest {

    @Test
    fun testLint() {
        assertThat(
            NoBlankLineBeforeRbraceRule().diffFileLint(
                "spec/no-blank-line-before-rbrace/lint.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            NoBlankLineBeforeRbraceRule().diffFileFormat(
                "spec/no-blank-line-before-rbrace/format.kt.spec",
                "spec/no-blank-line-before-rbrace/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
