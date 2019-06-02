package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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
