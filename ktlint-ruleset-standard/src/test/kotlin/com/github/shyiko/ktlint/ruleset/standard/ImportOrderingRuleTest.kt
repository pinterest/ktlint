package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class ImportOrderingRuleTest {

    @Test
    fun testLint() =
        testLintUsingResource(ImportOrderingRule())

    @Test
    fun testFormat() =
        testFormatUsingResource(ImportOrderingRule())
}
