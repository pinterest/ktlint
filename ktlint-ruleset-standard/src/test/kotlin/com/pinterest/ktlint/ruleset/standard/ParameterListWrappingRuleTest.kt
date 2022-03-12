package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.test.EditorConfigOverride
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@FeatureInAlphaState
class ParameterListWrappingRuleTest {
    private val rules: List<Rule> = listOf(
        ParameterListWrappingRule(),
        // In case a parameter is already wrapped to a separate line but is indented incorrectly then this indent will
        // only be corrected by the IndentationRule. The IndentationRule is executed in relevant tests for clarity.
        IndentationRule()
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
        assertThat(rules.lint(code)).contains(
            LintError(1, 14, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(1, 30, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(2, 1, "indent", "Unexpected indentation (13) (should be 4)"),
            LintError(2, 28, "parameter-list-wrapping", """Missing newline before ")"""")
        )
        assertThat(rules.format(code)).isEqualTo(formattedCode)
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
        assertThat(
            ParameterListWrappingRule().lint(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 10)
            )
        ).isEqualTo(
            listOf(
                LintError(1, 14, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 30, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 46, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 60, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
        assertThat(
            ParameterListWrappingRule().format(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 10)
            )
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a class header with a very long name and without parameters which exceeds the maximum line length then do not change the class header`() {
        val code =
            """
            class ClassAWithALongName()
            """.trimIndent()
        assertThat(
            ParameterListWrappingRule().lint(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 10)
            )
        ).isEmpty()
        assertThat(ParameterListWrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a single line class header with parameters which is formatted correctly then do not change the class header`() {
        val code =
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent()
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
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
        assertThat(rules.lint(code)).contains(
            LintError(1, 7, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(2, 1, "indent", "Unexpected indentation (6) (should be 4)"),
            LintError(2, 1, "indent", "Unexpected indentation (6) (should be 4)"),
            LintError(3, 13, "parameter-list-wrapping", """Missing newline before ")"""")
        )
        assertThat(rules.format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListWrappingRule().lint(code)).containsExactly(
            LintError(3, 13, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)")
        )
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
        assertThat(
            ParameterListWrappingRule().lint(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 10)
            )
        ).isEqualTo(
            listOf(
                LintError(1, 7, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 15, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 23, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 29, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
        assertThat(
            ParameterListWrappingRule().format(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 10)
            )
        ).isEqualTo(formattedCode)
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
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
        assertThat(rules.lint(code)).contains(
            LintError(2, 11, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(3, 1, "indent", "Unexpected indentation (10) (should be 8)"),
            LintError(4, 1, "indent", "Unexpected indentation (10) (should be 8)"),
            LintError(5, 1, "indent", "Unexpected indentation (10) (should be 8)"),
            LintError(6, 1, "indent", "Unexpected indentation (10) (should be 8)"),
            LintError(6, 19, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(6, 37, "parameter-list-wrapping", """Missing newline before ")"""")
        )
        assertThat(rules.format(code)).isEqualTo(formattedCode)
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
        assertThat(rules.lint(code)).contains(
            LintError(2, 11, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(3, 1, "indent", "Unexpected indentation (10) (should be 8)"),
            LintError(4, 1, "indent", "Unexpected indentation (10) (should be 8)"),
            LintError(5, 1, "indent", "Unexpected indentation (7) (should be 4)")
        )
        assertThat(rules.format(code)).isEqualTo(formattedCode)
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
        assertThat(rules.lint(code)).contains(
            LintError(3, 1, "indent", "Unexpected indentation (8) (should be 4)"),
            LintError(4, 12, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(4, 25, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
            LintError(5, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(5, 32, "parameter-list-wrapping", """Missing newline before ")""""),
            LintError(5, 41, "parameter-list-wrapping", """Missing newline before ")"""")
        )
        assertThat(rules.format(code)).isEqualTo(formattedCode)
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
        assertThat(
            ParameterListWrappingRule().format(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 10)
            )
        ).isEqualTo(formattedCode)
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
        assertThat(ParameterListWrappingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
        assertThat(ParameterListWrappingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListWrappingRule().lint(code)).containsExactly(
            LintError(2, 1, "parameter-list-wrapping", """Unnecessary newline before "("""")
        )
        assertThat(ParameterListWrappingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListWrappingRule().lint(code)).containsExactly(
            LintError(2, 1, "parameter-list-wrapping", """Unnecessary newline before "("""")
        )
        assertThat(ParameterListWrappingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
        assertThat(ParameterListWrappingRule().format(code)).isEqualTo(code)
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
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
        assertThat(ParameterListWrappingRule().lint(code)).isEmpty()
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
        assertThat(
            ParameterListWrappingRule().lint(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 80)
            )
        ).containsExactly(
            LintError(1, 22, "parameter-list-wrapping", "Parameter of nullable type should be on a separate line (unless the type fits on a single line)"),
            LintError(1, 95, "parameter-list-wrapping", """Missing newline before ")"""")
        )
        assertThat(
            ParameterListWrappingRule().format(
                code,
                EditorConfigOverride.from(maxLineLengthProperty to 80)
            )
        ).isEqualTo(formattedCode)
    }
}
