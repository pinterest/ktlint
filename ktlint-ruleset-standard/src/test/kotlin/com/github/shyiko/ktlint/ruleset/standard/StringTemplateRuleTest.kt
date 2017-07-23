package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class StringTemplateRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(StringTemplateRule())
    }

    @Test
    fun testFormat() {
        testFormatUsingResource(StringTemplateRule())
    }

}
