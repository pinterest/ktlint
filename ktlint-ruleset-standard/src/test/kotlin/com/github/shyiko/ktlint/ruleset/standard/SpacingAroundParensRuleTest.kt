package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class SpacingAroundParensRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(SpacingAroundParensRule())
    }

    @Test
    fun testFormat() {
        testFormatUsingResource(SpacingAroundParensRule())
    }
}
