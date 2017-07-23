package com.github.shyiko.ktlint.ruleset.standard

import org.testng.annotations.Test

class MaxLineLengthRuleTest {

    @Test
    fun testLint() {
        testLintUsingResource(MaxLineLengthRule(), userData = mapOf("max_line_length" to "80"))
    }

}
