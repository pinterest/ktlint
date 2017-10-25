package com.github.shyiko.ktlint.ruleset.standard

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
}
