package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class StringTemplateRuleTest {

    @Test
    fun testLint() {
        assertThat(com.pinterest.ktlint.ruleset.standard.StringTemplateRule().diffFileLint("spec/string-template/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.StringTemplateRule().diffFileFormat(
                "spec/string-template/format.kt.spec",
                "spec/string-template/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
