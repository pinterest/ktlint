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
    fun `lint IndentTab with tabs`() {
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
    fun testUnexpectedTabCharacterForLinesAtCorrectIndentationLevel() {
        val ktScript = "" +
            "class Foo {\n" +
            "\tfun doBar() {\n" +
            "\t\t\tprintln(\"test\")\n" +
            "\t}\n" +
            "}\n"
        assertThat(IndentationRule().lint(ktScript)).isEqualTo(
            listOf(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected indentation (12) (should be 8)"),
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)")
            )
        )
        assertThat(IndentationRule().format(ktScript))
            .isEqualTo(
                "class Foo {\n" +
                    "    fun doBar() {\n" +
                    "        println(\"test\")\n" +
                    "    }\n" +
                    "}\n"
            )
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
        @Suppress("RemoveCurlyBracesFromTemplate") val expectedCodeTabs =
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
                LintError(2, 13, "indent", "Missing newline after \"(\""),
                LintError(5, 24, "indent", "Missing newline before \")\""),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
        assertThat(IndentationRule().format(code, INDENT_STYLE_TABS)).isEqualTo(expectedCodeTabs)
    }

    @Test
    fun `format multiline string assignment to variable with opening quotes on same line as declaration`() {
        val code =
            """
            fun foo() {
                val bar = $MULTILINE_STRING_QUOTE
                          line1
                              line2
                          $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val expectedCode =
            """
            fun foo() {
                val bar = $MULTILINE_STRING_QUOTE
                          line1
                              line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        assertThat(
            IndentationRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(5, 1, "indent", "Unexpected indent of multiline string closing quotes"),
            )
        )
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    fun `format multiline string containing quotation marks`() {
        val code =
            """
            fun foo() {
                println($MULTILINE_STRING_QUOTE
                    text ""

                         text
                         ""
                $MULTILINE_STRING_QUOTE.trimIndent())
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
                LintError(line = 2, col = 13, ruleId = "indent", detail = "Missing newline after \"(\""),
                LintError(line = 7, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string closing quotes"),
                LintError(line = 7, col = 20, ruleId = "indent", detail = "Missing newline before \")\""),
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
                LintError(line = 2, col = 13, ruleId = "indent", detail = "Missing newline after \"(\""),
                LintError(line = 6, col = 1, ruleId = "indent", detail = "Unexpected indent of multiline string closing quotes"),
                LintError(line = 6, col = 20, ruleId = "indent", detail = "Missing newline before \")\""),
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

                val j = 0
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint property delegate is indented properly 2`() {
        assertThat(
            IndentationRule().lint(
                """
                val i: Int
                    by lazy {
                        "".let {
                            println(it)
                        }
                        1
                    }

                val j = 0
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint property delegate is indented properly 3`() {
        assertThat(
            IndentationRule().lint(
                """
                val i: Int by lazy {
                    "".let {
                        println(it)
                    }
                    1
                }

                val j = 0
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint property delegate is indented properly 4`() {
        assertThat(
            IndentationRule().lint(
                """
                fun lazyList() = lazy { mutableListOf<String>() }

                class Test {
                    val list: List<String>
                        by lazyList()

                    val aVar = 0
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint property delegate is indented properly 5`() {
        assertThat(
            IndentationRule().lint(
                """
                fun lazyList(a: Int, b: Int) = lazy { mutableListOf<String>() }

                class Test {
                    val list: List<String>
                        by lazyList(
                            1,
                            2
                        )

                    val aVar = 0
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/1210
    @Test
    fun `lint delegated properties with a lambda argument`() {
        assertThat(
            IndentationRule().lint(
                """
                import kotlin.properties.Delegates

                class Test {
                    private var test
                        by Delegates.vetoable("") { _, old, new ->
                            true
                        }
                }
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
    fun `lint and format delegation 2`() {
        val code =
            """
            class Test2 : Foo
            by Bar(
                a = 1,
                b = 2,
                c = 3
            )
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
        assertThat(IndentationRule().lint(code)).isEmpty()
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

    // https://github.com/pinterest/ktlint/issues/764
    @Test
    fun `lint value argument list with lambda`() {
        assertThat(
            IndentationRule().lint(
                """
                fun test(i: Int, f: (Int) -> Unit) {
                    f(i)
                }

                fun main() {
                    test(1, f = {
                        println(it)
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint value argument list with two lambdas`() {
        assertThat(
            IndentationRule().lint(
                """
                fun test(f: () -> Unit, g: () -> Unit) {
                    f()
                    g()
                }

                fun main() {
                    test({
                        println(1)
                    }, {
                        println(2)
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint value argument list with anonymous function`() {
        assertThat(
            IndentationRule().lint(
                """
                fun test(i: Int, f: (Int) -> Unit) {
                    f(i)
                }

                fun main() {
                    test(1, fun(it: Int) {
                        println(it)
                    })
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint value argument list with lambda in super type entry`() {
        assertThat(
            IndentationRule().lint(
                """
                class A : B({
                    1
                }) {
                    val a = 1
                }

                open class B(f: () -> Int)
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/1202
    @Test
    fun `lint lambda argument and call chain`() {
        assertThat(
            IndentationRule().lint(
                """
                class Foo {
                    fun bar() {
                        val foo = bar.associateBy({ item -> item.toString() }, ::someFunction).toMap()
                    }
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    // https://github.com/pinterest/ktlint/issues/1165
    @Test
    fun `lint multiline expression with elvis operator in assignment`() {
        assertThat(
            IndentationRule().lint(
                """
                fun test() {
                    val a: String = ""

                    val someTest: Int?

                    someTest =
                        a
                            .toIntOrNull()
                            ?: 1
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `multiline string with mixed indentation characters, can not be autocorrected`() {
        val code =
            """
                val foo = $MULTILINE_STRING_QUOTE
                      line1
                ${TAB}line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """
                .trimIndent()
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
    fun `format kdoc`() {
        @Suppress("RemoveCurlyBracesFromTemplate")
        val code =
            """
            /**
             * some function1
             */
            fun someFunction1() {
                return Unit
            }

            class SomeClass {
                /**
                 * some function2
                 */
                fun someFunction2() {
                    return Unit
                }
            }
            """.trimIndent()
        @Suppress("RemoveCurlyBracesFromTemplate")
        val codeTabs =
            """
            /**
             * some function1
             */
            fun someFunction1() {
            ${TAB}return Unit
            }

            class SomeClass {
            ${TAB}/**
            ${TAB} * some function2
            ${TAB} */
            ${TAB}fun someFunction2() {
            ${TAB}${TAB}return Unit
            ${TAB}}
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEmpty()
        assertThat(IndentationRule().format(code)).isEqualTo(code)

        assertThat(IndentationRule().lint(codeTabs, INDENT_STYLE_TABS)).isEmpty()
        assertThat(IndentationRule().format(codeTabs, INDENT_STYLE_TABS)).isEqualTo(codeTabs)
    }

    @Test
    fun `Issue 1222 - format secondary constructor`() {
        val code =
            """
            class Issue1222 {
                constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
                        super(context, attrs, defStyleAttr, defStyleRes) {
                    init(attrs, defStyleAttr, defStyleRes)
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Issue1222 {
                constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
                    super(context, attrs, defStyleAttr, defStyleRes) {
                    init(attrs, defStyleAttr, defStyleRes)
                }
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code))
            .containsExactly(
                LintError(3, 1, "indent", "Unexpected indentation (12) (should be 8)"),
            )
        assertThat(IndentationRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Issue 1222 - format class constructor, parameter of super invocations are indented`() {
        val code =
            """
            class Issue1222 {
                constructor(string1: String, string2: String2) :
                    super(
                    string1, string2
                    ) {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Issue1222 {
                constructor(string1: String, string2: String2) :
                    super(
                        string1, string2
                    ) {
                    // do something
                }
            }
            """.trimIndent()
        assertThat(IndentationRule().lint(code))
            .containsExactly(
                LintError(4, 1, "indent", "Unexpected indentation (8) (should be 12)"),
            )
        assertThat(IndentationRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Formats function literal with comment before the parameter list`() {
        val code =
            """
            val foo1: (String) -> String = { // Some comment which should not be moved to the next line when formatting
                s: String
                ->
                // does something with string
            }

            val foo2: (String) -> String = {
                // Some comment which has to be indented with the parameter list
                s: String
                ->
                // does something with string
            }

            val foo3 = { // Some comment which should not be moved to the next line when formatting
                s: String,
                ->
                // does something with string
            }

            val foo4 = {
                // Some comment which has to be indented with the parameter list
                s: String,
                ->
                // does something with string
            }
            """.trimIndent()
        val expectedCode =
            """
            val foo1: (String) -> String = { // Some comment which should not be moved to the next line when formatting
                    s: String
                ->
                // does something with string
            }

            val foo2: (String) -> String = {
                    // Some comment which has to be indented with the parameter list
                    s: String
                ->
                // does something with string
            }

            val foo3 = { // Some comment which should not be moved to the next line when formatting
                    s: String,
                ->
                // does something with string
            }

            val foo4 = {
                    // Some comment which has to be indented with the parameter list
                    s: String,
                ->
                // does something with string
            }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
        assertThat(IndentationRule().lint(code)).containsExactly(
            LintError(2, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(8, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(9, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(15, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(21, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(22, 1, "indent", "Unexpected indentation (4) (should be 8)"),
        )
    }

    @Test
    fun `issue 1247 - Formats function literal with single value parameter`() {
        val code =
            """
            val foo1: (String) -> String = {
                s: String
                ->
                // does something with string
            }

            val foo2 = {
                // Trailing comma on last element is allowed and does not have effect
                s: String,
                ->
                // does something with string
            }
            """.trimIndent()
        val expectedCode =
            """
            val foo1: (String) -> String = {
                    s: String
                ->
                // does something with string
            }

            val foo2 = {
                    // Trailing comma on last element is allowed and does not have effect
                    s: String,
                ->
                // does something with string
            }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
        assertThat(IndentationRule().lint(code)).containsExactly(
            LintError(2, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(8, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(9, 1, "indent", "Unexpected indentation (4) (should be 8)"),
        )
    }

    @Test
    fun `issue 1247 - Formats function literal with multiple value parameters`() {
        val code =
            """
            val foo1: (String, String) -> String = {
                s1: String,
                s2: String
                ->
                // does something with strings
            }

            val foo2 = {
                s1: String,
                // Trailing comma on last element is allowed and does not have effect
                s2: String,
                ->
                // does something with strings
            }
            """.trimIndent()
        val expectedCode =
            """
            val foo1: (String, String) -> String = {
                    s1: String,
                    s2: String
                ->
                // does something with strings
            }

            val foo2 = {
                    s1: String,
                    // Trailing comma on last element is allowed and does not have effect
                    s2: String,
                ->
                // does something with strings
            }
            """.trimIndent()
        assertThat(IndentationRule().format(code)).isEqualTo(expectedCode)
        assertThat(IndentationRule().lint(code)).containsExactly(
            LintError(2, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(3, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(9, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(10, 1, "indent", "Unexpected indentation (4) (should be 8)"),
            LintError(11, 1, "indent", "Unexpected indentation (4) (should be 8)"),
        )
    }

    @Test
    fun `Issue 1210 - format supertype delegate`() {
        val code =
            """
            object ApplicationComponentFactory : ApplicationComponent.Factory
            by DaggerApplicationComponent.factory()
            """.trimIndent()
        assertThat(IndentationRule().lint(code)).isEmpty()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Issue 1210 - format of statements after supertype delegated entry 2`() {
        val code =
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

            // The next line ensures that the fix regarding the expectedIndex due to alignment of "by" keyword in
            // class above, is still in place. Without this fix, the expectedIndex would hold a negative value,
            // resulting in the formatting to crash on the next line.
            val bar = 1
            """.trimIndent()

        assertThat(IndentationRule().lint(code)).isEmpty()
        assertThat(IndentationRule().format(code)).isEqualTo(code)
    }

    private companion object {
        const val MULTILINE_STRING_QUOTE = "${'"'}${'"'}${'"'}"
        const val TAB = "${'\t'}"

        val INDENT_STYLE_TABS = mapOf("indent_style" to "tab")
    }
}
