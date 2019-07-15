package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MultiLineIfElseRuleTest {

    @Test
    fun testSingleLineWithCurlyBraces() {
        val ifThenWithCurlyBrace = "fun main() { if (true) { return 0 } }"
        assertOK(ifThenWithCurlyBrace)
        val ifElseWithCurlBrace = "fun main() { if (true) { return 0 } else {return 1}}"
        assertOK(ifElseWithCurlBrace)
    }

    @Test
    fun testSingleLineWithoutCurlyBraces() {
        val ifWithoutCurlyBrace = "fun main() { if (true) return 0 }"
        assertOK(ifWithoutCurlyBrace)
        val ifElseWithoutCurlyBrace = "fun main() { if (true) return 0 else 1}"
        assertOK(ifElseWithoutCurlyBrace)
    }

    @Test
    fun testMultiLineWithCurlBraces() {
        val ifWithCurlyBrace = "fun main() { if (true) {\n return 0 } }"
        assertOK(ifWithCurlyBrace)
        val ifElseWithCurlyBrace = "fun main() { if (true) {\n return 0 } \n else {\n return 1}}"
        assertOK(ifElseWithCurlyBrace)
    }

    @Test
    fun tesMultiLineWithoutCurlyBraces() {
        val ifWithoutCurlyBrace = "fun main() { if (true) \n return 0 }"
        val ifWithoutCurlyBraceExpected = "fun main() { if (true) \n {return 0} }"
        // If-Then block with multiple lines and curly brace.
        // assertThat(format(ifWithoutCurlyBrace)).isEqualTo(ifWithoutCurlyBraceExpected)
        assertThat(lint(ifWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(2, 2, "multiline-if-else", "Missing { ... }")
            )
        )
        val ifElseWithoutCurlyBrace = "fun main() { if (true) \n return 0 \n else \n return 1 }"
        val ifElseWithoutCurlyBraceExpected = "fun main() { if (true) \n {return 0} \n else \n {return 1} }"
        // assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(ifElseWithoutCurlyBraceExpected)
        assertThat(lint(ifElseWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(2, 2, "multiline-if-else", "Missing { ... }"),
                LintError(4, 2, "multiline-if-else", "Missing { ... }")
            )
        )
    }

    private fun assertOK(kotlinScript: String) {
        Assertions.assertThat(format(kotlinScript)).isEqualTo(kotlinScript)
        Assertions.assertThat(lint(kotlinScript)).isEqualTo(emptyList<LintError>())
    }

    private fun format(kotlinScript: String): String {
        return MultiLineIfElseRule().format(kotlinScript)
    }

    private fun lint(kotlinScript: String): List<LintError> {
        return MultiLineIfElseRule().lint(kotlinScript)
    }
}
