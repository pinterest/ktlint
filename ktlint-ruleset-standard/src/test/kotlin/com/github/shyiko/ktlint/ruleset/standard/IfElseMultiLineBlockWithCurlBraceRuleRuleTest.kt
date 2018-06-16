package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat

import org.testng.annotations.Test

class IfElseMultiLineBlockWithCurlBraceRuleRuleTest {
    @Test
    fun testWithCurlyBrace_SingleLine() {

        val ifThenWithCurlyBrace = "fun main() { if (true) { return 0 } }"
        assertFormatAndLintSuccess(ifThenWithCurlyBrace)

        val ifElseWithCurlBrace = "fun main() { if (true) { return 0 } else {return 1}}"
        assertFormatAndLintSuccess(ifElseWithCurlBrace)
    }

    @Test
    fun testWithoutCurlyBrace_SingleLine() {
        val ifWithoutCurlyBrace = "fun main() { if (true) return 0 }"
        assertFormatAndLintSuccess(ifWithoutCurlyBrace)

        val ifElseWithoutCurlyBrace = "fun main() { if (true) return 0 else 1}"
        assertFormatAndLintSuccess(ifElseWithoutCurlyBrace)
    }

    @Test
    fun testWithCurlBrace_MultiLine() {
        val ifWithCurlyBrace = "fun main() { if (true) {\n return 0 } }"
        assertFormatAndLintSuccess(ifWithCurlyBrace)

        val ifElseWithCurlyBrace = "fun main() { if (true) {\n return 0 } \n else {\n return 1}}"
        assertFormatAndLintSuccess(ifElseWithCurlyBrace)
    }

    @Test
    fun tesWithoutCurlyBrace_MultiLine() {
        val ifWithoutCurlyBrace = "fun main() { if (true) \n return 0 }"
        val ifWithoutCurlyBraceExpected = "fun main() { if (true) \n {return 0} }"
        // If-Then block with multiple lines and curly brace.
        assertThat(format(ifWithoutCurlyBrace)).isEqualTo(ifWithoutCurlyBraceExpected)
        assertThat(lint(ifWithoutCurlyBrace)).isEqualTo(listOf(
            LintError(line = 2, col = 2, ruleId = "if-else-multiline-block-with-curly-brace-rule", detail = "if-else block with multiline should start with `{`"),
            LintError(line = 2, col = 9, ruleId = "if-else-multiline-block-with-curly-brace-rule", detail = "if-else block with multiline should end with `}`")
        ))

        val ifElseWithoutCurlyBrace = "fun main() { if (true) \n return 0 \n else \n return 1 }"
        val ifElseWithoutCurlyBraceExpected = "fun main() { if (true) \n {return 0} \n else \n {return 1} }"
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(ifElseWithoutCurlyBraceExpected)
        assertThat(lint(ifElseWithoutCurlyBrace)).isEqualTo(listOf(
            LintError(line = 2, col = 2, ruleId = "if-else-multiline-block-with-curly-brace-rule", detail = "if-else block with multiline should start with `{`"),
            LintError(line = 2, col = 9, ruleId = "if-else-multiline-block-with-curly-brace-rule", detail = "if-else block with multiline should end with `}`"),
            LintError(line = 4, col = 2, ruleId = "if-else-multiline-block-with-curly-brace-rule", detail = "if-else block with multiline should start with `{`"),
            LintError(line = 4, col = 9, ruleId = "if-else-multiline-block-with-curly-brace-rule", detail = "if-else block with multiline should end with `}`")
        ))
    }

    private fun assertFormatAndLintSuccess(kotlinScript: String) {
        Assertions.assertThat(format(kotlinScript)).isEqualTo(kotlinScript)
        Assertions.assertThat(lint(kotlinScript)).isEqualTo(emptyList<LintError>())
    }

    private fun format(kotlinScript: String): String {
        return IfElseMultiLineBlockWithCurlBraceRule().format(kotlinScript)
    }

    private fun lint(kotlinScript: String): List<LintError> {
        return IfElseMultiLineBlockWithCurlBraceRule().lint(kotlinScript)
    }
}
