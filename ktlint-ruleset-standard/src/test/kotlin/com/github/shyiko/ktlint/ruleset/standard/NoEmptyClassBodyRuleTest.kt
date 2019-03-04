package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoEmptyClassBodyRuleTest {

    @Test
    fun testLint() {
        assertThat(
            NoEmptyClassBodyRule().diffFileLint(
                "spec/no-empty-class-body/lint.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            NoEmptyClassBodyRule().diffFileFormat(
                "spec/no-empty-class-body/format.kt.spec",
                "spec/no-empty-class-body/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatEmptyClassBodyAtTheEndOfFile() {
        assertThat(NoEmptyClassBodyRule().format("class A {}\n")).isEqualTo("class A\n")
        assertThat(NoEmptyClassBodyRule().format("class A {}")).isEqualTo("class A")
    }
}
