package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class ChainWrappingRuleTest {

    @Test
    fun testLint() =
        testLintUsingResource(ChainWrappingRule())

    @Test
    fun testFormat() =
        testFormatUsingResource(ChainWrappingRule())
}
