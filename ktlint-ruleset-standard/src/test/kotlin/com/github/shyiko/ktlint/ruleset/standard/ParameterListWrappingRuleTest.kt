package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ParameterListWrappingRuleTest {

    @Test
    fun testLintClassParameterList() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            class ClassA(paramA: String, paramB: String,
                         paramC: String)
            """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 14, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 30, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(2, 14, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 13)"),
                LintError(2, 28, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testLintClassParameterListWhenMaxLineLengthExceeded() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent(),
            userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
            listOf(
                LintError(1, 14, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 30, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 46, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 60, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
        // corner case
        assertThat(
            ParameterListWrappingRule().lint(
            """
            class ClassA(paramA: String)
             class ClassA(paramA: String)
            class ClassA(paramA: String)
            """.trimIndent(),
            userData = mapOf("max_line_length" to "28")
            )
        ).isEqualTo(
            listOf(
                LintError(2, 15, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(2, 29, "parameter-list-wrapping", "Missing newline before \")\"")
            )
        )
    }

    @Test
    fun testLintClassParameterListValid() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testLintClassParameterListValidMultiLine() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            class ClassA(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormatClassParameterList() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            class ClassA(paramA: String, paramB: String,
                         paramC: String)
            """.trimIndent()
            )
        ).isEqualTo(
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
    fun testFormatClassParameterListWhenMaxLineLengthExceeded() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            class ClassA(paramA: String, paramB: String, paramC: String)
            """.trimIndent(),
            userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
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
    fun testLintFunctionParameterList() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            fun f(a: Any,
                  b: Any,
                  c: Any) {
            }
            """.trimIndent()
            )).isEqualTo(
            listOf(
                LintError(1, 7, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(2, 7, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 6)"),
                LintError(3, 7, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 6)"),
                LintError(3, 13, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testLintFunctionParameterListWhenMaxLineLengthExceeded() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            fun f(a: Any, b: Any, c: Any) {
            }
            """.trimIndent(),
            userData = mapOf("max_line_length" to "10")
            )).isEqualTo(
            listOf(
                LintError(1, 7, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 15, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 23, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(1, 29, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testFormatFunctionParameterList() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            fun f(a: Any,
                  b: Any,
                  c: Any) {
            }
            """.trimIndent()
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
    fun testFormatFunctionParameterListWhenMaxLineLengthExceeded() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            fun f(a: Any, b: Any, c: Any) {
            }
            """.trimIndent(),
            userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
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
    fun testLambdaParametersAreIgnored() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            val fieldExample =
                  LongNameClass { paramA,
                                  paramB,
                                  paramC ->
                      ClassB(paramA, paramB, paramC)
                  }
            """.trimIndent()
            )).isEmpty()
    }

    @Test
    fun testFormatPreservesIndent() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            class A {
                fun f(a: Any,
                      b: Any,
                      c: Any) {
                }
            }
            """.trimIndent()
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
    fun testFormatPreservesIndentWithAnnotations() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            class A {
                fun f(@Annotation
                      a: Any,
                      @Annotation([
                          "v1",
                          "v2"
                      ])
                      b: Any,
                      c: Any =
                          false,
                      @Annotation d: Any) {
                }
            }
            """.trimIndent()
            )).isEqualTo(
            """
            class A {
                fun f(
                    @Annotation
                    a: Any,
                    @Annotation([
                        "v1",
                        "v2"
                    ])
                    b: Any,
                    c: Any =
                        false,
                    @Annotation d: Any
                ) {
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testFormatCorrectsRPARIndentIfNeeded() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            class A {
                fun f(a: Any,
                      b: Any,
                      c: Any
                   ) {
                }
            }
            """.trimIndent()
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
    fun testFormatNestedDeclarations() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            fun visit(
                node: ASTNode,
                    autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String,
                canBeAutoCorrected: Boolean) -> Unit
            ) {}
            """.trimIndent()
            )).isEqualTo(
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
        )
    }

    @Test
    fun testFormatNestedDeclarationsWhenMaxLineLengthExceeded() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {}
            """.trimIndent(),
            userData = mapOf("max_line_length" to "10")
            )).isEqualTo(
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
        )
    }

    @Test
    fun testFormatNestedDeclarationsValid() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
            ) {}
            """.trimIndent()
            )).isEqualTo(
            """
            fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
            ) {}
            """.trimIndent()
        )
    }

    @Test
    fun testCommentsAreIgnored() {
        assertThat(ParameterListWrappingRule().lint(
            """
            data class A(
               /*
                * comment
                */
               //
               var v: String
            )
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(6, 4, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 3)")
        ))
    }

    @Test
    fun testLintClassDanglingLeftParen() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            class ClassA
            (
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "parameter-list-wrapping", """Unnecessary newline before "("""")
            )
        )
    }

    @Test
    fun testLintFunctionDanglingLeftParen() {
        assertThat(
            ParameterListWrappingRule().lint(
            """
            fun doSomething
            (
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "parameter-list-wrapping", """Unnecessary newline before "("""")
            )
        )
    }

    @Test
    fun testFormatClassDanglingLeftParen() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            class ClassA constructor
            (
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
            )
        ).isEqualTo(
            """
            class ClassA constructor(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        )
    }

    @Test
    fun testFormatFunctionDanglingLeftParen() {
        assertThat(
            ParameterListWrappingRule().format(
            """
            fun doSomething
            (
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
            )
        ).isEqualTo(
            """
            fun doSomething(
                paramA: String,
                paramB: String,
                paramC: String
            )
            """.trimIndent()
        )
    }
}
