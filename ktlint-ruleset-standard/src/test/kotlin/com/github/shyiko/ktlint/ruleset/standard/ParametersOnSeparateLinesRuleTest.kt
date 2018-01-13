package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.testng.annotations.Test

class ParametersOnSeparateLinesRuleTest {
    private val expectedErrorMessage = "Parameter should be on separate line with indentation"
    private val expectedParenthesesMessage = "Parentheses should be on new line"
    private val expectedRuleId = ParametersOnSeparateLinesRule.RULE_ID
    private val configUserData = mapOf("indent_size" to "4", "continuation_indent_size" to "6")
    private fun expectedIndentErrorMessage(actualInden: Int, expectedIndent: Int): String {
        return "Unexpected indentation for parameter $actualInden (should be $expectedIndent)"
    }

    @Test
    fun testClassWithAbsentLineBreak() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            class ClassA(paramA: String, paramB: String,
                         paramC: String)
            """.trimIndent(),
            configUserData
        )).isEqualTo(
            listOf(
                LintError(1, 14, expectedRuleId, expectedErrorMessage),
                LintError(1, 30, expectedRuleId, expectedErrorMessage),
                LintError(2, 14, expectedRuleId, expectedIndentErrorMessage(13, 4)),
                LintError(2, 28, expectedRuleId, expectedParenthesesMessage))
        )
    }

    @Test
    fun testFormatClassWithAbsentLineBreak() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().format(
            """
            class ClassA(paramA: String, paramB: String,
                         paramC: String)
            """.trimIndent(),
            configUserData
        )).isEqualTo(
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        )
    }

    @Test
    fun testValidClassDefinitionWithMultipleLines() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent(),
            configUserData
        )).isEmpty()
    }

    @Test
    fun testValidClassDefinitionOnOneLine() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent(),
            configUserData
        )).isEmpty()
    }

    @Test
    fun testErrorWhenFirstParameterIsNotOnNewLine() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            fun f(a: Any,
                  b: Any,
                  c: Any) {
            }
            """.trimIndent(),
            configUserData
        )).isEqualTo(
            listOf(
                LintError(1, 7, expectedRuleId, expectedErrorMessage),
                LintError(2, 7, expectedRuleId, expectedIndentErrorMessage(6, 4)),
                LintError(3, 7, expectedRuleId, expectedIndentErrorMessage(6, 4)),
                LintError(3, 13, expectedRuleId, expectedParenthesesMessage)
            )
        )
    }

    @Test
    fun testFormatWhenFirstParameterIsNoOnNewLine() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().format(
            """
            fun f(a: Any,
                  b: Any,
                  c: Any) {
            }
            """.trimIndent(),
            configUserData
        )).isEqualTo(
            """
            fun f(
                a: Any,
                b: Any,
                c: Any
            ) {
            }
            """.trimIndent()
        )
    }

    @Test
    fun testIgnoreLambdaParameters() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            val fieldExample =
                  LongNameClass { paramA,
                                  paramB,
                                  paramC ->
                      ClassB(paramA, paramB, paramC)
                  }
            """.trimIndent(),
            configUserData
        )).isEmpty()
    }

    @Test
    fun testFailWhenWrongIndentIsUsed() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            class A {
                fun f(
                   a: Any,
                   b: Any) {
                }
            }
            """.trimIndent(),
            configUserData
        )).isEqualTo(
            listOf(
                LintError(3, 8, expectedRuleId, expectedIndentErrorMessage(3, 4)),
                LintError(4, 8, expectedRuleId, expectedIndentErrorMessage(3, 4)),
                LintError(4, 14, expectedRuleId, expectedParenthesesMessage)
            )
        )
    }

    @Test
    fun testRespectOuterIndent() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().format(
            """
            class A {
                fun f(a: Any,
                      b: Any,
                      c: Any) {
                }
            }
            """.trimIndent(),
            configUserData
        )).isEqualTo(
            """
            class A {
                fun f(
                    a: Any,
                    b: Any,
                    c: Any
                ) {
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testFailWhenHeaderIsTooLong() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().lint(
            """
            class WithLongClassHeader(parameter1: Int, parameter2: Int, parameter3: Int) {
              fun a() = ""
            }
            """.trimIndent(),
            mapOf("max_line_length" to "50")
        )).isEqualTo(
            listOf(
                LintError(1, 27, expectedRuleId, expectedErrorMessage),
                LintError(1, 44, expectedRuleId, expectedErrorMessage),
                LintError(1, 61, expectedRuleId, expectedErrorMessage),
                LintError(1, 76, expectedRuleId, expectedParenthesesMessage)
            )
        )
    }

    @Test
    fun testFormatClassWithAllCases() {
        Assertions.assertThat(ParametersOnSeparateLinesRule().format(
            """
            class WithLongClassHeader(parameter1: Int, parameter2: Int, parameter3: Int) {
                constructor(parameter1: Int, parameter2: Int, parameter3: Int) {
                }

                constructor() {
                    val defaultParameter1 = 0
                    val defaultParameter2 = 0
                    val defaultParameter3 = 0
                    this(defaultParameter1, defaultParameter2, defaultParameter3)
                }

                fun withLongDefinition(param1: Int, parameter2: Int, parameter3: Int): String {
                    return " "
                }

                fun a(param1: Int): String {
                    return "very long line that exceeds fifty characters, to make sure that this doesn't count"
                }
            }
            """.trimIndent(),
            mapOf("max_line_length" to "50")
        )).isEqualTo(
            """
            class WithLongClassHeader(
                parameter1: Int,
                parameter2: Int,
                parameter3: Int
            ) {
                constructor(
                    parameter1: Int,
                    parameter2: Int,
                    parameter3: Int
                ) {
                }

                constructor() {
                    val defaultParameter1 = 0
                    val defaultParameter2 = 0
                    val defaultParameter3 = 0
                    this(defaultParameter1, defaultParameter2, defaultParameter3)
                }

                fun withLongDefinition(
                    param1: Int,
                    parameter2: Int,
                    parameter3: Int
                ): String {
                    return " "
                }

                fun a(param1: Int): String {
                    return "very long line that exceeds fifty characters, to make sure that this doesn't count"
                }
            }
            """.trimIndent()
        )
    }
}
