package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class NoBlankLineBeforeRbraceRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(NoBlankLineBeforeRbraceRule())
    }

    @Test
    fun testFormat() {
        testFormatUsingResource(NoBlankLineBeforeRbraceRule())
    }
}
