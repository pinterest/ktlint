package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class LambdaOutsideOfParensRuleTest {

    @Test
    fun testLint() =
        testLintUsingResource(LambdaOutsideOfParensRule())

    @Test
    fun testFormat() =
        testFormatUsingResource(LambdaOutsideOfParensRule())

}
