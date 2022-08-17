package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("RemoveCurlyBracesFromTemplate")
class SpacingAroundOperatorsRuleTest {
    private val spacingAroundOperatorsRuleAssertThat = assertThatRule { SpacingAroundOperatorsRule() }

    @ParameterizedTest(name = "Operator: {0}")
    @ValueSource(
        strings = [
            "+",
            "-",
            "/",
            "*",
            "<",
            "<=",
            ">",
            ">=",
            "==",
            "===",
            "!=",
            "!==",
        ],
    )
    fun `Given a simple operator`(operator: String) {
        val code =
            """
            val foo1 = 1${operator}2
            val foo2 = 1${operator} 2
            val foo3 = 1 ${operator}2
            val foo4 = 1 ${operator} 2
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = 1 ${operator} 2
            val foo2 = 1 ${operator} 2
            val foo3 = 1 ${operator} 2
            val foo4 = 1 ${operator} 2
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 13, "Missing spacing around \"${operator}\""),
                LintViolation(2, 13, "Missing spacing before \"${operator}\""),
                LintViolation(3, 15, "Missing spacing after \"${operator}\""),
            ).isFormattedAs(formattedCode)
    }

    @ParameterizedTest(name = "Operator: {0}")
    @ValueSource(
        strings = [
            "+",
            "++",
            "-",
            "--",
            "!",
        ],
    )
    fun `Given a unary prefix operator`(operator: String) {
        val code =
            """
            val foo = ${operator}1
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = "Operator: {0}")
    @ValueSource(
        strings = [
            "++",
            "--",
        ],
    )
    fun `Given a unary postfix operator`(operator: String) {
        val code =
            """
            val foo = 1${operator}
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a unary operator`() {
        val code =
            """
            val foo1 = +1
            val foo2 = -1
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given the in operator`() {
        val code =
            """
            var x = 1 in 3..4
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function with default parameter value`() {
        val code =
            """
            fun foo1(a=true)
            fun foo2(a= true)
            fun foo3(a =true)
            fun foo4(a = true)
            """.trimIndent()
        val formattedCode =
            """
            fun foo1(a = true)
            fun foo2(a = true)
            fun foo3(a = true)
            fun foo4(a = true)
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Missing spacing around \"=\""),
                LintViolation(2, 11, "Missing spacing before \"=\""),
                LintViolation(3, 13, "Missing spacing after \"=\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some generic type then do not handle angle brackets as comparator operator`() {
        val code =
            """
            val foo = fn(arrayOfNulls<Any>(0))
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some assignment operators missing spaces around the operator`() {
        val code =
            """
            val foo1="foo"
            val foo2= "foo"
            val foo3 ="foo"
            val foo4 = "foo"
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = "foo"
            val foo2 = "foo"
            val foo3 = "foo"
            val foo4 = "foo"
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 9, "Missing spacing around \"=\""),
                LintViolation(2, 9, "Missing spacing before \"=\""),
                LintViolation(3, 11, "Missing spacing after \"=\""),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class AugmentedAssignmentOperators {
        @Test
        fun `Given some augmented assignment operators missing spaces around the operator`() {
            val code =
                """
                fun main() {
                    var i = 1
                    i+=1
                    i-=1
                    i/=1
                    i*=1
                }
                """.trimIndent()
            val formattedCode =
                """
                fun main() {
                    var i = 1
                    i += 1
                    i -= 1
                    i /= 1
                    i *= 1
                }
                """.trimIndent()
            spacingAroundOperatorsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 6, "Missing spacing around \"+=\""),
                    LintViolation(4, 6, "Missing spacing around \"-=\""),
                    LintViolation(5, 6, "Missing spacing around \"/=\""),
                    LintViolation(6, 6, "Missing spacing around \"*=\""),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some augmented assignment operators missing spaces before the operator`() {
            val code =
                """
                fun main() {
                    var i = 1
                    i+= 1
                    i-= 1
                    i/= 1
                    i*= 1
                }
                """.trimIndent()
            val formattedCode =
                """
                fun main() {
                    var i = 1
                    i += 1
                    i -= 1
                    i /= 1
                    i *= 1
                }
                """.trimIndent()
            spacingAroundOperatorsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 6, "Missing spacing before \"+=\""),
                    LintViolation(4, 6, "Missing spacing before \"-=\""),
                    LintViolation(5, 6, "Missing spacing before \"/=\""),
                    LintViolation(6, 6, "Missing spacing before \"*=\""),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some augmented assignment operators missing spaces after the operator`() {
            val code =
                """
                fun main() {
                    var i = 1
                    i +=1
                    i -=1
                    i /=1
                    i *=1
                }
                """.trimIndent()
            val formattedCode =
                """
                fun main() {
                    var i = 1
                    i += 1
                    i -= 1
                    i /= 1
                    i *= 1
                }
                """.trimIndent()
            spacingAroundOperatorsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 8, "Missing spacing after \"+=\""),
                    LintViolation(4, 8, "Missing spacing after \"-=\""),
                    LintViolation(5, 8, "Missing spacing after \"/=\""),
                    LintViolation(6, 8, "Missing spacing after \"*=\""),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a wildcard import then do no require spacing around the wildcard`() {
        val code =
            """
            import a.b.*
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a spread operator as function parameter value then do no require spacing around the spread operator`() {
        val code =
            """
            fun main() {
                call(*v)
                call(1, *v, 2)
            }
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a type argument list, containing a wildcard type then do not require space around the wildcard`() {
        val code =
            """
            val foo = Map<PropertyType<*>>
            """.trimIndent()
        spacingAroundOperatorsRuleAssertThat(code).hasNoLintViolations()
    }
}
