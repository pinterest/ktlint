package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class SpacingAroundOperatorsRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(SpacingAroundOperatorsRule())
    }

    @Test
    fun testFormat() {
        testFormatUsingResource(SpacingAroundOperatorsRule())
    }
}
