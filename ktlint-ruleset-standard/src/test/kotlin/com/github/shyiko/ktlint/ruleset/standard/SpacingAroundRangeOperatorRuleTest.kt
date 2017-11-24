package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class SpacingAroundRangeOperatorRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(SpacingAroundRangeOperatorRule())
    }

    @Test
    fun testFormat() {
        testFormatUsingResource(SpacingAroundRangeOperatorRule())
    }
}
