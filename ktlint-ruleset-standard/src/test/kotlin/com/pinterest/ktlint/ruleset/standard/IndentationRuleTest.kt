package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class IndentationRuleTest {

    @Test
    fun testLint() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format.kt.spec",
                "spec/indent/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatTabs() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format.kt.spec",
                "spec/indent/format-expected-tabs.kt.spec",
                mapOf("indent_style" to "tab")
            )
        ).isEmpty()
    }

    @Test
    fun testLintIndentSize2() {
        assertThat(
            IndentationRule().lint(
                """
                fun main() {
                   val v = ""
                    println(v)
                }
                fun main() {
                  val v = ""
                  println(v)
                }
                class A {
                  var x: String
                    get() = ""
                    set(v: String) { x = v }
                }
                """.trimIndent(),
                mapOf("indent_size" to "2")
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected indentation (3) (should be 2)"),
                LintError(3, 1, "indent", "Unexpected indentation (4) (should be 2)")
            )
        )
    }

    @Test
    fun testLintIndentTab() {
        assertThat(
            IndentationRule().lint(
                """
                |fun main() {
                |val v = ""
                |		println(v)
                |}
                |fun main() {
                |	val v = ""
                |	println(v)
                |}
                |class A {
                |	var x: String
                |		get() = ""
                |		set(v: String) { x = v }
                |}
                |""".trimMargin(),
                mapOf("indent_style" to "tab")
            )
        ).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected indentation (0) (should be 1)"),
                LintError(3, 1, "indent", "Unexpected indentation (2) (should be 1)")
            )
        )
    }

    @Test
    fun testLintIndentSizeUnset() {
        assertThat(
            IndentationRule().lint(
                """
                fun main() {
                   val v = ""
                    println(v)
                }
                """.trimIndent(),
                mapOf("indent_size" to "unset")
            )
        ).isEmpty()
    }

    @Test
    fun testLintArgumentList() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-argument-list.kt.spec")).isEmpty()
    }

    @Test
    fun testFormatKDoc() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-kdoc.kt.spec",
                "spec/indent/format-kdoc-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testLintComment() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-comment.kt.spec")).isEmpty()
    }

    @Test
    fun testLintCondition() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-condition.kt.spec")).isEmpty()
    }

    @Test
    fun testLintPropertyAccessor() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-property-accessor.kt.spec")).isEmpty()
    }

    @Test
    fun testFormatPropertyAccessor() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-property-accessor.kt.spec",
                "spec/indent/format-property-accessor-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatRawStringTrimIndent() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-raw-string-trim-indent.kt.spec",
                "spec/indent/format-raw-string-trim-indent-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testLintString() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-string.kt.spec")).isEmpty()
    }

    @Test
    fun testLintSuperType() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-supertype.kt.spec")).isEmpty()
    }

    @Test
    fun testFormatSuperType() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-supertype.kt.spec",
                "spec/indent/format-supertype-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testLintFirstLine() {
        assertThat(IndentationRule().lint("  // comment")).hasSize(1)
        assertThat(IndentationRule().lint("  // comment", script = true)).hasSize(1)
        assertThat(IndentationRule().lint("  \n  // comment")).hasSize(1)
        assertThat(IndentationRule().lint("  \n  // comment", script = true)).hasSize(1)
    }

    @Test
    fun testLintBinaryExpression() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-binary-expression.kt.spec")).isEmpty()
    }

    @Test
    fun testFormatBinaryExpression() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-binary-expression.kt.spec",
                "spec/indent/format-binary-expression-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testLintWhenExpression() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-when-expression.kt.spec")).isEmpty()
    }

    @Test
    fun testLintControlBody() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-control-body.kt.spec")).isEmpty()
    }

    @Test
    fun testLintDotQualifiedExpression() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-dot-qualified-expression.kt.spec")).isEmpty()
    }

    @Test
    fun testFormatDotQualifiedExpression() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-dot-qualified-expression.kt.spec",
                "spec/indent/format-dot-qualified-expression-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatMultilineString() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-multiline-string.kt.spec",
                "spec/indent/format-multiline-string-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatArrow() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-arrow.kt.spec",
                "spec/indent/format-arrow-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatEq() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-eq.kt.spec",
                "spec/indent/format-eq-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatParameterList() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-parameter-list.kt.spec",
                "spec/indent/format-parameter-list-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatArgumentList() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-argument-list.kt.spec",
                "spec/indent/format-argument-list-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test // "https://github.com/shyiko/ktlint/issues/180"
    fun testLintWhereClause() {
        assertThat(
            IndentationRule().lint(
                """
                class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
                    val adapter1: A1,
                    val adapter2: A2
                ) : RecyclerView.Adapter<C>()
                    where A1 : RecyclerView.Adapter<V1>, A1 : ComposableAdapter.ViewTypeProvider,
                          A2 : RecyclerView.Adapter<V2>, A2 : ComposableAdapter.ViewTypeProvider {
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test // "https://github.com/pinterest/ktlint/issues/433"
    fun testLintParameterListWithComments() {
        assertThat(
            IndentationRule().lint(
                """
                fun main() {
                    foo(
                        /*param1=*/param1,
                        /*param2=*/param2
                    )

                    foo(
                        /*param1=*/ param1,
                        /*param2=*/ param2
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun testUnexpectedSpaceCharacter() {
        val ktScript = "fun main() {\n    return 0\n  }"
        assertThat(IndentationRule().lint(ktScript, mapOf("indent_style" to "tab"))).isEqualTo(
            listOf(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected space character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected space character(s)")
            )
        )
        assertThat(IndentationRule().format(ktScript))
            .isEqualTo("fun main() {\n    return 0\n}")
    }

    @Test
    fun testUnexpectedTabCharacter() {
        val ktScript = "fun main() {\n\t\treturn 0\n\t}"
        assertThat(IndentationRule().lint(ktScript)).isEqualTo(
            listOf(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected indentation (8) (should be 4)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected indentation (4) (should be 0)")
            )
        )
        assertThat(IndentationRule().format(ktScript))
            .isEqualTo("fun main() {\n    return 0\n}")
    }

    @Test
    fun testUnexpectedTabCharacterWithCustomIndentSize() {
        val ktScript = "fun main() {\n\t\treturn 0\n\t}"
        assertThat(IndentationRule().lint(ktScript, mapOf("indent_size" to "2"))).isEqualTo(
            listOf(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected indentation (4) (should be 2)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected indentation (2) (should be 0)")
            )
        )
        assertThat(IndentationRule().format(ktScript, mapOf("indent_size" to "2")))
            .isEqualTo("fun main() {\n  return 0\n}")
    }

    @Test
    fun testLintNewlineAfterEqAllowed() {
        assertThat(
            IndentationRule().lint(
                // Previously the IndentationRule would force the line break after the `=`. Verify that it is
                // still allowed.
                """
                private fun getImplementationVersion() =
                    javaClass.`package`.implementationVersion
                        ?: javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")
                            ?.let { stream ->
                                Manifest(stream).mainAttributes.getValue("Implementation-Version")
                            }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `no indentation after lambda arrow`() {
        assertThat(
            IndentationRule().lint(
                """
                fun bar() {
                    foo.func {
                        param1, param2 ->
                            doSomething()
                            doSomething2()
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected indentation (12) (should be 8)"),
                LintError(line = 5, col = 1, ruleId = "indent", detail = "Unexpected indentation (12) (should be 8)")
            )
        )
    }

    @Test
    fun `lint indentation new line before return type`() {
        assertThat(
            IndentationRule().lint(
                """
                abstract fun doPerformSomeOperation(param: ALongParameter):
                    SomeLongInterface<ALongParameter.InnerClass, SomeOtherClass>
                val s:
                    String = ""
                fun process(
                    fileName:
                        String
                ): List<Output>
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format indentation new line before return type`() {
        assertThat(
            IndentationRule().format(
                """
                abstract fun doPerformSomeOperation(param: ALongParameter):
                SomeLongInterface<ALongParameter.InnerClass, SomeOtherClass>

                val s:
                        String = ""

                fun process(
                    fileName:
                        String
                ): List<Output>
                """.trimIndent()
            )
        ).isEqualTo(
            """
            abstract fun doPerformSomeOperation(param: ALongParameter):
                SomeLongInterface<ALongParameter.InnerClass, SomeOtherClass>

            val s:
                String = ""

            fun process(
                fileName:
                    String
            ): List<Output>
            """.trimIndent()
        )
    }

    @Test
    fun `lint trailing comment in multiline parameter is allowed`() {
        assertThat(
            IndentationRule().lint(
                """
                fun foo(param: Foo, other: String) {
                    foo(
                        param = param
                            .copy(foo = ""), // A comment
                        other = ""
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format trailing comment in multiline parameter is allowed`() {
        val code =
            """
            fun foo(param: Foo, other: String) {
                foo(
                    param = param
                        .copy(foo = ""), // A comment
                    other = ""
                )
            }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint safe-called wrapped trailing lambda is allowed`() {
        assertThat(
            IndentationRule().lint(
                """
                val foo = bar
                    ?.filter { number ->
                        number == 0
                    }?.map { evenNumber ->
                        evenNumber * evenNumber
                    }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format safe-called wrapped trailing lambda is allowed`() {
        val code =
            """
            val foo = bar
                ?.filter { number ->
                    number == 0
                }?.map { evenNumber ->
                    evenNumber * evenNumber
                }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint block started with parens after if is allowed`() {
        val code =
            """
            fun test() {
                if (true)
                    (1).toString()
                else
                    2.toString()
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format block started with parens after if is allowed`() {
        val code =
            """
            fun test() {
                if (true)
                    (1).toString()
                else
                    2.toString()
            }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    // https://github.com/pinterest/ktlint/issues/796
    @Test
    fun `lint if-condition with multiline call expression is indented properly`() {
        val code =
            """
            private val gpsRegion =
                if (permissionHandler.isPermissionGranted(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    // stuff
                }
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format if-condition with multiline call expression is indented properly`() {
        val code =
            """
            private val gpsRegion =
                if (permissionHandler.isPermissionGranted(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    // stuff
                }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }
}
