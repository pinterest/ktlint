package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.ec4j.core.model.PropertyType.IndentStyleValue.tab
import org.junit.jupiter.api.Test

internal class WrappingRuleTest {
    private val wrappingRuleAssertThat = assertThatRule { WrappingRule() }

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
            .addAdditionalRuleProvider { IndentationRule() }
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
            .addAdditionalRuleProvider { IndentationRule() }
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
            .addAdditionalRuleProvider { IndentationRule() }
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
            .addAdditionalRuleProvider { IndentationRule() }
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
            .addAdditionalRuleProvider { IndentationRule() }
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
            .addAdditionalRuleProvider { IndentationRule() }
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
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(6, 8, "Missing newline after \"(\""),
                LintViolation(8, 8, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test // "https://github.com/shyiko/ktlint/issues/180"
    fun testLintWhereClause() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test // "https://github.com/pinterest/ktlint/issues/433"
    fun testLintParameterListWithComments() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        // Previously the IndentationRule would force the line break after the `=`. Verify that it is
        // still allowed.
        val code =
            """
            private fun getImplementationVersion() =
                javaClass.`package`.implementationVersion
                    ?: javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")
                        ?.let { stream ->
                            Manifest(stream).mainAttributes.getValue("Implementation-Version")
                        }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint indentation new line before return type`() {
        val code =
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint trailing comment in multiline parameter is allowed`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint safe-called wrapped trailing lambda is allowed`() {
        val code =
            """
            val foo = bar
                ?.filter { number ->
                    number == 0
                }?.map { evenNumber ->
                    evenNumber * evenNumber
                }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        val formattedCode =
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Missing newline after \"(\""),
                LintViolation(5, 24, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        val formattedCode =
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
        wrappingRuleAssertThat(code)
            .withEditorConfigOverride(DefaultEditorConfigProperties.indentStyleProperty to tab)
            .hasLintViolations(
                LintViolation(2, 10, "Missing newline after \"(\""),
                LintViolation(5, 18, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Missing newline after \"(\""),
                LintViolation(7, 24, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 13, "Missing newline after \"(\""),
                LintViolation(6, 24, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint if-condition with line break and multiline call expression is indented properly`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint property delegate is indented properly`() {
        val code =
            """
            val i: Int
                by lazy { 1 }

            val j = 0
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint property delegate is indented properly 2`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint property delegate is indented properly 3`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint property delegate is indented properly 4`() {
        val code =
            """
            fun lazyList() = lazy { mutableListOf<String>() }

            class Test {
                val list: List<String>
                    by lazyList()

                val aVar = 0
            }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint property delegate is indented properly 5`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/1210
    @Test
    fun `lint delegated properties with a lambda argument`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint delegation 1`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint delegation 3`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint delegation 4`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint delegation 5`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint delegation 6`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint named argument`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint default parameter`() {
        val code =
            """
            data class D(
                val a: Int = 1,
                val b: Int =
                    2,
                val c: Int = 3
            )
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/959
    @Test
    fun `lint conditions with multi-line call expressions indented properly`() {
        val code =
            """
            fun test() {
                val result = true &&
                    minOf(
                        1, 2
                    ) == 2
            }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/1003
    @Test
    fun `lint multiple interfaces`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/918
    @Test
    fun `lint newline after type reference in functions`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/764
    @Test
    fun `lint value argument list with lambda`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint value argument list with two lambdas`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint value argument list with anonymous function`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `lint value argument list with lambda in super type entry`() {
        val code =
            """
            class A : B({
                1
            }) {
                val a = 1
            }

            open class B(f: () -> Int)
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/1202
    @Test
    fun `lint lambda argument and call chain`() {
        val code =
            """
            class Foo {
                fun bar() {
                    val foo = bar.associateBy({ item -> item.toString() }, ::someFunction).toMap()
                }
            }
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    // https://github.com/pinterest/ktlint/issues/1165
    @Test
    fun `lint multiline expression with elvis operator in assignment`() {
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolation(3, 14, "Missing newline before \"\"\"")
            .isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 12, "Missing newline after \"(\""),
                LintViolation(6, 24, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code).hasNoLintViolations()

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
        wrappingRuleAssertThat(code)
            .withEditorConfigOverride(DefaultEditorConfigProperties.indentStyleProperty to tab)
            .hasNoLintViolations()
    }

    @Test
    fun `Issue 1210 - format supertype delegate`() {
        val code =
            """
            object ApplicationComponentFactory : ApplicationComponent.Factory
            by DaggerApplicationComponent.factory()
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class with one supertype with a multiline call entry then do not reformat`() {
        val code =
            """
            class FooBar : Foo({
            })
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class for which all supertypes start on the same line but the last supertype has a multiline call entry then do not reformat`() {
        val code =
            """
            class FooBar : Foo1, Foo2({
            })
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 15, "Missing newline after \":\""),
                LintViolation(1, 21, "Missing newline after \",\""),
                LintViolation(2, 10, "Missing newline after \",\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class for which the supertypes start on a next line then do not reformat`() {
        val code =
            """
            class FooBar :
                Foo1, Foo2({
            })
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 10, "Missing newline after \",\""),
                LintViolation(3, 10, "Missing newline after \",\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolation(5, 8, "Missing newline after \"->\"")
            .isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 9, "Missing newline after \"(\""),
                LintViolation(4, 9, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 18, "Missing newline after \"(\""),
                LintViolation(3, 11, "Missing newline after \"(\""),
                LintViolation(4, 5, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 13, "Missing newline after \"(\""),
                LintViolation(6, 2, "Missing newline before \"\"\""),
                LintViolation(6, 17, "Missing newline before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1375 - Do not wrap raw string literal when not followed by trimIndent or trimMargin`() {
        val code =
            """
            val someCodeBlock = $MULTILINE_STRING_QUOTE
              foo()$MULTILINE_STRING_QUOTE
            """.trimIndent()
        wrappingRuleAssertThat(code).hasNoLintViolations()
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
        wrappingRuleAssertThat(code)
            .hasLintViolation(2, 8, "Missing newline before \"\"\"")
            .isFormattedAs(formattedCode)
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
        wrappingRuleAssertThat(code)
            .hasLintViolation(2, 8, "Missing newline before \"\"\"")
            .isFormattedAs(formattedCode)
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
    }
}

// Replace the "$." placeholder with an actual "$" so that string "$.{expression}" is transformed to a String template
// "${expression}".
private fun String.replacePlaceholderWithStringTemplate() = replace("$.", "${'$'}")
