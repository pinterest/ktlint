package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.EditorConfig.Companion.indentStyleProperty
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Test

@FeatureInAlphaState
internal class WrappingRuleTest {
    private val wrappingRuleAssertThat = WrappingRule().assertThat()

    @Test
    fun `Given a multiline string containing a string-template as parameter value but then wrap the value to a start and end on separate lines`() {
        // Interpret "$." in code samples below as "$". It is used here as otherwise the indentation in the code sample
        // is disapproved when running ktlint on the unit tests during the build process (not that the indent rule can
        // not be disabled for a block).
        val code =
            """
            fun foo() {
                println("$.{
                true
                }")
            }
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        val formattedCode =
            """
            fun foo() {
                println(
                    "$.{
                    true
                    }"
                )
            }
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 13, "Missing newline after \"(\""),
                LintViolation(4, 6, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline raw string literal then wrap and indent conditionally`() {
        val code =
            """
            fun foo() {
                println(${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE})
                println(${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent())
                println(${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimMargin())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                println(
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                )
                println(
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent()
                )
                println(
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimMargin()
                )
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 13, "Missing newline after \"(\""),
                LintViolation(3, 7, "Missing newline before \")\""),
                LintViolation(4, 13, "Missing newline after \"(\""),
                LintViolation(5, 20, "Missing newline before \")\""),
                LintViolation(6, 13, "Missing newline after \"(\""),
                LintViolation(7, 20, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some multiline raw string literal contain multiline string templates`() {
        val code =
            """
            fun foo1() {
                foo2(${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}$.{
            true
                }
                text
            _$.{
            true
                }${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent(), ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}text${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE})
            }
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        val formattedCode =
            """
            fun foo1() {
                foo2(
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}$.{
                    true
                    }
                text
            _$.{
                    true
                    }
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent(),
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}text${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                )
            }
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 10, "Missing newline after \"(\""),
                LintViolation(8, 6, "Missing newline before \"\"\""),
                LintViolation(8, 23, "Missing newline after \",\""),
                LintViolation(8, 33, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline raw string literal as function call parameter but not starting and ending on a separate line`() {
        val code =
            """
            fun foo() {
            println(${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                text

                    text
            ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent().toByteArray())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                println(
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                text

                    text
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent().toByteArray()
                )
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 9, "Missing newline after \"(\""),
                LintViolation(6, 30, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline raw string literal as non-first function call parameter`() {
        val code =
            """
            fun foo() {
                write(fs.getPath("/projects/.editorconfig"), ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                    root = true
                    [*]
                    end_of_line = lf
                ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent().toByteArray())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                write(
                    fs.getPath("/projects/.editorconfig"),
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}
                    root = true
                    [*]
                    end_of_line = lf
                    ${com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE}.trimIndent().toByteArray()
                )
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(2, 11, "Missing newline after \"(\""),
                LintViolation(2, 49, "Missing newline after \",\""),
                LintViolation(6, 34, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some parameter lists`() {
        val code =
            """
            class C (val a: Int, val b: Int, val e: (
            r: Int
            ) -> Unit, val c: Int, val d: Int) {
            fun f(a: Int, b: Int, e: (
            r: Int
            ) -> Unit, c: Int, d: Int) {}
            }
            """.trimIndent()
        val formattedCode =
            """
            class C (
                val a: Int, val b: Int,
                val e: (
                    r: Int
                ) -> Unit,
                val c: Int, val d: Int
            ) {
                fun f(
                    a: Int, b: Int,
                    e: (
                        r: Int
                    ) -> Unit,
                    c: Int, d: Int
                ) {}
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(1, 10, "Missing newline after \"(\""),
                LintViolation(1, 33, "Missing newline after \",\""),
                LintViolation(3, 11, "Missing newline after \",\""),
                LintViolation(3, 33, "Missing newline before \")\""),
                LintViolation(4, 7, "Missing newline after \"(\""),
                LintViolation(4, 22, "Missing newline after \",\""),
                LintViolation(6, 11, "Missing newline after \",\""),
                LintViolation(6, 25, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function call`() {
        val code =
            """
            fun main() {
                f(a, b, {
                // body
                }, c, d)

                fn(a,
                   b,
                   c)
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                f(a, b, {
                    // body
                }, c, d)

                fn(
                    a,
                    b,
                    c
                )
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .addAdditionalRules(IndentationRule())
            .hasLintViolations(
                LintViolation(6, 8, "Missing newline after \"(\""),
                LintViolation(8, 8, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test // "https://github.com/shyiko/ktlint/issues/180"
    fun testLintWhereClause() {
        assertThat(
            WrappingRule().lint(
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
            WrappingRule().lint(
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
    fun `test wrapping rule allows line comment`() {
        val code =
            """
            interface Foo1 {}
            interface Foo2 {}
            interface Foo3 {}
            class Bar :
                Foo1, // this comment should be legal
                Foo2,// this comment should be legal
                Foo3 {
            }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `test wrapping rule allows block comment`() {
        val code =
            """
            interface Foo1 {}
            interface Foo2 {}
            interface Foo3 {}
            class Bar :
                Foo1, /* this comment should be legal */
                Foo2,/* this comment should be legal */
                Foo3 {
            }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun testLintNewlineAfterEqAllowed() {
        assertThat(
            WrappingRule().lint(
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
    fun `lint indentation new line before return type`() {
        assertThat(
            WrappingRule().lint(
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
    fun `lint trailing comment in multiline parameter is allowed`() {
        assertThat(
            WrappingRule().lint(
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
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint safe-called wrapped trailing lambda is allowed`() {
        assertThat(
            WrappingRule().lint(
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
        assertThat(WrappingRule().format(code)).isEqualTo(code)
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
        assertThat(WrappingRule().lint(code)).isEmpty()
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
        assertThat(WrappingRule().format(code)).isEqualTo(code)
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
        assertThat(WrappingRule().lint(code)).isEmpty()
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
        assertThat(WrappingRule().format(code)).isEqualTo(code)
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

        assertThat(
            WrappingRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(2, 13, "wrapping", "Missing newline after \"(\""),
                LintError(5, 24, "wrapping", "Missing newline before \")\"")
            )
        )
        assertThat(WrappingRule().format(code)).isEqualTo(expectedCode)
    }

    @Test
    @Suppress("RemoveCurlyBracesFromTemplate")
    fun `format new line before opening quotes multiline string as parameter with tab spacing`() {
        val code =
            """
            fun foo() {
            ${TAB}println($MULTILINE_STRING_QUOTE
            ${TAB}${TAB}line1
            ${TAB}${TAB}    line2
            ${TAB}${TAB}$MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val expectedCode =
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
            WrappingRule().lint(code, INDENT_STYLE_TABS)
        ).isEqualTo(
            listOf(
                LintError(2, 10, "wrapping", "Missing newline after \"(\""),
                LintError(5, 18, "wrapping", "Missing newline before \")\"")
            )
        )
        assertThat(WrappingRule().format(code, INDENT_STYLE_TABS)).isEqualTo(expectedCode)
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
            WrappingRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(line = 2, col = 13, ruleId = "wrapping", detail = "Missing newline after \"(\""),
                LintError(line = 7, col = 24, ruleId = "wrapping", detail = "Missing newline before \")\"")
            )
        )
        assertThat(WrappingRule().format(code)).isEqualTo(expectedCode)
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
            WrappingRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(line = 2, col = 13, ruleId = "wrapping", detail = "Missing newline after \"(\""),
                LintError(line = 6, col = 24, ruleId = "wrapping", detail = "Missing newline before \")\"")
            )
        )
        assertThat(WrappingRule().format(code)).isEqualTo(expectedCode)
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
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint if-condition with line break and multiline call expression is indented properly`() {
        assertThat(
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
        assertThat(WrappingRule().format(code)).isEqualTo(code)
        assertThat(WrappingRule().lint(code)).isEmpty()
    }

    @Test
    fun `lint delegation 3`() {
        assertThat(
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
            WrappingRule().lint(
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
    fun `multi line string at start of line`() {
        val code =
            """
            fun foo() =
            $MULTILINE_STRING_QUOTE
            some text
            $MULTILINE_STRING_QUOTE
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a multi line string but closing quotes not a separate line then wrap them to a new line`() {
        val code =
            """
            fun foo() =
                $MULTILINE_STRING_QUOTE
                some text$MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            fun foo() =
                $MULTILINE_STRING_QUOTE
                some text
                $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        assertThat(wrappingAndIndentRule.lint(code)).containsExactly(
            LintError(3, 14, "wrapping", "Missing newline before \"\"\"")
        )
        assertThat(wrappingAndIndentRule.format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Issue 1127 - multiline string followed by trimIndent in parameter list`() {
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
        assertThat(
            WrappingRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(line = 2, col = 12, ruleId = "wrapping", detail = "Missing newline after \"(\""),
                LintError(line = 6, col = 24, ruleId = "wrapping", detail = "Missing newline before \")\"")
            )
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
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
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)

        assertThat(WrappingRule().lint(codeTabs, INDENT_STYLE_TABS)).isEmpty()
        assertThat(WrappingRule().format(codeTabs, INDENT_STYLE_TABS)).isEqualTo(codeTabs)
    }

    @Test
    fun `Issue 1210 - format supertype delegate`() {
        val code =
            """
            object ApplicationComponentFactory : ApplicationComponent.Factory
            by DaggerApplicationComponent.factory()
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
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

        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Issue 1330 - Function with lambda parameter having a default value is allowed on a single line`() {
        val code =
            """
            fun func(lambdaArg: Unit.() -> Unit = {}, secondArg: Int) {
                println()
            }
            fun func(lambdaArg: Unit.(a: String) -> Unit = { it -> it.toUpperCaseAsciiOnly() }, secondArg: Int) {
                println()
            }
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Function with multiple lambda parameters can be formatted differently`() {
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
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a class with one supertype with a multiline call entry then do not reformat`() {
        val code =
            """
            class FooBar : Foo({
            })
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a class for which all supertypes start on the same line but the last supertype has a multiline call entry then do not reformat`() {
        val code =
            """
            class FooBar : Foo1, Foo2({
            })
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a class with supertypes start on different lines then place each supertype on a separate line`() {
        val code =
            """
            class FooBar : Foo1, Foo2,
                Bar1, Bar2
            """.trimIndent()
        val formattedCode =
            """
            class FooBar :
                Foo1,
                Foo2,
                Bar1,
                Bar2
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(1, 15, "wrapping", "Missing newline after \":\""),
            LintError(1, 21, "wrapping", "Missing newline after \",\""),
            LintError(2, 10, "wrapping", "Missing newline after \",\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a class for which the supertypes start on a next line then do not reformat`() {
        val code =
            """
            class FooBar :
                Foo1, Foo2({
            })
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Given a class for which the supertypes start on a next line but they not all start on the same line then place each supertype on a separate line`() {
        val code =
            """
            class FooBar :
                Foo1, Foo2,
                Bar1, Bar2
            """.trimIndent()
        val formattedCode =
            """
            class FooBar :
                Foo1,
                Foo2,
                Bar1,
                Bar2
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(2, 10, "wrapping", "Missing newline after \",\""),
            LintError(3, 10, "wrapping", "Missing newline after \",\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a when condition with a multiline expression without block after the arrow then start that expression on the next line`() {
        val code =
            """
            val bar = when (foo) {
                1 -> true
                2 ->
                    false
                3 -> false ||
                    true
                4 -> false || foobar({
                }) // Special case which is allowed
                else -> {
                    true
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            val bar = when (foo) {
                1 -> true
                2 ->
                    false
                3 ->
                    false ||
                    true
                4 -> false || foobar({
                }) // Special case which is allowed
                else -> {
                    true
                }
            }
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(5, 8, "wrapping", "Missing newline after \"->\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given an multiline argument list which is incorrectly formatted then reformat `() {
        val code =
            """
            fun foo() =
                bar(a,
                    b,
                    c)
            """.trimIndent()
        val formattedCode =
            """
            fun foo() =
                bar(
                    a,
                    b,
                    c
                )
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(2, 9, "wrapping", "Missing newline after \"(\""),
            LintError(4, 9, "wrapping", "Missing newline before \")\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Given a function call and last parameter value is a function call then the clossing parenthesis may be on a single line`() {
        val code =
            """
            val foobar = foo(""
                + ""
                + bar("" // IDEA quirk (ignored)
                ))
            """.trimIndent()
        val formattedCode =
            """
            val foobar = foo(
                ""
                + ""
                + bar(
                    "" // IDEA quirk (ignored)
                )
            )
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(1, 18, "wrapping", "Missing newline after \"(\""),
            LintError(3, 11, "wrapping", "Missing newline after \"(\""),
            LintError(4, 5, "wrapping", "Missing newline before \")\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Multiline string starting at position 0`() {
        val code =
            """
            fun foo() {
                println($MULTILINE_STRING_QUOTE
                text

                    text
            _$MULTILINE_STRING_QUOTE.trimIndent())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                text

                    text
            _
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
            }
            """.trimIndent()
        assertThat(wrappingAndIndentRule.lint(code)).containsExactly(
            LintError(2, 13, "wrapping", "Missing newline after \"(\""),
            LintError(6, 2, "wrapping", "Missing newline before \"\"\""),
            LintError(6, 17, "wrapping", "Missing newline before \")\"")
        )
        assertThat(wrappingAndIndentRule.format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Issue 1375 - Do not wrap raw string literal when not followed by trimIndent or trimMargin`() {
        val code =
            """
            val someCodeBlock = $MULTILINE_STRING_QUOTE
              foo()$MULTILINE_STRING_QUOTE
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).isEmpty()
        assertThat(WrappingRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `Issue 1375 - Wrap raw string literal when followed by trimIndent`() {
        val code =
            """
            val someCodeBlock = $MULTILINE_STRING_QUOTE
              foo()$MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        val formattedCode =
            """
            val someCodeBlock = $MULTILINE_STRING_QUOTE
              foo()
            $MULTILINE_STRING_QUOTE.trimIndent()
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(2, 8, "wrapping", "Missing newline before \"\"\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Issue 1375 - Wrap raw string literal when followed by trimMargin`() {
        val code =
            """
            val someCodeBlock = $MULTILINE_STRING_QUOTE
              foo()$MULTILINE_STRING_QUOTE.trimMargin()
            """.trimIndent()
        val formattedCode =
            """
            val someCodeBlock = $MULTILINE_STRING_QUOTE
              foo()
            $MULTILINE_STRING_QUOTE.trimMargin()
            """.trimIndent()
        assertThat(WrappingRule().lint(code)).containsExactly(
            LintError(2, 8, "wrapping", "Missing newline before \"\"\"")
        )
        assertThat(WrappingRule().format(code)).isEqualTo(formattedCode)
    }

    @Test
    fun `Issue 1350 - Given a for-statement with a newline in the expression only then do not wrap`() {
        val code =
            """
            fun foo() {
                for (item in listOf(
                    "a",
                    "b"
                )) {
                    println(item)
                }
            }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    private companion object {
        const val MULTILINE_STRING_QUOTE = "${'"'}${'"'}${'"'}"
        const val TAB = "${'\t'}"

        val INDENT_STYLE_TABS = EditorConfigOverride.from(
            indentStyleProperty to PropertyType.IndentStyleValue.tab
        )

        val wrappingAndIndentRule = listOf(WrappingRule(), IndentationRule())
    }
}

// Replace the "$." placeholder with an actual "$" so that string "$.{expression}" is transformed to a String template
// "${expression}".
private fun String.replacePlaceholderWithStringTemplate() = replace("$.", "${'$'}")
