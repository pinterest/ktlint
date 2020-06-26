package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
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
    fun testMultiLineIfWithoutCurlyBraces() {
        val ifWithoutCurlyBrace =
            """
            fun main() {
                if (true)
                    return 0
            }
            """.trimIndent()
        assertThat(lint(ifWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(3, 9, "multiline-if-else", "Missing { ... }")
            )
        )
        assertThat(format(ifWithoutCurlyBrace)).isEqualTo(
            """
            fun main() {
                if (true) {
                    return 0
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testMultiLineIfElseWithoutCurlyBraces() {
        val ifElseWithoutCurlyBrace =
            """
            fun main() {
                if (true)
                    return 0
                else
                    return 1
            }
            """.trimIndent()

        assertThat(lint(ifElseWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(3, 9, "multiline-if-else", "Missing { ... }"),
                LintError(5, 9, "multiline-if-else", "Missing { ... }")
            )
        )
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
            """
            fun main() {
                if (true) {
                    return 0
                } else {
                    return 1
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testMultilineCondition() {
        val ifElseWithoutCurlyBrace =
            """
            fun main() {
                if (i2 > 0 &&
                    i3 < 0
                )
                    return 2
                else
                    return 3
            }
            """.trimIndent()

        assertThat(lint(ifElseWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(5, 9, "multiline-if-else", "Missing { ... }"),
                LintError(7, 9, "multiline-if-else", "Missing { ... }")
            )
        )
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
            """
            fun main() {
                if (i2 > 0 &&
                    i3 < 0
                ) {
                    return 2
                } else {
                    return 3
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testMultiLineIfElseIfElseWithoutCurlyBraces() {
        val ifElseWithoutCurlyBrace =
            """
            fun main() {
                if (true)
                    return 0
                else if (false)
                    return 1
                else
                    return -1
            }
            """.trimIndent()

        assertThat(lint(ifElseWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(3, 9, "multiline-if-else", "Missing { ... }"),
                LintError(5, 9, "multiline-if-else", "Missing { ... }"),
                LintError(7, 9, "multiline-if-else", "Missing { ... }")
            )
        )
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
            """
            fun main() {
                if (true) {
                    return 0
                } else if (false) {
                    return 1
                } else {
                    return -1
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testNestedMultiLineIfElse() {
        val ifElseWithoutCurlyBrace =
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

        assertThat(lint(ifElseWithoutCurlyBrace)).isEqualTo(
            listOf(
                LintError(3, 9, "multiline-if-else", "Missing { ... }"),
                LintError(4, 13, "multiline-if-else", "Missing { ... }"),
                LintError(5, 17, "multiline-if-else", "Missing { ... }"),
                LintError(7, 17, "multiline-if-else", "Missing { ... }"),
                LintError(9, 17, "multiline-if-else", "Missing { ... }"),
                LintError(11, 17, "multiline-if-else", "Missing { ... }"),
                LintError(13, 13, "multiline-if-else", "Missing { ... }"),
                LintError(18, 9, "multiline-if-else", "Missing { ... }"),
                LintError(19, 13, "multiline-if-else", "Missing { ... }"),
                LintError(21, 13, "multiline-if-else", "Missing { ... }"),
                LintError(23, 13, "multiline-if-else", "Missing { ... }"),
                LintError(25, 9, "multiline-if-else", "Missing { ... }"),
                LintError(26, 13, "multiline-if-else", "Missing { ... }"),
                LintError(28, 13, "multiline-if-else", "Missing { ... }")
            )
        )
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
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
        )
    }

    @Test
    fun testWithEmptyLineBeforeIfExpression() {
        val ifElseWithoutCurlyBrace =
            """
            fun test(): Int {
                val b = foo()

                if (b)
                    return 1
                else
                    return 2
            }
            """.trimIndent()
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
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
        )
    }

    @Test
    fun testInReturnExpression() {
        val ifElseWithoutCurlyBrace =
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
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
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
        )
    }

    @Test
    fun testInLambdaExpression() {
        val ifElseWithoutCurlyBrace =
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
        assertThat(format(ifElseWithoutCurlyBrace)).isEqualTo(
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
        )
    }

    private fun assertOK(kotlinScript: String) {
        assertThat(format(kotlinScript)).isEqualTo(kotlinScript)
        assertThat(lint(kotlinScript)).isEqualTo(emptyList<LintError>())
    }

    private fun format(kotlinScript: String): String {
        return MultiLineIfElseRule().format(kotlinScript)
    }

    private fun lint(kotlinScript: String): List<LintError> {
        return MultiLineIfElseRule().lint(kotlinScript)
    }
}
