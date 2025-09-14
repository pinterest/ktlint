package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class MultiLineIfElseRuleTest {
    private val multiLineIfElseRuleAssertThat = assertThatRule { MultiLineIfElseRule() }

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
                LintViolation(5, 9, "Missing { ... }"),
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
                LintViolation(7, 9, "Missing { ... }"),
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
                LintViolation(7, 9, "Missing { ... }"),
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
                LintViolation(28, 13, "Missing { ... }"),
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
                LintViolation(7, 9, "Missing { ... }"),
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
                LintViolation(16, 13, "Missing { ... }"),
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
                LintViolation(6, 13, "Missing { ... }"),
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(4, 9, "Missing { ... }"),
                LintViolation(7, 9, "Missing { ... }"),
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(3, 9, "Missing { ... }"),
                LintViolation(5, 9, "Missing { ... }"),
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
                    if (bar) {
                        b
                    } else {
                        c
                    },
                    d
                )
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 18, "Missing { ... }"),
                LintViolation(6, 13, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1560 - Given an if statement with else keyword on same line as true branch`() {
        val code =
            """
            fun foo() = if (bar())
                "a" else
                "b"
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = if (bar()) {
                "a"
            } else {
                "b"
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 5, "Missing { ... }"),
                LintViolation(3, 5, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 828 - Given an if statement with multiline statement starting on same line as if`() {
        val code =
            """
            fun foo() {
                if (true) 50
                  .toString()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    50
                        .toString()
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolation(2, 15, "Missing { ... }")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 828 - Given an if statement with simple branches but the else branch is on a separate line`() {
        val code =
            """
            fun foo() {
                if (true) 50
                else 55
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (true) {
                    50
                } else {
                    55
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 15, "Missing { ... }"),
                LintViolation(3, 10, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1904 - Given an nested if else statement followed by an elvis operator`() {
        val code =
            """
            val foo1 = if (bar1) {
                "bar1"
            } else {
                null
            } ?: "something-else"

            val foo2 = if (bar1) {
                "bar1"
            } else if (bar2) {
                null
            } else {
                null
            } ?: "something-else"
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2057 - Given an else condition with single line binary expression`() {
        val code =
            """
            val foo = if (bar1) {
                "bar1"
            } else bar2 ?: "something-else"
            """.trimIndent()
        val formattedCode =
            """
            val foo = if (bar1) {
                "bar1"
            } else {
                bar2 ?: "something-else"
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 8, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1904 - Given an nested if else statement and else which is part of a dot qualified expression`() {
        val code =
            """
            val foo1 = if (bar1) {
                "bar1"
            } else {
                "bar2"
            }.plus("foo")

            val foo2 = if (bar1) {
                "bar1"
            } else if (bar2) {
                "bar2"
            } else {
                "bar3"
            }.plus("foo")
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2057 - Given an else with chained condition`() {
        val code =
            """
            val foo = if (System.currentTimeMillis() % 2 == 0L) {
                0
            } else System.currentTimeMillis().toInt()
            """.trimIndent()
        val formattedCode =
            """
            val foo = if (System.currentTimeMillis() % 2 == 0L) {
                0
            } else {
                System.currentTimeMillis().toInt()
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 8, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2057 - Given an if with chained condition`() {
        val code =
            """
            val foo = if (System.currentTimeMillis() % 2 == 0L) System.currentTimeMillis().toInt()
            else {
                System.currentTimeMillis().toInt()
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = if (System.currentTimeMillis() % 2 == 0L) {
                System.currentTimeMillis().toInt()
            } else {
                System.currentTimeMillis().toInt()
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 53, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2420 - Given a single line else-if`() {
        val code =
            """
            fun foo() {
                if (foo) {
                    doFoo()
                } else if (bar) doBar()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                if (foo) {
                    doFoo()
                } else if (bar) {
                    doBar()
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 21, "Missing { ... }"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 3088 - Given an if with an empty 'then' block then do not throw an exception`() {
        val code =
            """
            fun foo() {
                if (false) else {
                    false
                }
            }
            """.trimIndent()
        multiLineIfElseRuleAssertThat(code).hasNoLintViolations()
    }
}
