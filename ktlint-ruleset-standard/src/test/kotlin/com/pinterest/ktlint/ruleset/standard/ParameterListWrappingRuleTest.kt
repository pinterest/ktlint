package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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
    fun testLintClassParameterListWhenMaxLineLengthExceededAndNoParameters() {
        assertThat(
            ParameterListWrappingRule().lint(
                """
                class ClassAWithALongName()
                """.trimIndent(),
                userData = mapOf("max_line_length" to "10")
            )
        ).doesNotContain(
            LintError(1, 27, "parameter-list-wrapping", """Missing newline before ")"""")
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
            )
        ).isEqualTo(
            listOf(
                LintError(1, 7, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)"),
                LintError(2, 7, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 6)"),
                LintError(3, 7, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 6)"),
                LintError(3, 13, "parameter-list-wrapping", """Missing newline before ")"""")
            )
        )
    }

    @Test
    fun testLintFunctionParameterInconsistency() {
        assertThat(
            ParameterListWrappingRule().lint(
                """
                    fun f(
                        a: Any,
                        b: Any, c: Any
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(3, 13, "parameter-list-wrapping", "Parameter should be on a separate line (unless all parameters can fit a single line)")
            )
        )
    }

    @Test
    fun testLintArgumentInconsistency() {
        assertThat(
            ParameterListWrappingRule().lint(
                """
                    val x = f(
                        a,
                        b, c
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(3, 8, "parameter-list-wrapping", "Argument should be on a separate line (unless all arguments can fit a single line)")
            )
        )
    }

    @Test
    fun testFormatArgumentInconsistency() {
        assertThat(
            ParameterListWrappingRule().format(
                """
                    val x = f(
                        a,
                        b, c
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            """
                val x = f(
                    a,
                    b,
                    c
                )
            """.trimIndent()
        )
    }

    @Test
    fun testFormatArgumentsWithNestedCalls() {
        assertThat(
            ParameterListWrappingRule().format(
                """
                    val x = test(
                        one("a", "b",
                        "c"),
                        "Two", "Three", "Four"
                    )
                """.trimIndent()
            )
        ).isEqualTo(
            """
                val x = test(
                    one(
                        "a",
                        "b",
                        "c"
                    ),
                    "Two",
                    "Three",
                    "Four"
                )
            """.trimIndent()
        )
    }

    @Test
    fun testLintArgumentListWhenMaxLineLengthExceeded() {
        assertThat(
            ParameterListWrappingRule().lint(
                """
                    val x = f(a, b, c)
                """.trimIndent(),
                userData = mapOf("max_line_length" to "10")
            )
        ).isEqualTo(
            listOf(
                LintError(1, 11, "parameter-list-wrapping", "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintError(1, 14, "parameter-list-wrapping", "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintError(1, 17, "parameter-list-wrapping", "Argument should be on a separate line (unless all arguments can fit a single line)"),
                LintError(1, 18, "parameter-list-wrapping", """Missing newline before ")"""")
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
            )
        ).isEqualTo(
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
            )
        ).isEmpty()
    }

    @Test
    fun testLambdaArgumentsAreIgnored() {
        assertThat(
            ParameterListWrappingRule().lint(
                """
                abstract class A(init: String.() -> Int)
                class B : A({
                    toInt()
                })

                fun test(a: Any, b: (Any) -> Any) {
                    test(a = "1", b = {
                        it.toString()
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testFormatWithLambdaArguments() {
        assertThat(
            ParameterListWrappingRule().format(
                """
                abstract class A(init: String.() -> Int)
                class B : A({
                    toInt()
                })

                fun test(a: Any, b: (Any) -> Any) {
                    test(
                        a = "1", b = {
                        it.toString()
                    })
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test(a = "1", b = {
                        it.toString()
                    }, c = 123)
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test(a = "1", b = {
                        it.toString()
                    },
                    c = 123)
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test("1",
                        { val x = it.toString(); x }, 123)
                }

                fun test(a: Any, b: (Any) -> Any, c: Any) {
                    test(
                        "1",
                        {
                            f(1,
                                { "stuff" }, 3)
                        },
                        123
                    )
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            abstract class A(init: String.() -> Int)
            class B : A({
                toInt()
            })

            fun test(a: Any, b: (Any) -> Any) {
                test(
                    a = "1",
                    b = {
                        it.toString()
                    }
                )
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(a = "1", b = {
                    it.toString()
                }, c = 123)
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    a = "1",
                    b = {
                        it.toString()
                    },
                    c = 123
                )
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    "1",
                    { val x = it.toString(); x },
                    123
                )
            }

            fun test(a: Any, b: (Any) -> Any, c: Any) {
                test(
                    "1",
                    {
                        f(
                            1,
                            { "stuff" },
                            3
                        )
                    },
                    123
                )
            }
            """.trimIndent()
        )
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
            )
        ).isEqualTo(
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
    fun testFormatPreservesIndentWithAnnotationsOnSingleLine() {
        assertThat(
            ParameterListWrappingRule().format(
                """
                class A {
                    fun f(@Annotation
                          a: Any,
                          @Annotation(["v1", "v2"])
                          b: Any,
                          c: Any =
                              false,
                          @Annotation d: Any) {
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A {
                fun f(
                    @Annotation
                    a: Any,
                    @Annotation(["v1", "v2"])
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
    fun testFormatPreservesIndentWithAnnotationsOnMultiLine() {
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
            )
        ).isEqualTo(
            """
            class A {
                fun f(
                    @Annotation
                    a: Any,
                    @Annotation(
                        [
                            "v1",
                            "v2"
                        ]
                    )
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
            )
        ).isEqualTo(
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
            )
        ).isEqualTo(
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
            )
        ).isEqualTo(
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
            )
        ).isEqualTo(
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
        assertThat(
            ParameterListWrappingRule().lint(
                """
                data class A(
                   /*
                    * comment
                    */
                   //
                   var v: String
                )
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(6, 4, "parameter-list-wrapping", "Unexpected indentation (expected 4, actual 3)")
            )
        )
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

    @Test
    fun testLintVarargIsIgnored() {
        assertThat(
            ParameterListWrappingRule().lint(
                """
                private val tokenSet = TokenSet.create(
                    MUL, PLUS, MINUS, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ,
                    EXCLEQ, ANDAND, OROR, ELVIS, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, ARROW
                )
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/680
    @Test
    fun `multiline type parameter list in function signature - indented correctly`() {
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
}
