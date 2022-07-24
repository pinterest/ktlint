package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ParameterListWrappingRuleTest {
    private val parameterListWrappingRuleAssertThat =
        assertThatRule(
            provider = { ParameterListWrappingRule() },
            additionalRuleProviders = setOf(
                // Apply the IndentationRule always as additional rule, so that the formattedCode in the unit test looks
                // correct.
                RuleProvider { IndentationRule() }
            )
        )

    @Test
    fun `Given a class with parameters on multiple lines then put each parameter and closing parenthesis on a separate line`() {
        val code =
            """
            class ClassA(paramA: String, paramB: String,
                         paramC: String)
            """.trimIndent()
        val formattedCode =
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 14, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 30, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(2, 28, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line class header which exceeds the maximum line length`() {
        val code =
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent()
        val formattedCode =
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 10)
            .hasLintViolations(
                LintViolation(1, 14, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 30, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 46, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 60, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class header with a very long name and without parameters which exceeds the maximum line length then do not change the class header`() {
        val code =
            """
            class ClassAWithALongName()
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 10)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a single line class header with parameters which is formatted correctly then do not change the class header`() {
        val code =
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multiline class header with parameters which is formatted correctly then do not change the class header`() {
        val code =
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multiline function with parameters then start each parameter and closing parenthesis on a separate line`() {
        val code =
            """
            fun f(a: Any,
                  b: Any,
                  c: Any) {
            }
            """.trimIndent()
        val formattedCode =
            """
            fun f(
                a: Any,
                b: Any,
                c: Any
            ) {
            }
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 7, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(3, 13, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline function with parameters wheren some parameters are on the same line then start each parameter and closing parenthesis on a separate line`() {
        val code =
            """
            fun f(
                a: Any,
                b: Any, c: Any
            )
            """.trimIndent()
        val formattedCode =
            """
            fun f(
                a: Any,
                b: Any,
                c: Any
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolation(
                3,
                13,
                "Parameter should be on a separate line (unless all parameters can fit a single line)"
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function which exceeds the maximum line length then start each parameter and the closing parenthesis on a separate line`() {
        val code =
            """
            fun f(a: Any, b: Any, c: Any) {
            }
            """.trimIndent()
        val formattedCode =
            """
            fun f(
                a: Any,
                b: Any,
                c: Any
            ) {
            }
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 10)
            .hasLintViolations(
                LintViolation(1, 7, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 15, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 23, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 29, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with lambda parameters then do not reformat`() {
        val code =
            """
            val fieldExample =
                LongNameClass { paramA,
                                paramB,
                                paramC ->
                    ClassB(paramA, paramB, paramC)
                }
            """.trimIndent()
        val parameterListWrappingRuleWithoutIndentationRule = assertThatRule { ParameterListWrappingRule() }
        parameterListWrappingRuleWithoutIndentationRule(code).hasNoLintViolations()
        // IndentationRule does alter the code while the code is accepted by the Default IDEA formatter. So statement
        // below would fail!
        // parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function with annotated parameters then start each parameter on a separate line but preserve spacing between annotation and parameter name`() {
        val code =
            """
            class A {
                fun f(@Annotation
                      a: Any,
                      b: Any,
                      @Annotation
                      c: Any, @Annotation d: Any) {
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
                fun f(
                    @Annotation
                    a: Any,
                    b: Any,
                    @Annotation
                    c: Any,
                    @Annotation d: Any
                ) {
                }
            }
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 10)
            .hasLintViolations(
                LintViolation(2, 11, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(6, 19, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(6, 37, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline function signature with each parameter, except first, and closing parenthesis on separate lines then reformat properly`() {
        val code =
            """
            class A {
                fun f(a: Any,
                      b: Any,
                      c: Any
                   ) {
                }
            }
            """.trimIndent()
        val formattedCode =
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
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolation(
                2,
                11,
                "Parameter should be on a separate line (unless all parameters can fit a single line)"
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline function with nested declarations then format each parameter and closing parenthesis on a separate line`() {
        val code =
            """
            fun visit(
                node: ASTNode,
                    autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String,
                canBeAutoCorrected: Boolean) -> Unit) {}
            """.trimIndent()
        val formattedCode =
            """
            fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (
                    offset: Int,
                    errorMessage: String,
                    canBeAutoCorrected: Boolean
                ) -> Unit
            ) {}
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 12, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(4, 25, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(5, 32, """Missing newline before ")""""),
                LintViolation(5, 41, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function with nested declarations which exceeds the maximum line length then format each parameter and closing parenthesis on a separate line`() {
        val code =
            """
            fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {}
            """.trimIndent()
        val formattedCode =
            """
            fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (
                    offset: Int,
                    errorMessage: String,
                    canBeAutoCorrected: Boolean
                ) -> Unit
            ) {}
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 10)
            .hasLintViolations(
                LintViolation(1, 11, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 26, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 48, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 55, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 68, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 90, "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintViolation(1, 117, "Missing newline before \")\""),
                LintViolation(1, 126, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function with nested declarations which is formatted correct then do not change the function signature`() {
        val code =
            """
            fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
            ) {}
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun testCommentsAreIgnored() {
        val code =
            """
            data class A(
                /*
                 * comment
                 */
                //
                var v: String
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class declaration with a dangling opening parenthesis`() {
        val code =
            """
            class ClassA
            (
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        val formattedCode =
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolation(2, 1, """Unnecessary newline before "("""")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function declaration with a dangling opening parenthesis`() {
        val code =
            """
            fun doSomething
            (
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        val formattedCode =
            """
            fun doSomething(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .hasLintViolation(2, 1, """Unnecessary newline before "("""")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 680 - multiline type parameter list in function signature - indented correctly`() {
        val code =
            """
            object TestCase {
                inline fun <
                    T1,
                    T2,
                    T3> create(
                    t1: T1,
                    t2: T2,
                    t3: T3
                ) {
                    // do things
                }
            }
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class with a multiline type parameter which is indented correctly then do not reformat`() {
        val code =
            """
            // https://github.com/pinterest/ktlint/issues/921
            class ComposableLambda<
                P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16,
                P17, P18, R>(
                val key: Int,
                private val tracked: Boolean,
                private val sourceInformation: String?
            )
            // https://github.com/pinterest/ktlint/issues/938
            class GenericTypeWithALongLongALong1
            class GenericTypeWithALongLongALong2
            class GenericTypeWithALongLongALong3
            class ViewModelWithALongLongLongLongLongLongLongLongName3<
                A : GenericTypeWithALongLongALong1,
                B : GenericTypeWithALongLongALong2,
                C : GenericTypeWithALongLongALong3
                > constructor(
                parameterWithLongLongLongLongLongLongLongLongNameA: A,
                parameterWithLongLongLongLongLongLongLongLongNameB: B,
                parameterWithLongLongLongLongLongLongLongLongNameC: C
            )
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `multiline type argument list in function signature`() {
        val code =
            """
            class Foo<A, B, C>

            fun Foo<String, Boolean,
                Int>.bar(
                i: Int
            ) = apply {
            }
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1255 - Given a variable declaration for nullable function type which exceeds the max-line-length then wrap the function type to a new line`() {
        val code =
            """
            var changesListener: ((width: Double?, depth: Double?, length: Double?, area: Double?) -> Unit)? = null
            """.trimIndent()
        val formattedCode =
            """
            var changesListener: (
                (width: Double?, depth: Double?, length: Double?, area: Double?) -> Unit
            )? = null
            """.trimIndent()
        parameterListWrappingRuleAssertThat(code)
            .withEditorConfigOverride(maxLineLengthProperty to 80)
            .hasLintViolations(
                LintViolation(1, 22, "Parameter of nullable type should be on a separate line (unless the type fits on a single line)"),
                LintViolation(1, 95, """Missing newline before ")"""")
            ).isFormattedAs(formattedCode)
    }
}
