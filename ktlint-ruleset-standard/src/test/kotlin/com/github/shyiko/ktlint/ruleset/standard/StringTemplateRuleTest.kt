package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class StringTemplateRuleTest {

    @Test
    fun testLint() {
        assertThat(StringTemplateRule().diffFileLint("spec/string-template/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(StringTemplateRule().diffFileFormat(
            "spec/string-template/format.kt.spec",
            "spec/string-template/format-expected.kt.spec"
        )).isEmpty()
    }
}
