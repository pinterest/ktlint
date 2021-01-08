package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.assertThatFileFormat
import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

internal class IndentationRuleTest {

    @Test
    fun testLint() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint.kt.spec")).isEmpty()
    }

    @Test
    fun `format unindented input`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format.kt.spec",
                "spec/indent/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format unindented input with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format.kt.spec",
                "spec/indent/format-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
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
                INDENT_STYLE_TABS
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
    fun `format KDoc`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-kdoc.kt.spec",
                "spec/indent/format-kdoc-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format KDoc with Tabs`() {
        IndentationRule().assertThatFileFormat(
            "spec/indent/format-kdoc.kt.spec",
            "spec/indent/format-kdoc-expected-tabs.kt.spec",
            INDENT_STYLE_TABS
        )
    }

    @Test
    fun testLintComment() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-comment.kt.spec")).isEmpty()
    }

    @Test
    fun `lint KDoc comment`() {
        val code = """
        class Foo {
              /**
                *
                 */
            fun foo() {}
        }
        """.trimIndent()
        assertThat(IndentationRule().lint(code))
            .isEqualTo(
                listOf(
                    LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected indentation (6) (should be 4)"),
                    LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected indentation (8) (should be 5)"),
                    LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected indentation (9) (should be 5)"),
                )
            )
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
    fun `format property accessor`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-property-accessor.kt.spec",
                "spec/indent/format-property-accessor-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format property accessor with tabs`() {
        IndentationRule().assertThatFileFormat(
            "spec/indent/format-property-accessor.kt.spec",
            "spec/indent/format-property-accessor-expected-tabs.kt.spec",
            INDENT_STYLE_TABS
        )
    }

    @Test
    fun `format raw string followed by trim indent`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-raw-string-trim-indent.kt.spec",
                "spec/indent/format-raw-string-trim-indent-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format raw string followed by trim indent with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-raw-string-trim-indent.kt.spec",
                "spec/indent/format-raw-string-trim-indent-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
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
    fun `format SuperType`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-supertype.kt.spec",
                "spec/indent/format-supertype-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format SuperType with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-supertype.kt.spec",
                "spec/indent/format-supertype-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
            )
        ).isEmpty()
    }

    @Test
    fun testLintFirstLine() {
        assertThat(IndentationRule().lint("  // comment"))
            .isEqualTo(
                listOf(
                    LintError(line = 1, col = 1, ruleId = "indent", detail = "Unexpected indentation (2) (should be 0)"),
                )
            )
        assertThat(IndentationRule().lint("  // comment", script = true))
            .isEqualTo(
                listOf(
                    LintError(line = 1, col = 1, ruleId = "indent", detail = "Unexpected indentation (2) (should be 0)"),
                )
            )
        assertThat(IndentationRule().lint("  \n  // comment"))
            .isEqualTo(
                listOf(
                    LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected indentation (2) (should be 0)"),
                )
            )
        assertThat(IndentationRule().lint("  \n  // comment", script = true))
            .isEqualTo(
                listOf(
                    LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected indentation (2) (should be 0)"),
                )
            )
    }

    @Test
    fun testLintBinaryExpression() {
        assertThat(IndentationRule().diffFileLint("spec/indent/lint-binary-expression.kt.spec")).isEmpty()
    }

    @Test
    fun `test format Binary Expression`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-binary-expression.kt.spec",
                "spec/indent/format-binary-expression-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `test format Binary Expression with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-binary-expression.kt.spec",
                "spec/indent/format-binary-expression-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
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
    fun `format DotQualifiedExpression`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-dot-qualified-expression.kt.spec",
                "spec/indent/format-dot-qualified-expression-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format DotQualifiedExpression with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-dot-qualified-expression.kt.spec",
                "spec/indent/format-dot-qualified-expression-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
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
    fun testFormatMultilineStringTabs() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-multiline-string.kt.spec",
                "spec/indent/format-multiline-string-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
            )
        ).isEmpty()
    }

    @Test
    fun `format Arrow`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-arrow.kt.spec",
                "spec/indent/format-arrow-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format Arrow with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-arrow.kt.spec",
                "spec/indent/format-arrow-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
            )
        ).isEmpty()
    }

    @Test
    fun `format Eq`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-eq.kt.spec",
                "spec/indent/format-eq-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format Eq with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-eq.kt.spec",
                "spec/indent/format-eq-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
            )
        ).isEmpty()
    }

    @Test
    fun `format ParameterList`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-parameter-list.kt.spec",
                "spec/indent/format-parameter-list-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format ParameterList with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-parameter-list.kt.spec",
                "spec/indent/format-parameter-list-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
            )
        ).isEmpty()
    }

    @Test
    fun `format ArgumentList`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-argument-list.kt.spec",
                "spec/indent/format-argument-list-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `format ArgumentList with tabs`() {
        assertThat(
            IndentationRule().diffFileFormat(
                "spec/indent/format-argument-list.kt.spec",
                "spec/indent/format-argument-list-expected-tabs.kt.spec",
                INDENT_STYLE_TABS
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
        val code = """
            fun main() {
                return 0
              }
            """.trimIndent()
        val expectedCode = """
            fun main() {
            ${TAB}return 0
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code, INDENT_STYLE_TABS)).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected 'space' character(s) in indentation"),
                LintError(3, 1, "indent", "Unexpected 'space' character(s) in indentation"),
            )
        )
        assertThat(IndentationRule().format(code, INDENT_STYLE_TABS)).isEqualTo(expectedCode)
    }

    @Test
    fun testUnexpectedTabCharacter() {
        val code = """
            fun main() {
            ${TAB}return 0
            }
            """.trimIndent()
        val expectedCode = """
            fun main() {
                return 0
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected 'tab' character(s) in indentation"),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun testUnexpectedTabCharacterWithCustomIndentSize() {
        val code = """
            fun main() {
            ${TAB}${TAB}return 0
            ${TAB}}
            """.trimIndent()
        val expectedCode = """
            fun main() {
              return 0
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code, mapOf("indent_size" to "2"))).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected 'tab' character(s) in indentation"),
                LintError(3, 1, "indent", "Unexpected 'tab' character(s) in indentation"),
            )
        )
        assertThat(IndentationRule().format(code, mapOf("indent_size" to "2"))).isEqualTo(expectedCode)
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

    @Ignore  // Code fix needed
    @Test
    fun `lint multiline comment`() {
        val code = """
          /*
        *
             */
        """.trimIndent()
        assertThat(IndentationRule().lint(code))
            .isEqualTo(
                listOf(
                    LintError(line = 1, col = 1, ruleId = "indent", detail = "Unexpected indentation (2) (should be 0)"),
                )
            )
    }


    @Ignore // Code fix needed
    @Test
    fun `no new line before lambda arrow`() {
        assertThat(
            IndentationRule().lint(
                """
                fun bar() {
                    Pair("val1", "val2")
                        .let {
                            (first, second) ->
                                first + second
                        }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(line = 3, col = 34, ruleId = "indent", detail = "Unexpected indentation (16) (should be 12)"),
            )
        )
    }

    @Test
    fun `incorrect indentation after lambda arrow`() {
        assertThat(
            IndentationRule().lint(
                """
                fun bar() {
                    Pair("val1", "val2")
                        .let { (first, second) ->
                                first + second
                        }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected indentation (16) (should be 12)"),
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

    @Test
    fun `format new line before opening quotes multiline string as parameter`() {
        val code =
            """
            fun foo() {
              println($MULTILINE_STRING_QUOTE
                line1
                    line2
                    $MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val expectedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                    line1
                        line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        val expectedCodeTabs =
            """
            fun foo() {
            ${TAB}println(
            ${TAB}${TAB}$MULTILINE_STRING_QUOTE
            ${TAB}${TAB}line1
            ${TAB}${TAB}    line2
            ${TAB}${TAB}$MULTILINE_STRING_QUOTE.trimIndent()
            ${TAB})
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(2, 1, ruleId="indent", detail="Unexpected indentation (2) (should be 4)"),
                LintError(2, 11, ruleId="indent", detail="Missing newline after \"(\""),
                LintError(3, 1, ruleId="indent", detail="Unexpected indent of multiline string"),
                LintError(4, 1, ruleId="indent", detail="Unexpected indent of multiline string"),
                LintError(5, 24, ruleId="indent", detail="Missing newline before \")\""),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
        assertThat(IndentationRule().format(code, INDENT_STYLE_TABS)).isEqualTo(expectedCodeTabs)
    }

    @Test
    fun `format new line before closing quotes multiline string when not blank`() {
        val code =
            """
            fun foo() {
                println($MULTILINE_STRING_QUOTE
                    line1
                line2$MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val expectedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                        line1
                    line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(line = 2, col = 13, ruleId = "indent", detail = "Missing newline after \"(\""),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string"),
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string"),
                LintError(line = 4, col = 10, ruleId = "indent", detail = "Missing newline before \"\"\""),
                LintError(line = 4, col = 25, ruleId = "indent", detail = "Missing newline before \")\""),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `format empty multiline string from spaces to tabs`() {
        val code =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        val expectedCodeTabs =
            """
            fun foo() {
            ${TAB}println(
            ${TAB}${TAB}$MULTILINE_STRING_QUOTE
            ${TAB}${TAB}$MULTILINE_STRING_QUOTE.trimIndent()
            ${TAB})
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code, INDENT_STYLE_TABS)
        ).isEqualTo(
            listOf(
                LintError(line=2, col=1, ruleId="indent", detail="Unexpected 'space' character(s) in indentation"),
                LintError(line=3, col=1, ruleId="indent", detail="Unexpected 'space' character(s) in indentation"),
                LintError(line=4, col=1, ruleId="indent", detail="Unexpected 'space' character(s) in margin of multiline string"),
                LintError(line=5, col=1, ruleId="indent", detail="Unexpected 'space' character(s) in indentation"),
            )
        )
        assertThat(IndentationRule().format(code, INDENT_STYLE_TABS)).isEqualTo(expectedCodeTabs)
    }

    @Test
    fun `format multiline string containing quotation marks`() {
        val code =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                text ""

                     text
                     ""
                $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        val expectedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                    text ""

                         text
                         ""
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string"),
                LintError(line = 6, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string"),
                LintError(line = 7, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string"),
                LintError(line = 8, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string"),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `format multiline string containing a template string as the first non blank element on the line`() {
        // Escape '${true}' as '${"$"}{true}' to prevent evaluation before actually processing the multiline sting
        val code =
            """
            fun foo() {
            println($MULTILINE_STRING_QUOTE
            ${"$"}{true}

                ${"$"}{true}
            $MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val expectedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                    ${"$"}{true}

                        ${"$"}{true}
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected indentation (0) (should be 4)"),
                LintError(2, 9, "indent", "Missing newline after \"(\""),
                LintError(3, 1, "indent", "Unexpected indent of multiline string"),
                LintError(5, 1, "indent", "Unexpected indent of multiline string"),
                LintError(6, 1, "indent", "Unexpected indent of multiline string"),
                LintError(6, 16, "indent", "Missing newline before \")\"")
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `format variable with multiline string value`() {
        val code =
            """
            val foo1 = $MULTILINE_STRING_QUOTE
                    line1
                $MULTILINE_STRING_QUOTE.trimIndent()
            val foo2 =
                $MULTILINE_STRING_QUOTE
                    line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            val foo3 = // comment
                $MULTILINE_STRING_QUOTE
                    line3
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        /*
        // TODO: What is the proper way to indent a multi line string value?
        //     val foo =
        //        """
        //        line1
        //        line2
        //        """
        //  or
        //     val foo = """
        //        line1
        //        line2
        //        """
        //  or
        //     val foo = """
        //     line1
        //     line2
        //     """
        //  or
        //     val foo = """
        //        line1
        //        line2
        //     """
        // First option would be most in line with function parameter. Second option is also acceptable. Third option
        // (previous behavior) is not logical as there is no continuation kind of indent visible. Last option is not in
        // sync with function parameter.
        */
        val expectedCode =
            """
            val foo1 = $MULTILINE_STRING_QUOTE
                line1
                $MULTILINE_STRING_QUOTE.trimIndent()
            val foo2 =
                $MULTILINE_STRING_QUOTE
                line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            val foo3 = // comment
                $MULTILINE_STRING_QUOTE
                line3
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(2, 1, "indent", "Unexpected indent of multiline string"),
                LintError(6, 1, "indent", "Unexpected indent of multiline string"),
                LintError(10, 1, "indent", "Unexpected indent of multiline string"),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `format variable in class with multiline string value`() {
        val code =
            """
            class C {
                val CONFIG_COMPACT1 = $MULTILINE_STRING_QUOTE
                        {
                        }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                val CONFIG_COMPACT2 = // comment
                    $MULTILINE_STRING_QUOTE
                        {
                        }
                    $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val expectedCode =
            """
            class C {
                val CONFIG_COMPACT1 = $MULTILINE_STRING_QUOTE
                    {
                    }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                val CONFIG_COMPACT2 = // comment
                    $MULTILINE_STRING_QUOTE
                    {
                    }
                    $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(3, 1, "indent", "Unexpected indent of multiline string"),
                LintError(4, 1, "indent", "Unexpected indent of multiline string"),
                LintError(8, 1, "indent", "Unexpected indent of multiline string"),
                LintError(9, 1, "indent", "Unexpected indent of multiline string"),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `issue 575 - format multiline string with tabs only in indentation margin`() {
        val code =
            """
            val str =
                $MULTILINE_STRING_QUOTE
            ${TAB}line1
            ${TAB}${TAB}line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val expectedCode =
            """
            val str =
                $MULTILINE_STRING_QUOTE
                line1
                ${TAB}line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEqualTo(
            listOf(
                LintError(3, 1, "indent", "Unexpected 'tab' character(s) in margin of multiline string"),
                LintError(4, 1, "indent", "Unexpected 'tab' character(s) in margin of multiline string"),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `issue 575 - format multiline string with tabs after the margin is indented properly`() {
        val code =
            """
            val str =
                $MULTILINE_STRING_QUOTE
                ${TAB}Tab at the beginning of this line but after the indentation margin
                Tab${TAB}in the middle of this string
                Tab at the end of this line.$TAB
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEmpty()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `issue 575 - lint multiline string with mixed indentation characters, can not be autocorrected`() {
        val code =
            """
                val foo = $MULTILINE_STRING_QUOTE
                      line1
                {TAB} line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """
                .trimIndent()
                .replace("{TAB}", "\t")
                .replace("{SPACE}", " ")
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(
                    line = 1,
                    col = 11,
                    ruleId = "indent",
                    detail = "Indentation of multiline string should not contain both tab(s) and space(s)"
                ),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint if-condition with line break and multiline call expression is indented properly`() {
        assertThat(
            IndentationRule().lint(
                """
                // https://github.com/pinterest/ktlint/issues/871
                fun function(param1: Int, param2: Int, param3: Int?): Boolean {
                    return if (
                        listOf(
                            param1,
                            param2,
                            param3
                        ).none { it != null }
                    ) {
                        true
                    } else {
                        false
                    }
                }

                // https://github.com/pinterest/ktlint/issues/900
                enum class Letter(val value: String) {
                    A("a"),
                    B("b");
                }
                fun broken(key: String): Letter {
                    for (letter in Letter.values()) {
                        if (
                            letter.value
                                .equals(
                                    key,
                                    ignoreCase = true
                                )
                        ) {
                            return letter
                        }
                    }
                    return Letter.B
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint property delegate is indented properly`() {
        assertThat(
            IndentationRule().lint(
                """
                val i: Int
                    by lazy { 1 }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint delegation 1`() {
        assertThat(
            IndentationRule().lint(
                """
                interface Foo

                class Bar(a: Int, b: Int, c: Int) : Foo

                class Test1 : Foo by Bar(
                    a = 1,
                    b = 2,
                    c = 3
                )
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint delegation 2`() {
        assertThat(
            IndentationRule().lint(
                """
                interface Foo

                class Bar(a: Int, b: Int, c: Int) : Foo

                class Test2 : Foo
                by Bar(
                    a = 1,
                    b = 2,
                    c = 3
                )
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint delegation 3`() {
        assertThat(
            IndentationRule().lint(
                """
                interface Foo

                class Bar(a: Int, b: Int, c: Int) : Foo

                class Test3 :
                    Foo by Bar(
                        a = 1,
                        b = 2,
                        c = 3
                    )
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint delegation 4`() {
        assertThat(
            IndentationRule().lint(
                """
                interface Foo

                class Bar(a: Int, b: Int, c: Int) : Foo

                class Test4 :
                    Foo
                    by Bar(
                        a = 1,
                        b = 2,
                        c = 3
                    )
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint delegation 5`() {
        assertThat(
            IndentationRule().lint(
                """
                interface Foo

                class Bar(a: Int, b: Int, c: Int) : Foo

                class Test5 {
                    companion object : Foo by Bar(
                        a = 1,
                        b = 2,
                        c = 3
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint delegation 6`() {
        assertThat(
            IndentationRule().lint(
                """
                data class Shortcut(val id: String, val url: String)

                object Someclass : List<Shortcut> by listOf(
                    Shortcut(
                        id = "1",
                        url = "url"
                    ),
                    Shortcut(
                        id = "2",
                        url = "asd"
                    ),
                    Shortcut(
                        id = "3",
                        url = "TV"
                    )
                )
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint named argument`() {
        assertThat(
            IndentationRule().lint(
                """
                data class D(val a: Int, val b: Int, val c: Int)

                fun test() {
                    val d = D(
                        a = 1,
                        b =
                        2,
                        c = 3
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint default parameter`() {
        assertThat(
            IndentationRule().lint(
                """
                data class D(
                    val a: Int = 1,
                    val b: Int =
                        2,
                    val c: Int = 3
                )
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/959
    @Test
    fun `lint conditions with multi-line call expressions indented properly`() {
        assertThat(
            IndentationRule().lint(
                """
                fun test() {
                    val result = true &&
                        minOf(
                            1, 2
                        ) == 2
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/1003
    @Test
    fun `lint multiple interfaces`() {
        assertThat(
            IndentationRule().lint(
                """
                abstract class Parent(a: Int, b: Int)

                interface Parent2

                class Child(
                    a: Int,
                    b: Int
                ) : Parent(
                    a,
                    b
                ),
                    Parent2
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/918
    @Test
    fun `lint newline after type reference in functions`() {
        assertThat(
            IndentationRule().lint(
                """
                override fun actionProcessor():
                    ObservableTransformer<in SomeVeryVeryLongNameOverHereAction, out SomeVeryVeryLongNameOverHereResult> =
                    ObservableTransformer { actions ->
                        // ...
                    }

                fun generateGooooooooooooooooogle():
                    Gooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooogle {
                    return Gooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooogle()
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    private companion object {
        const val MULTILINE_STRING_QUOTE = "${'"'}${'"'}${'"'}"
        const val TAB = "${'\t'}"

        val INDENT_STYLE_TABS = mapOf("indent_style" to "tab")
    }
}
