package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ImportOrderingRuleTest {

    @Test
    fun testLint() {
        assertThat(ImportOrderingRule().diffFileLint("spec/import-ordering/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            ImportOrderingRule().diffFileFormat(
                "spec/import-ordering/format.kt.spec",
                "spec/import-ordering/format-expected.kt.spec"
            )
        ).isEmpty()
    }
}
