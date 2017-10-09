package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class NoItParamInMultilineLambdaRuleTest {

    @Test
    fun testLint() =
        testLintUsingResource(NoItParamInMultilineLambdaRule())

}
