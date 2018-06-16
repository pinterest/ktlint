package com.github.shyiko.ktlint.ruleset.standard


import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions

import org.testng.annotations.Test

class ifThenBlockWithCurlyBraceRuleRuleTest {
    @Test
    fun testFormatIfThenWithCurlyBrace() {
        Assertions.assertThat(IfThenBlockWithCurlyBraceRule().format("fun main() { if (true) { return 0 } }")).isEqualTo("fun main() { if (true) { return 0 } }")
    }

    @Test
    fun testLintIfThenWithCurlyBrace() {
        Assertions.assertThat(IfThenBlockWithCurlyBraceRule().lint("fun main() { if (true) { return 0 } }")).isEqualTo(emptyList<LintError>())
    }

    @Test
    fun testFormatIfThenWithoutCurlyBrace() {
        Assertions.assertThat(IfThenBlockWithCurlyBraceRule().format("fun main() { if (true) return 0 }")).isEqualTo("fun main() { if (true) {return 0} }")
    }

    @Test
    fun testLintIfThenWithoutCurlyBrace() {
        Assertions.assertThat(IfThenBlockWithCurlyBraceRule().lint("fun main() { if (true) return 0 }")).isEqualTo(
            listOf(
                LintError(line = 1, col = 24, ruleId = "if-then-without-curly-brace-rule", detail = "if-then block should start with `{`"),
                LintError(line = 1, col = 31, ruleId = "if-then-without-curly-brace-rule", detail = "if-then block should end with `}`")
            )
        )
    }

}
