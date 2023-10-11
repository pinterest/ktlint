package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class MultiLineLoopRuleTest {
    private val multiLineLoopRuleAssertThat = assertThatRule { MultiLineLoopRule() }

    @Test
    fun `Given loop statements with curly braces on single line`() {
        val code =
            """
            fun foo() {
                for (i in 1..10) { return 0 }
                while (true) { return 0 }
                do { return 0 } while (true)
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given loop statements without curly braces on single line`() {
        val code =
            """
            fun foo() {
                for (i in 1..10) return 0
                while (true) return 0
                do return 0 while (true)
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given multiline loop statements with curly braces`() {
        val code =
            """
            fun foo() {
                for (i in 1..10) {
                    return 0
                }
                while (true) {
                    return 0
                }
                do {
                    return 0
                } while (true)
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given multiline loop statements without curly braces`() {
        val code =
            """
            fun foo() {
                for (i in 1..10)
                    return 0
                while (true)
                    return 0
                do
                    return 0
                while (true)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                for (i in 1..10) {
                    return 0
                }
                while (true) {
                    return 0
                }
                do {
                    return 0
                } while (true)
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(5, 9, "Missing { ... }"),
                LintViolation(7, 9, "Missing { ... }"),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given deep nested loop statements without curly braces`() {
        val code =
            """
            fun main() {
                for (i in 1..10)
                    while (true)
                        do
                            return 0
                        while (true)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                for (i in 1..10) {
                    while (true) {
                        do {
                            return 0
                        } while (true)
                    }
                }
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(4, 13, "Missing { ... }"),
                LintViolation(5, 17, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given loop statements inside a lambda`() {
        val code =
            """
            fun test(s: String?): Int {
                val i = s.let {
                    for (i in 1..10)
                        1
                    while (true)
                        2
                    do
                        3
                    while (true)
                } ?: 0
                return i
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(s: String?): Int {
                val i = s.let {
                    for (i in 1..10) {
                        1
                    }
                    while (true) {
                        2
                    }
                    do {
                        3
                    } while (true)
                } ?: 0
                return i
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 13, "Missing { ... }"),
                LintViolation(6, 13, "Missing { ... }"),
                LintViolation(8, 13, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a do-while-statement with do keyword on same line as main statement`() {
        val code =
            """
            fun foo() {
                do return 0
                while (true)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                do {
                    return 0
                } while (true)
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given loop statements with multiline statement starting on same line as loop`() {
        val code =
            """
            fun foo() {
                for (i in 1..10) 25
                  .toString()
                while (true) 50
                  .toString()
                do 75
                  .toString()
                while (true)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                for (i in 1..10) {
                    25
                        .toString()
                }
                while (true) {
                    50
                        .toString()
                }
                do {
                    75
                        .toString()
                } while (true)
            }
            """.trimIndent()
        multiLineLoopRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 22, "Missing { ... }"),
                LintViolation(4, 18, "Missing { ... }"),
                LintViolation(6, 8, "Missing { ... }"),
            )
            .isFormattedAs(formattedCode)
    }
}
