package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoEmptyClassBodyRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(NoEmptyClassBodyRule())
    }

    @Test
    fun testFormat() {
        testFormatUsingResource(NoEmptyClassBodyRule())
    }

    @Test
    fun testFormatEmptyClassBodyAtTheEndOfFile() {
        assertThat(NoEmptyClassBodyRule().format("class A {}\n")).isEqualTo("class A\n")
        assertThat(NoEmptyClassBodyRule().format("class A {}")).isEqualTo("class A")
    }
}
