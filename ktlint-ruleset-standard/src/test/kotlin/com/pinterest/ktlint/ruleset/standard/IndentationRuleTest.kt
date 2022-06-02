package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import com.pinterest.ktlint.test.SPACE
import com.pinterest.ktlint.test.TAB
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("RemoveCurlyBracesFromTemplate")
@FeatureInAlphaState
internal class IndentationRuleTest {
    private val indentationRuleAssertThat = IndentationRule().assertThat()

    @Nested
    inner class MultilineValueDeclaration {
        val code =
            """
            class Foo {
            val foo1 =
            "foo"
            val foo2 =
            { bar: Int -> 2 * bar }
            }
            """.trimIndent()

        @Test
        fun `Given a multiline value declaration`() {
            val formattedCode =
                """
                class Foo {
                    val foo1 =
                        "foo"
                    val foo2 =
                        { bar: Int -> 2 * bar }
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 8)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a multiline value declaration (tab indentation)`() {
            val formattedCode =
                """
                class Foo {
                ${TAB}val foo1 =
                ${TAB}${TAB}"foo"
                ${TAB}val foo2 =
                ${TAB}${TAB}{ bar: Int -> 2 * bar }
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 2)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class AnnotatedFunctionDeclaration {
        val code =
            """
            class Foo {
            @Deprecated("Foo")
            fun foo() = "foo"
            }

            @Deprecated("Foo")
            fun foo() = "foo"
            """.trimIndent()

        @Test
        fun `Given an annotated function declaration`() {
            val formattedCode =
                """
                class Foo {
                    @Deprecated("Foo")
                    fun foo() = "foo"
                }

                @Deprecated("Foo")
                fun foo() = "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an annotated function declaration (tab indentation)`() {
            val formattedCode =
                """
                class Foo {
                ${TAB}@Deprecated("Foo")
                ${TAB}fun foo() = "foo"
                }

                @Deprecated("Foo")
                fun foo() = "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class FunctionDeclarationWithParametersWrappedToSeparateLines {
        val code =
            """
            class Foo {
            fun foo(
            foo1: Int,
            foo2: Int
            ) = foo1 + foo2
            }
            """.trimIndent()

        @Test
        fun `Given a function declaration with parameters wrapped to separate lines`() {
            val formattedCode =
                """
                class Foo {
                    fun foo(
                        foo1: Int,
                        foo2: Int
                    ) = foo1 + foo2
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a function declaration with parameters wrapped to separate lines (tab indentation)`() {
            val formattedCode =
                """
                class Foo {
                ${TAB}fun foo(
                ${TAB}${TAB}foo1: Int,
                ${TAB}${TAB}foo2: Int
                ${TAB}) = foo1 + foo2
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class WhenStatement {
        val code =
            """
            fun foo(bar: Any) =
            when (bar) {
            is Number -> 0
            else -> 1
            }
            """.trimIndent()

        @Test
        fun `Given a when-statement`() {
            val formattedCode =
                """
                fun foo(bar: Any) =
                    when (bar) {
                        is Number -> 0
                        else -> 1
                    }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a when-statement (tab indentation)`() {
            val formattedCode =
                """
                fun foo(bar: Any) =
                ${TAB}when (bar) {
                ${TAB}${TAB}is Number -> 0
                ${TAB}${TAB}else -> 1
                ${TAB}}
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class IfStatement {
        val code =
            """
            fun foo(i1: Int, i2: Int) =
            if (i1 > 0 &&
            i2 < 0
            ) {
            1
            } else {
            2
            }
            """.trimIndent()

        @Test
        fun `Given an if-statement`() {
            val formattedCode =
                """
                fun foo(i1: Int, i2: Int) =
                    if (i1 > 0 &&
                        i2 < 0
                    ) {
                        1
                    } else {
                        2
                    }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an if-statement (tab indentation)`() {
            val formattedCode =
                """
                fun foo(i1: Int, i2: Int) =
                ${TAB}if (i1 > 0 &&
                ${TAB}${TAB}i2 < 0
                ${TAB}) {
                ${TAB}${TAB}1
                ${TAB}} else {
                ${TAB}${TAB}2
                ${TAB}}
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class EndOfLineCommentStartingAtPosition0 {
        val code =
            """
            fun foo1() =
            // To be implemented
            "foo"
            """.trimIndent()

        @Test
        fun `Given an EOL comment starting at position 0 is not fixed`() {
            val formattedCode =
                """
                fun foo1() =
                // To be implemented
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolation(3, 1, "Unexpected indentation (0) (should be 4)")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an EOL comment starting at position 0 is not fixed (tab indentation)`() {
            val formattedCode =
                """
                fun foo1() =
                // To be implemented
                ${TAB}"foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolation(3, 1, "Unexpected indentation (0) (should be 1)")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class FunctionCallWithParametersWrappedToSeparateLines {
        val code =
            """
            fun bar() =
            foo(
            1,
            2
            )
            """.trimIndent()

        @Test
        fun `Given a function call with parameters wrapped to separate lines`() {
            val formattedCode =
                """
                fun bar() =
                    foo(
                        1,
                        2
                    )
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a function call with parameters wrapped to separate lines (tab indentation)`() {
            val formattedCode =
                """
                fun bar() =
                ${TAB}foo(
                ${TAB}${TAB}1,
                ${TAB}${TAB}2
                ${TAB})
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class TryCatchFinallyStatement {
        val code =
            """
            fun foo() =
            try {
            "foo1"
            } catch (e: Exception) {
            "foo2"
            } finally {
            Unit // do something
            }
            """.trimIndent()

        @Test
        fun `Given an try-catch-finally-statement`() {
            val formattedCode =
                """
                fun foo() =
                    try {
                        "foo1"
                    } catch (e: Exception) {
                        "foo2"
                    } finally {
                        Unit // do something
                    }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an try-catch-finally-statement (tab indentation)`() {
            val formattedCode =
                """
                fun foo() =
                ${TAB}try {
                ${TAB}${TAB}"foo1"
                ${TAB}} catch (e: Exception) {
                ${TAB}${TAB}"foo2"
                ${TAB}} finally {
                ${TAB}${TAB}Unit // do something
                ${TAB}}
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class ChainedMethodCalls {
        val code: String =
            """
            fun foo1(bar: String) =
            bar.uppercase(Locale.getDefault())
            .trim()
            .length.also {
            println("done")
            }
            """.trimIndent()

        @Test
        fun `Given some chained method calls`() {
            val formattedCode =
                """
                fun foo1(bar: String) =
                    bar.uppercase(Locale.getDefault())
                        .trim()
                        .length.also {
                            println("done")
                        }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some chained method calls (tab indentation)`() {
            val formattedCode =
                """
                fun foo1(bar: String) =
                ${TAB}bar.uppercase(Locale.getDefault())
                ${TAB}${TAB}.trim()
                ${TAB}${TAB}.length.also {
                ${TAB}${TAB}${TAB}println("done")
                ${TAB}${TAB}}
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 3)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 2)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class AnnotatedFunctionParameter {
        val code =
            """
            fun foo(
            @Named("foo1") bar1: Int,
            bar2: String
            ) = "foobar"
            """.trimIndent()

        @Test
        fun `Given an annotated function parameter`() {
            val formattedCode =
                """
                fun foo(
                    @Named("foo1") bar1: Int,
                    bar2: String
                ) = "foobar"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an annotated function parameter (tab indentation)`() {
            val formattedCode =
                """
                fun foo(
                ${TAB}@Named("foo1") bar1: Int,
                ${TAB}bar2: String
                ) = "foobar"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class AnnotatedValueDeclaration {
        val code =
            """
            class Foo {
            @Deprecated("Foo") val foo1 = "foo"

            @Deprecated("Foo")
            val foo2 = "foo"
            }

            @Deprecated("Foo")
            val foo3 = "foo"
            """.trimIndent()

        @Test
        fun `Given an annotated value declaration`() {
            val formattedCode =
                """
                class Foo {
                    @Deprecated("Foo") val foo1 = "foo"

                    @Deprecated("Foo")
                    val foo2 = "foo"
                }

                @Deprecated("Foo")
                val foo3 = "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an annotated value declaration (tab indentation)`() {
            val formattedCode =
                """
                class Foo {
                ${TAB}@Deprecated("Foo") val foo1 = "foo"

                ${TAB}@Deprecated("Foo")
                ${TAB}val foo2 = "foo"
                }

                @Deprecated("Foo")
                val foo3 = "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class FunctionDeclarationNestedElvisOperator {
        val code =
            """
            fun foo1(bar: String?) =
            bar?.map { it } ?: 0
            ?: -1
            fun foo2(bar: String?) =
            bar?.map { it }
            ?: 0
            ?: -1
            """.trimIndent()

        @Test
        fun `Given some function declaration having nested elvis operators`() {
            val formattedCode =
                """
                fun foo1(bar: String?) =
                    bar?.map { it } ?: 0
                        ?: -1
                fun foo2(bar: String?) =
                    bar?.map { it }
                        ?: 0
                        ?: -1
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 8)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some function declaration having nested elvis operators (tab indentation)`() {
            val formattedCode =
                """
                fun foo1(bar: String?) =
                ${TAB}bar?.map { it } ?: 0
                ${TAB}${TAB}?: -1
                fun foo2(bar: String?) =
                ${TAB}bar?.map { it }
                ${TAB}${TAB}?: 0
                ${TAB}${TAB}?: -1
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 2)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class ClassDeclarationWithSupertypeUsingGenerics {
        val code =
            """
            open class Foo<K, V>
            class Bar
            class FooBar :
            Foo<
            String,
            Int
            >,
            Bar() {
            }
            """.trimIndent()

        @Test
        fun `Given a class declaration implementing a super type with generics`() {
            val formattedCode =
                """
                open class Foo<K, V>
                class Bar
                class FooBar :
                    Foo<
                        String,
                        Int
                        >,
                    Bar() {
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a class declaration implementing a super type with generics (tab indentation)`() {
            val formattedCode =
                """
                open class Foo<K, V>
                class Bar
                class FooBar :
                ${TAB}Foo<
                ${TAB}${TAB}String,
                ${TAB}${TAB}Int
                ${TAB}${TAB}>,
                ${TAB}Bar() {
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 2)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class ClassDeclarationMultipleSuperTypes {
        val code =
            """
            abstract class Foo :
            Comparable<Any>,
            Appendable {
            fun foo() = "foo"
            }
            """.trimIndent()

        @Test
        fun `Given a class declaration implementing multiple super types`() {
            val formattedCode =
                """
                abstract class Foo :
                    Comparable<Any>,
                    Appendable {
                    fun foo() = "foo"
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a class declaration implementing multiple super types (tab indentation)`() {
            val formattedCode =
                """
                abstract class Foo :
                ${TAB}Comparable<Any>,
                ${TAB}Appendable {
                ${TAB}fun foo() = "foo"
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class AnnotatedClassDefinition {
        val code =
            """
            @Deprecated("Foo")
            class Foo
            """.trimIndent()

        @Test
        fun `Given an annotated class declaration`() {
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given an annotated class declaration (tab indentation)`() {
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class EnumerationClass {
        val code =
            """
            enum class FooBar {
            FOO,
            BAR
            }
            """.trimIndent()

        @Test
        fun `Given an enumeration class`() {
            val formattedCode =
                """
                enum class FooBar {
                    FOO,
                    BAR
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an enumeration class (tab indentation)`() {
            val formattedCode =
                """
                enum class FooBar {
                ${TAB}FOO,
                ${TAB}BAR
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .withEditorConfigOverride(INDENT_STYLE_TAB)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 1)")
                ).isFormattedAs(formattedCode)
        }
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
        val formattedCode =
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (3) (should be 2)"),
                LintViolation(3, 1, "Unexpected indentation (4) (should be 2)")
            ).isFormattedAs(formattedCode)
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
        val formattedCode =
            """
            fun main() {
            ${TAB}val v = ""
            ${TAB}println(v)
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (0) (should be 1)"),
                LintViolation(3, 1, "Unexpected indentation (2) (should be 1)")
            ).isFormattedAs(formattedCode)
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
            .hasNoLintViolations()
    }

    // https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting
    @Test
    fun `Given some function call`() {
        val code =
            """
            fun main() {
                foobar(
                      a,
                      b,
                      c
                      )
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                foobar(
                    a,
                    b,
                    c
                )
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (10) (should be 8)"),
                LintViolation(4, 1, "Unexpected indentation (10) (should be 8)"),
                LintViolation(5, 1, "Unexpected indentation (10) (should be 8)"),
                LintViolation(6, 1, "Unexpected indentation (10) (should be 4)")
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some KDoc incorrectly indented`() {
        val code =
            """
            data class Foo(
              /**
               * Some bar
               */
              var bar: String
            )
            """.trimIndent()
        val formattedCode =
            """
            data class Foo(
                /**
                 * Some bar
                 */
                var bar: String
            )
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (2) (should be 4)"),
                LintViolation(3, 1, "Unexpected indentation (3) (should be 5)"),
                LintViolation(4, 1, "Unexpected indentation (3) (should be 5)"),
                LintViolation(5, 1, "Unexpected indentation (2) (should be 4)")
            )
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class EndOfLineComment {
        @Test
        fun `Given some correctly indented EOL comment`() {
            val code =
                """
                fun foo() =
                    // Some comment
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given some incorrectly indented EOL comment`() {
            val code =
                """
                fun foo() =
                        // Some comment
                    "foo"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() =
                    // Some comment
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unexpected indentation (8) (should be 4)")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given EOL comment starting at position 0`() {
            val code =
                """
                fun foo() =
                // Some comment
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class BlockComment {
        @Test
        fun `Given some correctly indented block comment`() {
            val code =
                """
                fun foo() =
                    /*
                     * Some comment
                     */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given some incorrectly indented block comment (single line)`() {
            val code =
                """
                fun foo() =
                        /* Some comment */
                    "foo"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() =
                    /* Some comment */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unexpected indentation (8) (should be 4)")
                .isFormattedAs(formattedCode)
        }

        @Disabled("To be fixed")
        @Test
        fun `Given some incorrectly indented block comment`() {
            val code =
                """
                fun foo() =
                        /*
                         * Some comment
                         */
                    "foo"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() =
                    /*
                     * Some comment
                     */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (8) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (9) (should be 5)"),
                    LintViolation(4, 1, "Unexpected indentation (9) (should be 5)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given block comment starting at position 0`() {
            val code =
                """
                fun foo() =
                /*
                 * Some comment
                 */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class KDoc {
        @Test
        fun `Given some correctly indented KDoc`() {
            val code =
                """
                fun foo() =
                    /**
                     * Some comment
                     */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given some incorrectly indented KDoc (single line)`() {
            val code =
                """
                fun foo() =
                        /** Some comment */
                    "foo"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() =
                    /** Some comment */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolation(2, 1, "Unexpected indentation (8) (should be 4)")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some incorrectly indented KDoc`() {
            val code =
                """
                fun foo() =
                        /**
                         * Some comment
                         */
                    "foo"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() =
                    /**
                     * Some comment
                     */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (8) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (9) (should be 5)"),
                    LintViolation(4, 1, "Unexpected indentation (9) (should be 5)")
                ).isFormattedAs(formattedCode)
        }

        @Disabled("To be fixed")
        @Test
        fun `Given KDoc starting at position 0`() {
            val code =
                """
                fun foo() =
                /**
                 * Some comment
                 */
                    "foo"
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class NestedConditionals {
        @Test
        fun `Given a method chain combined with nested conditionals`() {
            val code =
                """
                val foo =
                listOf<Any>()
                .toString()
                .isEmpty() &&
                false ||
                (
                true ||
                false ||
                listOf<Any>()
                .toString()
                .isEmpty()
                ) ||
                false
                """.trimIndent()
            val formattedCode =
                """
                val foo =
                    listOf<Any>()
                        .toString()
                        .isEmpty() &&
                        false ||
                        (
                            true ||
                                false ||
                                listOf<Any>()
                                    .toString()
                                    .isEmpty()
                            ) ||
                        false
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 16)"),
                    LintViolation(9, 1, "Unexpected indentation (0) (should be 16)"),
                    LintViolation(10, 1, "Unexpected indentation (0) (should be 20)"),
                    LintViolation(11, 1, "Unexpected indentation (0) (should be 20)"),
                    LintViolation(12, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(13, 1, "Unexpected indentation (0) (should be 8)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a while statement with method chain combined with nested conditionals`() {
            val code =
                """
                fun foo() {
                while (
                listOf<Any>()
                .toString()
                .isEmpty() &&
                false ||
                (
                true ||
                false ||
                listOf<Any>()
                .toString()
                .isEmpty()
                ) ||
                false
                ) {
                println("hello")
                }
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    while (
                        listOf<Any>()
                            .toString()
                            .isEmpty() &&
                        false ||
                        (
                            true ||
                                false ||
                                listOf<Any>()
                                    .toString()
                                    .isEmpty()
                            ) ||
                        false
                    ) {
                        println("hello")
                    }
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(9, 1, "Unexpected indentation (0) (should be 16)"),
                    LintViolation(10, 1, "Unexpected indentation (0) (should be 16)"),
                    LintViolation(11, 1, "Unexpected indentation (0) (should be 20)"),
                    LintViolation(12, 1, "Unexpected indentation (0) (should be 20)"),
                    LintViolation(13, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(14, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(15, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(16, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(17, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an if-statement with method chain combined with nested conditionals`() {
            val code =
                """
                fun foo() {
                if (
                listOf<Any>()
                .toString()
                .isEmpty() &&
                false ||
                (
                true ||
                false ||
                listOf<Any>()
                .toString()
                .isEmpty()
                ) ||
                false
                ) {
                println("hello")
                }
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    if (
                        listOf<Any>()
                            .toString()
                            .isEmpty() &&
                        false ||
                        (
                            true ||
                                false ||
                                listOf<Any>()
                                    .toString()
                                    .isEmpty()
                            ) ||
                        false
                    ) {
                        println("hello")
                    }
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(9, 1, "Unexpected indentation (0) (should be 16)"),
                    LintViolation(10, 1, "Unexpected indentation (0) (should be 16)"),
                    LintViolation(11, 1, "Unexpected indentation (0) (should be 20)"),
                    LintViolation(12, 1, "Unexpected indentation (0) (should be 20)"),
                    LintViolation(13, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(14, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(15, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(16, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(17, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given some property accessors`() {
        val code =
            """
            class Foo {
            val b: Boolean
            get() = true

            var value: String = ""
            get() = ""
            set(v: String) { field = v }

            var valueMultiLine: String = ""
            get() {
            return ""
            }
                // comment
            set(v: String) {
            field = v
            }

            val isEmpty: Boolean
            get() = this.size == 0

            var counter = 0 // comment
            set(value) { // comment
            if (value >= 0) field = value
            }

            var setterVisibility: String = "abc"
            private set

            var setterWithAnnotation: Any? = null
            @Inject set

            var multilineInitialValue: String =
            "tooooooooooooo loooooooooooooooong"
            private set
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                val b: Boolean
                    get() = true

                var value: String = ""
                    get() = ""
                    set(v: String) { field = v }

                var valueMultiLine: String = ""
                    get() {
                        return ""
                    }
                    // comment
                    set(v: String) {
                        field = v
                    }

                val isEmpty: Boolean
                    get() = this.size == 0

                var counter = 0 // comment
                    set(value) { // comment
                        if (value >= 0) field = value
                    }

                var setterVisibility: String = "abc"
                    private set

                var setterWithAnnotation: Any? = null
                    @Inject set

                var multilineInitialValue: String =
                    "tooooooooooooo loooooooooooooooong"
                    private set
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(5, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(9, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(10, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(11, 1, "Unexpected indentation (0) (should be 12)"),
                LintViolation(12, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(13, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(14, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(15, 1, "Unexpected indentation (0) (should be 12)"),
                LintViolation(16, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(18, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(19, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(21, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(22, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(23, 1, "Unexpected indentation (0) (should be 12)"),
                LintViolation(24, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(26, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(27, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(29, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(30, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(32, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(33, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(34, 1, "Unexpected indentation (0) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline string template containing an incorrectly indented expression `() {
        // Interpret "$." in code samples below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            fun foo1() =
                "Sum of uneven numbers = $.{
                    listOf(1,2,3)
                        .filter { it % 2 == 0 }
                        .sum()
                }"
            fun foo2() = "Sum of uneven numbers = $.{
            listOf(1,2,3)
                .filter { it % 2 == 0 }
                .sum()
            }"
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (8) (should be 4)"),
                LintViolation(4, 1, "Unexpected indentation (12) (should be 8)"),
                LintViolation(5, 1, "Unexpected indentation (12) (should be 8)")
            ) // Can't check the formatted code here as the template string expression results in an error in AssertJ
    }

    @Nested
    inner class TestLintFirstLine {
        @Test
        fun `Given a file with indented code on the first line then report unexpected indentation on first line`() {
            val code =
                """
                |${SPACE}${SPACE}// comment
                """.trimMargin()
            val formattedCode =
                """
                |${SPACE}${SPACE}
                |// comment
                """.trimMargin()
            indentationRuleAssertThat(code)
                .hasLintViolation(1, 1, "Unexpected indentation (2) (should be 0)")
                // TODO: It is unexpected that a line break is introduced instead of removing the unwanted spacing
                .isFormattedAs(formattedCode)
        }

        @ParameterizedTest(name = "As Kotlin script: {0}")
        @ValueSource(booleans = [true, false])
        fun `Given a file with blanks only on the first line then do not report unexpected indentation for that first line`(
            asKotlinScript: Boolean
        ) {
            val code =
                """
                |${SPACE}${SPACE}
                |${SPACE}${SPACE}// comment
                """.trimMargin()
            val formattedCode =
                """
                |${SPACE}${SPACE}
                |// comment
                """.trimMargin()
            indentationRuleAssertThat(code)
                // Note that no LintError is created for the first line as it does not contain any code
                .hasLintViolation(2, 1, "Unexpected indentation (2) (should be 0)")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class BinaryExpression {
        @Test
        fun `Given a multiline binary expression of simple additions`() {
            val code =
                """
                val foo1 =
                    1 + 2 +
                        3 +
                4
                val foo2 =
                    "a" + "b" +
                        "c" +
                "d"
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    1 + 2 +
                        3 +
                        4
                val foo2 =
                    "a" + "b" +
                        "c" +
                        "d"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 8)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a binary expression of strings created from chained methods and function calls`() {
            val code =
                """
                fun bar(string: String) = string
                val foo =
                listOf("foo", "bar").joinToString {
                it.toUpperCaseAsciiOnly()
                } + bar(
                "foo"
                ) + bar(
                "foo"
                )
                """.trimIndent()
            val formattedCode =
                """
                fun bar(string: String) = string
                val foo =
                    listOf("foo", "bar").joinToString {
                        it.toUpperCaseAsciiOnly()
                    } + bar(
                        "foo"
                    ) + bar(
                        "foo"
                    )
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(9, 1, "Unexpected indentation (0) (should be 4)")
                )
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a binary expression in a map entry`() {
            val code =
                """
                val foo1 =
                "bar" to
                "a" +
                "b" +
                "c"
                val foo2 =
                "bar" to
                "a"
                .plus("b")
                .plus("c")
                """.trimIndent()
            val formattedCode =
                """
                val foo1 =
                    "bar" to
                        "a" +
                        "b" +
                        "c"
                val foo2 =
                    "bar" to
                        "a"
                            .plus("b")
                            .plus("c")
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(9, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(10, 1, "Unexpected indentation (0) (should be 12)")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a binary expression in annotation parameter`() {
            val code =
                """
                annotation class Bar(val description: String, val names: Array<String>)

                @Bar(
                description = "foo",
                names = arrayOf(
                "foo" +
                "bar"
                )
                )
                private val foo: String = "foo"
                """.trimIndent()
            val formattedCode =
                """
                annotation class Bar(val description: String, val names: Array<String>)

                @Bar(
                    description = "foo",
                    names = arrayOf(
                        "foo" +
                            "bar"
                    )
                )
                private val foo: String = "foo"
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(5, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                    LintViolation(7, 1, "Unexpected indentation (0) (should be 12)"),
                    LintViolation(8, 1, "Unexpected indentation (0) (should be 4)")
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a when-statement then the value might be placed at a separate line`() {
        val code =
            """
            val foo = when (1) {
                1 ->
            "1"
                else ->
            "2"
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = when (1) {
                1 ->
                    "1"
                else ->
                    "2"
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(5, 1, "Unexpected indentation (0) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a when-statement then the value might be placed in a block which opened on same line as arrow`() {
        val code =
            """
            val foo = when (1) {
                1 -> {
            "1"
            }
                else -> {
            "2"
            }
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo = when (1) {
                1 -> {
                    "1"
                }
                else -> {
                    "2"
                }
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(6, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(7, 1, "Unexpected indentation (0) (should be 4)")
            ).isFormattedAs(formattedCode)
    }

    val foo1 = 0 + 1 + when {
        else -> 2 + 3
    } + 4
    val foo2 = 0 + 1 + when {
        else -> 2 + 3
    } + 4

    @Test
    fun `Given a when-statement in a list of additions`() {
        val code =
            """
            val foo1 = 0 + 1 + when {
                else -> 2 + 3
            } + 4
            val foo2 = when {
                true -> 0 + 1 + when {
                    else -> 2 + 3
                } + 4
                else -> -1
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 916 - Given a multiline statement, not wrapped inside a block, as value in when-statement starting on the same line as the arrow`() {
        val code =
            """
            val foo = when (1) {
                1 -> if (true) {
                    2
                } else {
                    3
                }
                2 -> 1.let {
                    it + 1
                }.let {
                    it + 1
                }
                else -> 0
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given if-else-statement for which branches are not wrapped in block`() {
        val code =
            """
            val foo = if (true)
                1
            else
                2
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given if-else-statement for which branches are wrapped between parenthesis`() {
        val code =
            """
            val foo = if (true) (
                1 + 2
                ) else ( // IDEA quirk
                3 + 4
                ) // IDEA quirk
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a while statement for which the body is not wrapped in a block`() {
        val code =
            """
            fun foo() {
                while (true)
                    println()
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some dot qualified expressions`() {
        val code =
            """
            val foo1 =
            nullableList
            .find { !it.empty() }
            ?.map { x + 2 }
            ?.filter { true }
            val foo2 =
            listOf(listOf(1, 2, 3))
            .map {
            it
            .map { it + 1 }
            .filter { it > 3 }
            }
            .reduce { acc, curr -> acc + curr }
            .toString()
            val foo3 = 1
            """.trimIndent()
        val formattedCode =
            """
            val foo1 =
                nullableList
                    .find { !it.empty() }
                    ?.map { x + 2 }
                    ?.filter { true }
            val foo2 =
                listOf(listOf(1, 2, 3))
                    .map {
                        it
                            .map { it + 1 }
                            .filter { it > 3 }
                    }
                    .reduce { acc, curr -> acc + curr }
                    .toString()
            val foo3 = 1
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(4, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(7, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(8, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(9, 1, "Unexpected indentation (0) (should be 12)"),
                LintViolation(10, 1, "Unexpected indentation (0) (should be 16)"),
                LintViolation(11, 1, "Unexpected indentation (0) (should be 16)"),
                LintViolation(12, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(13, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(14, 1, "Unexpected indentation (0) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an elvis operator with an incorrect indented dot qualified expression`() {
        val code =
            """
            fun bar1(): Int? = null
            fun bar2(): List<Int> = emptyList()
            fun foo(): Int? = bar1()
                ?: bar2().firstOrNull()
                .also {
                    println("bar2")
                }
            """.trimIndent()
        val formattedCode =
            """
            fun bar1(): Int? = null
            fun bar2(): List<Int> = emptyList()
            fun foo(): Int? = bar1()
                ?: bar2().firstOrNull()
                    .also {
                        println("bar2")
                    }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(6, 1, "Unexpected indentation (8) (should be 12)"),
                LintViolation(7, 1, "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an elvis operator after a dot qualified expression`() {
        val code =
            """
            fun bar1(): Int? = null
            fun bar2(): List<Int> = emptyList()
            fun foo(): Int? =
                bar1().also {
                    println("bar1")
                }
                ?: bar2().also {
                    println("bar2")
                }
            """.trimIndent()
        val formattedCode =
            """
            fun bar1(): Int? = null
            fun bar2(): List<Int> = emptyList()
            fun foo(): Int? =
                bar1().also {
                    println("bar1")
                }
                    ?: bar2().also {
                        println("bar2")
                    }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(7, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(8, 1, "Unexpected indentation (8) (should be 12)"),
                LintViolation(9, 1, "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

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
        indentationRuleAssertThat(code)
            .addAdditionalRules(WrappingRule())
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(4, 1, "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline raw string literal then wrap and indent conditionally`() {
        val code =
            """
            fun foo() {
                println($MULTILINE_STRING_QUOTE
                $MULTILINE_STRING_QUOTE)
                println($MULTILINE_STRING_QUOTE
                $MULTILINE_STRING_QUOTE.trimIndent())
                println($MULTILINE_STRING_QUOTE
                $MULTILINE_STRING_QUOTE.trimMargin())
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                println(
                    $MULTILINE_STRING_QUOTE
                $MULTILINE_STRING_QUOTE
                )
                println(
                    $MULTILINE_STRING_QUOTE
                    $MULTILINE_STRING_QUOTE.trimIndent()
                )
                println(
                    $MULTILINE_STRING_QUOTE
                    $MULTILINE_STRING_QUOTE.trimMargin()
                )
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .addAdditionalRules(WrappingRule())
            .hasLintViolations(
                LintViolation(5, 1, "Unexpected indent of multiline string closing quotes"),
                LintViolation(7, 1, "Unexpected indent of multiline string closing quotes")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some multiline raw string literal contain multiline string templates`() {
        val code =
            """
            fun foo1() {
                foo2($MULTILINE_STRING_QUOTE$.{
            true
                }
                text
            _$.{
            true
                }$MULTILINE_STRING_QUOTE.trimIndent(), ${MULTILINE_STRING_QUOTE}text$MULTILINE_STRING_QUOTE)
            }
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        val formattedCode =
            """
            fun foo1() {
                foo2(
                    $MULTILINE_STRING_QUOTE$.{
                    true
                    }
                text
            _$.{
                    true
                    }
                    $MULTILINE_STRING_QUOTE.trimIndent(),
                    ${MULTILINE_STRING_QUOTE}text$MULTILINE_STRING_QUOTE
                )
            }
            """.trimIndent()
                .replacePlaceholderWithStringTemplate()
        indentationRuleAssertThat(code)
            .addAdditionalRules(WrappingRule())
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(4, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(7, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(8, 1, "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type alias of a lambda`() {
        val code =
            """
            typealias F = (
            v: String
            ) -> Unit
            """.trimIndent()
        val formattedCode =
            """
            typealias F = (
                v: String
            ) -> Unit
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Unexpected indentation (0) (should be 4)")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some call to a function with a lambda as parameter`() {
        val code =
            """
            fun main() {
            f({ v ->
            d(
            1
            )
            })
            f({ v ->
            x
            .f()
            })
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                f({ v ->
                    d(
                        1
                    )
                })
                f({ v ->
                    x
                        .f()
                })
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(3, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(4, 1, "Unexpected indentation (0) (should be 12)"),
                LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(6, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(7, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(8, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(9, 1, "Unexpected indentation (0) (should be 12)"),
                LintViolation(10, 1, "Unexpected indentation (0) (should be 4)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a when statement with multiple values in the clause then those can be separated to distinct lines `() {
        val code =
            """
            val foo =
                when {
                    1, 2 -> "a"
                    3,
                    4 -> "b"
                    5,
                    6 ->
                        "c"
                }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a when-statement with an EOL comment before or in the value list`() {
        val code =
            """
            fun foo() {
                when {
                    // comment
                    true -> {
                    }
                }
                when {
                    1, // first element
                    2 // second element
                    -> true
                }
            }
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an incorrectly indented lambda block`() {
        val code =
            """
            fun main() {
                foo.func {
                    param1, param2 ->
                        doSomething()
                        doSomething2()
                    }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun main() {
                foo.func {
                        param1, param2 ->
                    doSomething()
                    doSomething2()
                }
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (8) (should be 12)"),
                LintViolation(4, 1, "Unexpected indentation (12) (should be 8)"),
                LintViolation(5, 1, "Unexpected indentation (12) (should be 8)"),
                LintViolation(6, 1, "Unexpected indentation (8) (should be 4)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some EOL comment after assignment`() {
        val code =
            """
            val foo = // comment
                "foo"
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
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
            class C (val a: Int, val b: Int, val e: (
                r: Int
            ) -> Unit, val c: Int, val d: Int) {
                fun f(a: Int, b: Int, e: (
                    r: Int
                ) -> Unit, c: Int, d: Int) {}
            }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(4, 1, "Unexpected indentation (0) (should be 4)"),
                LintViolation(5, 1, "Unexpected indentation (0) (should be 8)"),
                LintViolation(6, 1, "Unexpected indentation (0) (should be 4)")
            ).isFormattedAs(formattedCode)
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected space character(s)"),
                LintViolation(3, 1, "Unexpected space character(s)")
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected tab character(s)"),
                LintViolation(2, 1, "Unexpected indentation (8) (should be 4)"),
                LintViolation(3, 1, "Unexpected tab character(s)"),
                LintViolation(3, 1, "Unexpected indentation (4) (should be 0)")
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected tab character(s)"),
                LintViolation(3, 1, "Unexpected tab character(s)"),
                LintViolation(4, 1, "Unexpected tab character(s)")
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected tab character(s)"),
                LintViolation(3, 1, "Unexpected tab character(s)"),
                LintViolation(4, 1, "Unexpected tab character(s)")
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
            .hasLintViolation(4, 1, "Unexpected indentation (16) (should be 12)")
            .isFormattedAs(formattedCode)
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
            .hasLintViolation(2, 1, "Unexpected indentation (0) (should be 4)")
            .isFormattedAs(formattedCode)
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (8) (should be 4)"),
                LintViolation(6, 1, "Unexpected indentation (4) (should be 8)")
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class RawStringLiterals {
        @Test
        fun `Given a raw string literal with an indented string template`() {
            val code =
                """
                val foo =
                    $MULTILINE_STRING_QUOTE
                        ${true}
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a raw string literal as single line`() {
            val code =
                """
                val foo = $MULTILINE_STRING_QUOTE${true}$MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a empty raw string literal with closing and ending quotes on different lines`() {
            val code =
                """
                val foo1 =
                    $MULTILINE_STRING_QUOTE
                    $MULTILINE_STRING_QUOTE.trimIndent()
                val foo2 =
                    $MULTILINE_STRING_QUOTE

                    $MULTILINE_STRING_QUOTE.trimIndent()
                val foo3 =
                $MULTILINE_STRING_QUOTE

                $MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a raw string literal assignment to variable with opening quotes on same line as declaration`() {
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
                .hasLintViolation(5, 1, "Unexpected indent of multiline string closing quotes")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a raw string literal containing quotation marks for which the closing quotes are not correctly indented`() {
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
                .hasLintViolation(8, 1, "Unexpected indent of multiline string closing quotes")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a multiline raw string literal as function call parameter but not starting and ending on a separate line`() {
            val code =
                """
                fun foo() {
                println($MULTILINE_STRING_QUOTE
                    text

                        text
                $MULTILINE_STRING_QUOTE.trimIndent().toByteArray())
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    println(
                        $MULTILINE_STRING_QUOTE
                    text

                        text
                        $MULTILINE_STRING_QUOTE.trimIndent().toByteArray()
                    )
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .addAdditionalRules(WrappingRule())
                .hasLintViolations(
                    LintViolation(2, 1, "Unexpected indentation (0) (should be 4)"),
                    LintViolation(6, 1, "Unexpected indent of multiline string closing quotes")
                ).isFormattedAs(formattedCode)
        }

        @Disabled("to be fixed")
        @Test
        fun `Given a multiline raw string literal in which the opening quotes are followed with some text on the same line`() {
            val code =
                """
                val foo1 = ${MULTILINE_STRING_QUOTE}Text
                    More text
                   $MULTILINE_STRING_QUOTE
                val foo2 = ${MULTILINE_STRING_QUOTE}Text
                $MULTILINE_STRING_QUOTE
                """.trimIndent()
            val formattedCode =
                """
                val foo1 = ${MULTILINE_STRING_QUOTE}
                Text
                    More text
                    $MULTILINE_STRING_QUOTE
                val foo2 = ${MULTILINE_STRING_QUOTE}
                    Text
                    $MULTILINE_STRING_QUOTE
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 3, "Unexpected indent of multiline string closing quotes"),
                    LintViolation(5, 1, "Unexpected indent of multiline string closing quotes")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a multiline raw string literal as non-first function call parameter`() {
            val code =
                """
                fun foo() {
                    write(fs.getPath("/projects/.editorconfig"), $MULTILINE_STRING_QUOTE
                        root = true
                        [*]
                        end_of_line = lf
                    $MULTILINE_STRING_QUOTE.trimIndent().toByteArray())
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    write(
                        fs.getPath("/projects/.editorconfig"),
                        $MULTILINE_STRING_QUOTE
                        root = true
                        [*]
                        end_of_line = lf
                        $MULTILINE_STRING_QUOTE.trimIndent().toByteArray()
                    )
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .addAdditionalRules(WrappingRule())
                .hasLintViolation(6, 1, "Unexpected indent of multiline string closing quotes")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given some class variables having a raw string literal been assigned as value`() {
            val code =
                """
                class Foo {
                    val foo1 = $MULTILINE_STRING_QUOTE
                        {
                        }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                    val foo2 =
                    $MULTILINE_STRING_QUOTE
                        {
                        }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                    val foo3 = // comment
                    $MULTILINE_STRING_QUOTE
                    {
                    }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                }
                """.trimIndent()
            val formattedCode =
                """
                class Foo {
                    val foo1 = $MULTILINE_STRING_QUOTE
                        {
                        }
                    $MULTILINE_STRING_QUOTE.trimIndent()
                    val foo2 =
                        $MULTILINE_STRING_QUOTE
                        {
                        }
                        $MULTILINE_STRING_QUOTE.trimIndent()
                    val foo3 = // comment
                        $MULTILINE_STRING_QUOTE
                    {
                    }
                        $MULTILINE_STRING_QUOTE.trimIndent()
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(7, 1, "Unexpected indentation (4) (should be 8)"),
                    LintViolation(10, 1, "Unexpected indent of multiline string closing quotes"),
                    LintViolation(12, 1, "Unexpected indentation (4) (should be 8)"),
                    LintViolation(15, 1, "Unexpected indent of multiline string closing quotes")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `format raw string literal containing a template string as the first non blank element on the line`() {
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
                    println(
                $MULTILINE_STRING_QUOTE
                ${"$"}{true}

                    ${"$"}{true}
                $MULTILINE_STRING_QUOTE.trimIndent()
                    )
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolation(7, 1, "Unexpected indent of multiline string closing quotes")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 575 - Given a raw string literal which is properly indented but does contain tabs after the margin then do not return lint errors`() {
            val code =
                """
                val str =
                    $MULTILINE_STRING_QUOTE
                    ${TAB}Tab at the beginning of this line but after the indentation margin
                    Tab${TAB}in the middle of this string
                    Tab at the end of this line.$TAB
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a raw string literal with mixed indentation characters, can not be autocorrected`() {
            val code =
                """
                val foo = $MULTILINE_STRING_QUOTE
                      line1
                ${TAB}line2
                    $MULTILINE_STRING_QUOTE.trimIndent()
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    1,
                    11,
                    "Indentation of multiline string should not contain both tab(s) and space(s)"
                )
        }

        @Test
        fun `Give a raw string literal at start of line`() {
            val code =
                """
                fun foo() =
                $MULTILINE_STRING_QUOTE
                some text
                $MULTILINE_STRING_QUOTE
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1127 - Given a raw string literal followed by trimIndent in parameter list`() {
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
            indentationRuleAssertThat(code)
                .addAdditionalRules(WrappingRule())
                .hasLintViolation(6, 1, "Unexpected indent of multiline string closing quotes")
                .isFormattedAs(formattedCode)
        }
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1340 - Given a dot-qualified-expression as delegated property value`() {
            val code =
                """
                class Foo {
                    private val foo
                        by option("--myOption")
                                .int()
                                    .default(1)
                }
                """.trimIndent()
            val formattedCode =
                """
                class Foo {
                    private val foo
                        by option("--myOption")
                            .int()
                            .default(1)
                }
                """.trimIndent()
            indentationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 1, "Unexpected indentation (16) (should be 12)"),
                    LintViolation(5, 1, "Unexpected indentation (20) (should be 12)")
                )
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 1340 - Given a dot-qualified-expression wrapped in a block as delegated property value`() {
            val code =
                """
                class MyCliktCommand : CliktCommand() {
                    private val myOption
                        by {
                            option("--myOption")
                                .int()
                                .default(1)
                        }
                }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
            indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
            .hasNoLintViolations()
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
            .hasLintViolation(3, 1, "Unexpected indentation (12) (should be 8)")
            .isFormattedAs(formattedCode)
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
            .hasLintViolation(4, 1, "Unexpected indentation (8) (should be 12)")
            .isFormattedAs(formattedCode)
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(8, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(9, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(15, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(21, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(22, 1, "Unexpected indentation (4) (should be 8)")
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(8, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(9, 1, "Unexpected indentation (4) (should be 8)")
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
            .hasLintViolations(
                LintViolation(2, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(3, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(9, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(10, 1, "Unexpected indentation (4) (should be 8)"),
                LintViolation(11, 1, "Unexpected indentation (4) (should be 8)")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1210 - Given a supertype delegate`() {
        val code =
            """
            object ApplicationComponentFactory : ApplicationComponent.Factory
            by DaggerApplicationComponent.factory()
            """.trimIndent()
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
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
        indentationRuleAssertThat(code).hasNoLintViolations()
    }

    @Disabled("To be fixed as code as lambda with parameters on multiple line")
    @Test
    fun `Given a function with lambda parameters on multiple lines then align the parameters`() {
        val code =
            """
            val fieldExample =
                LongNameClass { paramA,
                        paramB,
                        paramC ->
                    ClassB(paramA, paramB, paramC)
                }
            """.trimIndent()
        val formattedCode =
            """
            val fieldExample =
                LongNameClass { paramA,
                                paramB,
                                paramC ->
                    ClassB(paramA, paramB, paramC)
                }
            """.trimIndent()
        indentationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 1, "Unexpected indentation (12) (should be 20)"),
                LintViolation(4, 1, "Unexpected indentation (12) (should be 20)")
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class Issue1350 {
        @Test
        fun `Issue 1350 - Given a for-loop containing a newline before the declaration then do not indent it to keep it in sync with IntelliJ default formatting`() {
            val code =
                """
                fun foo() {
                    for (
                    item in listOf(
                        "a",
                        "b"
                    )) {
                        println(item)
                    }
                }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1350 - Given a for-loop containing a newline before the 'in' operator then do not indent it to keep it in sync with IntelliJ default formatting`() {
            val code =
                """
                fun foo() {
                    for (item
                    in listOf(
                        "a",
                        "b"
                    )) {
                        println(item)
                    }
                }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1350 - Given a for-loop containing a newline before the expression then do not indent it to keep it in sync with IntelliJ default formatting`() {
            val code =
                """
                fun foo() {
                    for (item in
                    listOf(
                        "a",
                        "b"
                    )) {
                        println(item)
                    }
                }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1350 - Given a for-loop with a multiline expression then indent that expression normally`() {
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
            indentationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @DisplayName("Issue 1335 - Given a property with an i")
    @Nested
    inner class Issue1335 {
        @Test
        fun `Issue 1335 Given a property with an initializer on a separate line followed by a getter with a multiline body expression`() {
            val code =
                """
                private val foo: String =
                    "foo"
                    get() =
                        listOf("a", value, "c")
                            .filterNotNull()
                            .joinToString()
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1335 Given a property with an initializer on a separate line followed by a getter with a block body containing a multiline dot qualified expression`() {
            val code =
                """
                private val foo1: String =
                    "foo"
                    get() {
                        return listOf("a", value, "c")
                            .filterNotNull()
                            .joinToString()
                    }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1335 - Given a property with an initializer on a separate line followed by a setter with a block body a multiline dot qualified expression`() {
            val code =
                """
                private var foo: String =
                    "foo"
                    set(value) {
                        listOf("a", value, "c")
                            .filterNotNull()
                            .joinToString()
                    }
                """.trimIndent()
            indentationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        val INDENT_STYLE_TAB = indentStyleProperty to PropertyType.IndentStyleValue.tab
    }
}

// Replace the "$." placeholder with an actual "$" so that string "$.{expression}" is transformed to a String template
// "${expression}".
private fun String.replacePlaceholderWithStringTemplate() = replace("$.", "${'$'}")
