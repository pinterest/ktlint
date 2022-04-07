package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("RemoveCurlyBracesFromTemplate")
@FeatureInAlphaState
internal class IndentationRuleTest {
    val indentationRuleAssertThat = IndentationRule().assertThat()
    val wrappingAndIndentationRuleAssertThat = listOf(WrappingRule(), IndentationRule()).assertThat()

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
    fun `Given that the indent size is set to 2 spaces and the code is properly indented the do not return lint errors`() {
        val code =
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
            """.trimIndent()
        indentationRuleAssertThat(code)
            .withEditorConfigOverride(indentSizeProperty to 2)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (3) (should be 2)"),
                LintError(3, 1, "indent", "Unexpected indentation (4) (should be 2)")
            )
    }

    @Test
    fun `Given that the indent style is set to tabs and the code is properly indented then do not return lint errors`() {
        val code =
            """
            fun main() {
            val v = ""
            ${TAB}${TAB}println(v)
            }
            fun main() {
            ${TAB}val v = ""
            ${TAB}println(v)
            }
            class A {
            ${TAB}var x: String
            ${TAB}${TAB}get() = ""
            ${TAB}${TAB}set(v: String) { x = v }
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .withEditorConfigOverride(INDENT_STYLE_TAB)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (0) (should be 1)"),
                LintError(3, 1, "indent", "Unexpected indentation (2) (should be 1)")
            )
    }

    @Test
    fun `Given that the indent size property is not set`() {
        val code =
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .withEditorConfigOverride(indentSizeProperty to "unset")
            .hasNoLintErrors()
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
        // TODO: Split into simple unit tests not using diffFileFormat and distinct between indentation and wrapping
        assertThat(
            wrappingAndIndentRule.diffFileFormat(
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
        assertThat(wrappingAndIndentRule.diffFileLint("spec/indent/lint-supertype.kt.spec")).isEmpty()
    }

    @Test
    fun testFormatSuperType() {
        assertThat(
            wrappingAndIndentRule.diffFileFormat(
                "spec/indent/format-supertype.kt.spec",
                "spec/indent/format-supertype-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Nested
    inner class TestLintFirstLine {
        @ParameterizedTest(name = "As Kotlin script: {0}")
        @ValueSource(booleans = [true, false])
        fun `Given a file with indented code on the first line then report unexpected indentation on first line`(asKotlinScript: Boolean) {
            val code =
                """
                |${SPACE}${SPACE}// comment
                """.trimMargin()
            indentationRuleAssertThat(code)
                .asKotlinScript(asKotlinScript)
                .hasLintErrors(
                    LintError(1, 1, "indent", "Unexpected indentation (2) (should be 0)")
                )
        }

        @ParameterizedTest(name = "As Kotlin script: {0}")
        @ValueSource(booleans = [true, false])
        fun `Given a file with blanks only on the first line then do not report unexpected indentation for that first line`(asKotlinScript: Boolean) {
            val code =
                """
                |${SPACE}${SPACE}
                |${SPACE}${SPACE}// comment
                """.trimMargin()
            indentationRuleAssertThat(code)
                .asKotlinScript(asKotlinScript)
                .hasLintErrors(
                    // Note that no LintError is created for the first line as it does not contain any code
                    LintError(2, 1, "indent", "Unexpected indentation (2) (should be 0)")
                )
        }
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
        assertThat(wrappingAndIndentRule.diffFileLint("spec/indent/lint-when-expression.kt.spec")).isEmpty()
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
        // TODO: Split into simple unit tests not using diffFileFormat and distinct between indentation and wrapping
        assertThat(
            wrappingAndIndentRule.diffFileFormat(
                "spec/indent/format-multiline-string.kt.spec",
                "spec/indent/format-multiline-string-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatArrow() {
        assertThat(
            wrappingAndIndentRule.diffFileFormat(
                "spec/indent/format-arrow.kt.spec",
                "spec/indent/format-arrow-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatEq() {
        assertThat(
            wrappingAndIndentRule.diffFileFormat(
                "spec/indent/format-eq.kt.spec",
                "spec/indent/format-eq-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatParameterList() {
        // TODO: Parameter and argument list do have a dedicated wrapping rule. This functionality should therefore be
        //  removed from the generic rule.
        assertThat(
            wrappingAndIndentRule.diffFileFormat(
                "spec/indent/format-parameter-list.kt.spec",
                "spec/indent/format-parameter-list-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test // "https://github.com/shyiko/ktlint/issues/180"
    fun `Given a class declaration using the WHERE keyword`() {
        val code =
            """
            class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
                val adapter1: A1,
                val adapter2: A2
            ) : RecyclerView.Adapter<C>()
                where A1 : RecyclerView.Adapter<V1>, A1 : ComposableAdapter.ViewTypeProvider,
                      A2 : RecyclerView.Adapter<V2>, A2 : ComposableAdapter.ViewTypeProvider {
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test // "https://github.com/pinterest/ktlint/issues/433"
    fun `Given a parameter list in which parameters are prefixed with a comment block`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given code with unexpected SPACE characters in the indentation`() {
        val code =
            """
            fun main() {
                return 0
              }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
            ${TAB}return 0
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .withEditorConfigOverride(INDENT_STYLE_TAB)
            .hasLintErrors(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected space character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected space character(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given code with unexpected TAB characters in the indentation`() {
        val code =
            """
            fun main() {
            ${TAB}${TAB}return 0
            ${TAB}}
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                return 0
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected indentation (8) (should be 4)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected indentation (4) (should be 0)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given code indented with TABS at the correct level while it should be indented with SPACES`() {
        val code =
            """
            class Foo {
            ${TAB}fun doBar() {
            ${TAB}${TAB}println("test")
            ${TAB}}
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                fun doBar() {
                    println("test")
                }
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given code which is correctly indented with TABS while a custom indent size of 2 SPACES should be used`() {
        val code =
            """
            class Foo {
            ${TAB}fun main() {
            ${TAB}${TAB}return 0
            ${TAB}}
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
              fun main() {
                return 0
              }
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .withEditorConfigOverride(indentSizeProperty to 2)
            .hasLintErrors(
                LintError(line = 2, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 3, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)"),
                LintError(line = 4, col = 1, ruleId = "indent", detail = "Unexpected tab character(s)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function declaration with a new line after the equality sign then do no return any lint errors`() {
        val code =
            """
            private fun getImplementationVersion() =
                javaClass.`package`.implementationVersion
                    ?: javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")
                        ?.let { stream ->
                            Manifest(stream).mainAttributes.getValue("Implementation-Version")
                        }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a lambda with incorrect indentation after lambda arrow`() {
        val code =
            """
            fun bar() {
                Pair("val1", "val2")
                    .let { (first, second) ->
                            first + second
                    }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun bar() {
                Pair("val1", "val2")
                    .let { (first, second) ->
                        first + second
                    }
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(4, 1, "indent", "Unexpected indentation (16) (should be 12)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function declaration with the return type incorrectly indented at new line`() {
        val code =
            """
            abstract fun doPerformSomeOperation(param: ALongParameter):
            SomeLongInterface<ALongParameter.InnerClass, SomeOtherClass>
            """.trimIndent()
        val formattedCode =
            """
            abstract fun doPerformSomeOperation(param: ALongParameter):
                SomeLongInterface<ALongParameter.InnerClass, SomeOtherClass>
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (0) (should be 4)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a variable declaration with type incorrectly indented on a new line`() {
        val code =
            """
            val s:
                    String = ""

            fun process(
                fileName:
                String
            ): List<Output>
            """.trimIndent()
        val formattedCode =
            """
            val s:
                String = ""

            fun process(
                fileName:
                    String
            ): List<Output>
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (8) (should be 4)"),
                LintError(6, 1, "indent", "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some code with an EOL comment in a multiline parameter then do not return lint errors`() {
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a safe-called lambda then do not return lint errors`() {
        val code =
            """
            val foo = bar
                ?.filter { number ->
                    number == 0
                }?.map { evenNumber ->
                    evenNumber * evenNumber
                }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a statement (not wrapped in a block) after an if then do no return lint errors`() {
        val code =
            """
            fun test() {
                if (true)
                    (1).toString()
                else
                    2.toString()
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 796 - Given an if-condition with multiline call expression which is indented properly then do no return lint errors`() {
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a multiline string assignment to variable with opening quotes on same line as declaration`() {
        val code =
            """
            fun foo() {
                val bar = $MULTILINE_STRING_QUOTE
                          line1
                              line2
                          $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                val bar = $MULTILINE_STRING_QUOTE
                          line1
                              line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(5, 1, "indent", "Unexpected indent of multiline string closing quotes")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline string containing quotation marks for which the closing quotes are not correctly indented`() {
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
        val formattedCode =
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(8, 1, "indent", "Unexpected indent of multiline string closing quotes")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `format multiline string containing a template string as the first non blank element on the line`() {
        // Escape '${true}' as '${"$"}{true}' to prevent evaluation before actually processing the multiline sting
        val code =
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
        val formattedCode =
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(7, 1, "indent", "Unexpected indent of multiline string closing quotes")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 575 - Given a multiline string which is properly indented but does contain tabs after the margin then do not return lint errors`() {
        val code =
            """
            val str =
                $MULTILINE_STRING_QUOTE
                ${TAB}Tab at the beginning of this line but after the indentation margin
                Tab${TAB}in the middle of this string
                Tab at the end of this line.$TAB
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given an if-condition with line break and multiline call expression which is indented properly then do not return lint errors`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Nested
    inner class PropertyDelegate {
        @Test
        fun `Property delegate is indented properly 1`() {
            val code =
                """
                val i: Int
                    by lazy { 1 }

                val j = 0
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Property delegate is indented properly 2`() {
            val code =
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
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Property delegate is indented properly 3`() {
            val code =
                """
                val i: Int by lazy {
                    "".let {
                        println(it)
                    }
                    1
                }

                val j = 0
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Property delegate is indented properly 4`() {
            val code =
                """
                fun lazyList() = lazy { mutableListOf<String>() }

                class Test {
                    val list: List<String>
                        by lazyList()

                    val aVar = 0
                }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Property delegate is indented properly 5`() {
            val code =
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
            indentationRuleAssertThat(code).hasNoLintErrors()
        }
    }

    @Test
    fun `Issue 1210 - lint delegated properties with a lambda argument`() {
        val code =
            """
            import kotlin.properties.Delegates

            class Test {
                private var test
                    by Delegates.vetoable("") { _, old, new ->
                        true
                    }
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Nested
    inner class Delegation {
        @Test
        fun `Delegation 1`() {
            val code =
                """
                interface Foo

                class Bar(a: Int, b: Int, c: Int) : Foo

                class Test1 : Foo by Bar(
                    a = 1,
                    b = 2,
                    c = 3
                )
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Delegation 2`() {
            val code =
                """
                class Test2 : Foo
                by Bar(
                    a = 1,
                    b = 2,
                    c = 3
                )
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Delegation 3`() {
            val code =
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
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Delegation 4`() {
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
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Delegation 5`() {
            val code =
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
            indentationRuleAssertThat(code).hasNoLintErrors()
        }

        @Test
        fun `Delegation 6`() {
            val code =
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
            indentationRuleAssertThat(code).hasNoLintErrors()
        }
    }

    @Test
    fun `Given a named argument`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a default parameter`() {
        val code =
            """
            data class D(
                val a: Int = 1,
                val b: Int =
                    2,
                val c: Int = 3
            )
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 959 - Given conditions with multi-line call expressions indented properly`() {
        val code =
            """
            fun test() {
                val result = true &&
                    minOf(
                        1, 2
                    ) == 2
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 1003 - Given multiple interfaces`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 918 - Given newline after type reference in functions`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 764 - Given value argument list with lambda`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given value argument list with two lambdas`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a value argument list with anonymous function`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a value argument list with lambda in super type entry`() {
        val code =
            """
            class A : B({
                1
            }) {
                val a = 1
            }

            open class B(f: () -> Int)
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 1202 - lint lambda argument and call chain`() {
        val code =
            """
            class Foo {
                fun bar() {
                    val foo = bar.associateBy({ item -> item.toString() }, ::someFunction).toMap()
                }
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 1165 - lint multiline expression with elvis operator in assignment`() {
        val code =
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a multiline string with mixed indentation characters, can not be autocorrected`() {
        val code =
            """
            val foo = $MULTILINE_STRING_QUOTE
                  line1
            ${TAB}line2
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintErrorsAfterFormatting(
                LintError(1, 11, "indent", "Indentation of multiline string should not contain both tab(s) and space(s)")
            )
    }

    @Test
    fun `Give a multiline string at start of line`() {
        val code =
            """
            fun foo() =
            $MULTILINE_STRING_QUOTE
            some text
            $MULTILINE_STRING_QUOTE
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 1127 - Given a multiline string followed by trimIndent in parameter list`() {
        val code =
            """
            interface UserRepository : JpaRepository<User, UUID> {
                @Query($MULTILINE_STRING_QUOTE
                    select u from User u
                    inner join Organization o on u.organization = o
                    where o = :organization
                $MULTILINE_STRING_QUOTE.trimIndent())
                fun findByOrganization(organization: Organization, pageable: Pageable): Page<User>
            }
            """.trimIndent()
        val formattedCode =
            """
            interface UserRepository : JpaRepository<User, UUID> {
                @Query(
                    $MULTILINE_STRING_QUOTE
                    select u from User u
                    inner join Organization o on u.organization = o
                    where o = :organization
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
                fun findByOrganization(organization: Organization, pageable: Pageable): Page<User>
            }
            """.trimIndent()
        wrappingAndIndentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(2, 12, "wrapping", "Missing newline after \"(\""),
                LintError(6, 1, "indent", "Unexpected indent of multiline string closing quotes"),
                LintError(6, 20, "wrapping", "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some kdoc and SPACE indent style`() {
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given some kdoc and TAB indent style`() {
        val code =
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
        indentationRuleAssertThat(code)
            .withEditorConfigOverride(INDENT_STYLE_TAB)
            .hasNoLintErrors()
    }

    @Test
    fun `Issue 1222 - Given a class with a secondary constructor`() {
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(3, 1, "indent", "Unexpected indentation (12) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1222 - Given a class constructor, parameter of super invocations are indented`() {
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(4, 1, "indent", "Unexpected indentation (8) (should be 12)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function literal with comment before the parameter list`() {
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
        val formattedCode =
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(8, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(9, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(15, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(21, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(22, 1, "indent", "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1247 - Given a function literal with single value parameter`() {
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
        val formattedCode =
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(8, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(9, 1, "indent", "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1247 - Formats function literal with multiple value parameters`() {
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
        val formattedCode =
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
        indentationRuleAssertThat(code)
            .hasLintErrors(
                LintError(2, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(3, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(9, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(10, 1, "indent", "Unexpected indentation (4) (should be 8)"),
                LintError(11, 1, "indent", "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1210 - Given a supertype delegate`() {
        val code =
            """
            object ApplicationComponentFactory : ApplicationComponent.Factory
            by DaggerApplicationComponent.factory()
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 1210 - Given some statements after supertype delegated entry 2`() {
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
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Issue 1330 - Given a function with a lambda parameter having a default value is allowed on a single line`() {
        val code =
            """
            fun func(lambdaArg: Unit.() -> Unit = {}, secondArg: Int) {
                println()
            }
            fun func(lambdaArg: Unit.(a: String) -> Unit = { it -> it.toUpperCaseAsciiOnly() }, secondArg: Int) {
                println()
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a function with multiple lambda parameters can be formatted differently`() {
        val code =
            """
            // https://github.com/pinterest/ktlint/issues/764#issuecomment-646822853
            val foo1 = println({
                bar()
            }, {
                bar()
            })
            // Other formats which should be allowed as well
            val foo2 = println(
                {
                    bar()
                },
                { bar() }
            )
            val foo3 = println(
                // Some comment
                {
                    bar()
                },
                // Some comment
                { bar() }
            )
            val foo4 = println(
                /* Some comment */
                {
                    bar()
                },
                /* Some comment */
                { bar() }
            )
            val foo5 = println(
                { bar() },
                { bar() }
            )
            val foo6 = println(
                // Some comment
                { bar() },
                // Some comment
                { bar() }
            )
            val foo7 = println(
                /* Some comment */
                { bar() },
                /* Some comment */
                { bar() }
            )
            val foo8 = println(
                { bar() }, { bar() }
            )
            val foo9 = println({ bar() }, { bar()})
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Binary expression`() {
        val code =
            """
            val x = "" +
                "" +
                f2(
                    "" // IDEA quirk (ignored)
                )
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintErrors()
    }

    private companion object {
        const val MULTILINE_STRING_QUOTE = "${'"'}${'"'}${'"'}"
        const val TAB = "${'\t'}"
        const val SPACE = "${' '}"

        val INDENT_STYLE_TAB = indentStyleProperty to PropertyType.IndentStyleValue.tab
        val INDENT_STYLE_TABS = EditorConfigOverride.from(
            indentStyleProperty to PropertyType.IndentStyleValue.tab
        )
        val wrappingAndIndentRule = listOf(WrappingRule(), IndentationRule())
    }
}
