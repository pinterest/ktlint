package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class MultiLineIfElseRuleTest {
    private val multiLineIfElseRuleAssertThat = MultiLineIfElseRule().assertThat()

    @Test
    fun `Given an if-statement with curly braces on single line`() {
        val code =
            """
            fun foo() {
                if (true) { return 0 }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an if-else-statement with curly braces on single line`() {
        val code =
            """
            val foo = if (true) { return 0 } else {return 1}
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an if-statement without curly braces on single line`() {
        val code =
            """
            fun foo() {
                if (true) return 0
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an if-else-statement without curly braces on single line`() {
        val code =
            """
            val foo = if (true) return 0 else return 1
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multiline if-statement with curly braces`() {
        val code =
            """
            fun foo() {
                if (true) {
                    return 0
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multiline if-else-statement with curly braces`() {
        val code =
            """
            val foo =
                if (true) {
                    return 0
                } else {
                    return 1
                }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multiline if-statement without curly braces`() {
        val code =
            """
            fun foo() {
                if (true)
                    return 0
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    return 0
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolation(3, 9, "Missing { ... }")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline if-else-statement without curly braces`() {
        val code =
            """
            val foo =
                if (true)
                    return 0
                else
                    return 1
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                if (true) {
                    return 0
                } else {
                    return 1
                }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(5, 9, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 727 - Given a multiline if-else-statement with multiple conditions without curly braces`() {
        val code =
            """
            val foo =
                if (false ||
                    true
                )
                    return 0
                else
                    return 1
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                if (false ||
                    true
                ) {
                    return 0
                } else {
                    return 1
                }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 9, "Missing { ... }"),
                LintViolation(7, 9, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 727 - Given a multiline if-else-if-statement without curly braces`() {
        val code =
            """
            val foo =
                if (true)
                    return 0
                else if (false)
                    return 1
                else
                    return 2
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                if (true) {
                    return 0
                } else if (false) {
                    return 1
                } else {
                    return 2
                }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(5, 9, "Missing { ... }"),
                LintViolation(7, 9, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 727 - Given a deep nested if-else-if-statement without curly braces`() {
        val code =
            """
            fun main() {
                if (outerCondition1)
                    if (innerCondition1)
                        if (innerCondition11)
                            return 0
                        else if (innerCondition12)
                            return 12
                        else if (innerCondition13)
                            return 13
                        else
                            return 14
                    else if (innerCondition44)
                        return 1
                    else {
                        return 16
                    }
                else if (outerCondition2)
                    if (innerCondition2)
                        return 2
                    else if (innerCondition3)
                        return 3
                    else
                        return 4
                else
                    if (innerCondition4)
                        return 5
                    else
                        return -1
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                if (outerCondition1) {
                    if (innerCondition1) {
                        if (innerCondition11) {
                            return 0
                        } else if (innerCondition12) {
                            return 12
                        } else if (innerCondition13) {
                            return 13
                        } else {
                            return 14
                        }
                    } else if (innerCondition44) {
                        return 1
                    } else {
                        return 16
                    }
                } else if (outerCondition2) {
                    if (innerCondition2) {
                        return 2
                    } else if (innerCondition3) {
                        return 3
                    } else {
                        return 4
                    }
                } else {
                    if (innerCondition4) {
                        return 5
                    } else {
                        return -1
                    }
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(4, 13, "Missing { ... }"),
                LintViolation(5, 17, "Missing { ... }"),
                LintViolation(7, 17, "Missing { ... }"),
                LintViolation(9, 17, "Missing { ... }"),
                LintViolation(11, 17, "Missing { ... }"),
                LintViolation(13, 13, "Missing { ... }"),
                LintViolation(18, 9, "Missing { ... }"),
                LintViolation(19, 13, "Missing { ... }"),
                LintViolation(21, 13, "Missing { ... }"),
                LintViolation(23, 13, "Missing { ... }"),
                LintViolation(25, 9, "Missing { ... }"),
                LintViolation(26, 13, "Missing { ... }"),
                LintViolation(28, 13, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-statement preceded by a blank line then do no add redundant blank lines`() {
        val code =
            """
            fun test(): Int {
                val b = foo()

                if (b)
                    return 1
                else
                    return 2
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(): Int {
                val b = foo()

                if (b) {
                    return 1
                } else {
                    return 2
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 9, "Missing { ... }"),
                LintViolation(7, 9, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an return-if-statement`() {
        val code =
            """
            fun test(i: Int, j: Int): Int {
                return if (i == 1)
                    if (j == 1)
                        1
                    else
                        2
                else if (i == 2)
                    if (j == 1)
                        3
                    else
                        4
                else
                    if (j == 1)
                        5
                    else
                        6
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(i: Int, j: Int): Int {
                return if (i == 1) {
                    if (j == 1) {
                        1
                    } else {
                        2
                    }
                } else if (i == 2) {
                    if (j == 1) {
                        3
                    } else {
                        4
                    }
                } else {
                    if (j == 1) {
                        5
                    } else {
                        6
                    }
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(4, 13, "Missing { ... }"),
                LintViolation(6, 13, "Missing { ... }"),
                LintViolation(8, 9, "Missing { ... }"),
                LintViolation(9, 13, "Missing { ... }"),
                LintViolation(11, 13, "Missing { ... }"),
                LintViolation(13, 9, "Missing { ... }"),
                LintViolation(14, 13, "Missing { ... }"),
                LintViolation(16, 13, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-statement inside a lambda`() {
        val code =
            """
            fun test(s: String?): Int {
                val i = s.let {
                    if (it == "")
                        1
                    else
                        2
                } ?: 0
                return i
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(s: String?): Int {
                val i = s.let {
                    if (it == "") {
                        1
                    } else {
                        2
                    }
                } ?: 0
                return i
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 13, "Missing { ... }"),
                LintViolation(6, 13, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 945 - Given an if-statement without curly brace and EOL-comments in the branches before actual result of branch`() {
        val code =
            """
            fun test() {
                val s = if (x > 0)
                // comment1
                    "a"
                else
                // comment2
                    "b"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test() {
                val s = if (x > 0) {
                    // comment1
                    "a"
                } else {
                    // comment2
                    "b"
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(4, 9, "Missing { ... }"),
                LintViolation(7, 9, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 945 - Given an if-statement without curly brace and EOL-comments in the branches on same line as result of branch`() {
        val code =
            """
            fun test() {
                val s = if (x > 0)
                    "a" // comment1
                else
                    "b" // comment2
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test() {
                val s = if (x > 0) {
                    "a" // comment1
                } else {
                    "b" // comment2
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(5, 9, "Missing { ... }")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1079 - Given an if-statement without curly braces a function call argument`() {
        val code =
            """
            fun foo(x: Int, y: Int, z: Int) {}
            fun test(a: Int, b: Int, c: Int, d: Int, bar: Boolean) {
                foo(
                    a,
                    if (bar) b else
                        c,
                    d
                )
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(x: Int, y: Int, z: Int) {}
            fun test(a: Int, b: Int, c: Int, d: Int, bar: Boolean) {
                foo(
                    a,
                    if (bar) b else {
                        c
                    },
                    d
                )
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            // TODO: It is not consistent that argument "b" is not wrapped in a block while argument "c" is wrapped
            .hasLintViolation(6, 13, "Missing { ... }")
            .isFormattedAs(formattedCode)
    }
}
