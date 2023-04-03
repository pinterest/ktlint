package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnnotationRuleTest {
    private val annotationRuleAssertThat = assertThatRule { AnnotationRule() }

    @Test
    fun `Given a single annotation on same line before the annotated construct`() {
        val code =
            """
            @FunctionalInterface class FooBar {
                @JvmField var foo: String
                @Test fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single annotation on line above the annotated construct`() {
        val code =
            """
            @FunctionalInterface
            class FooBar {
                @JvmField
                var foo: String
                @Test
                fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an annotation with a parameter followed by a EOL comment`() {
        val code =
            """
            @Suppress("AnnotationRule") // some comment
            class FooBar {
                @Suppress("AnnotationRule") // some comment
                var foo: String
                @Suppress("AnnotationRule") // some comment
                fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an annotation with a parameter on same line as annotation construct (possibly separated by a block comment or KDoc)`() {
        val code =
            """
            @Suppress("AnnotationRule") class FooBar1 {
                @Suppress("AnnotationRule") var foo: String
                @Suppress("AnnotationRule") fun bar() {}
            }
            @Suppress("AnnotationRule") /* some comment */ class FooBar2 {
                @Suppress("AnnotationRule") /* some comment */ var foo: String
                @Suppress("AnnotationRule") /* some comment */ fun bar() {}
            }
            @Suppress("AnnotationRule") /** some comment */ class FooBar3 {
                @Suppress("AnnotationRule") /** some comment */ var foo: String
                @Suppress("AnnotationRule") /** some comment */ fun bar() {}
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("AnnotationRule")
            class FooBar1 {
                @Suppress("AnnotationRule")
                var foo: String
                @Suppress("AnnotationRule")
                fun bar() {}
            }
            @Suppress("AnnotationRule")
            /* some comment */ class FooBar2 {
                @Suppress("AnnotationRule")
                /* some comment */ var foo: String
                @Suppress("AnnotationRule")
                /* some comment */ fun bar() {}
            }
            @Suppress("AnnotationRule")
            /** some comment */ class FooBar3 {
                @Suppress("AnnotationRule")
                /** some comment */ var foo: String
                @Suppress("AnnotationRule")
                /** some comment */ fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 1, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(2, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(3, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(5, 1, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(6, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(7, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(9, 1, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(10, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(11, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple annotations on same line as annotated construct`() {
        val code =
            """
            @Foo @Bar class FooBar {
                @Foo @Bar var foo: String
                @Foo @Bar fun bar() {}
            }
            """.trimIndent()
        val formattedCode =
            """
            @Foo @Bar
            class FooBar {
                @Foo @Bar
                var foo: String
                @Foo @Bar
                fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Multiple annotations should not be placed on the same line as the annotated construct"),
                LintViolation(2, 15, "Multiple annotations should not be placed on the same line as the annotated construct"),
                LintViolation(3, 15, "Multiple annotations should not be placed on the same line as the annotated construct"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an array of annotations on the same line as the annotated construct then do not report a violation`() {
        val code =
            """
            @[Foo Bar] class FooBar2 {
                @[Foo Bar] var foo: String
                @[Foo Bar] fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given multiple annotations on same line as annotated construct (without indentation)`() {
        val code =
            """
            @JvmField @Volatile var foo: String
            """.trimIndent()
        val formattedCode =
            """
            @JvmField @Volatile
            var foo: String
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolation(1, 21, "Multiple annotations should not be placed on the same line as the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple annotations on same line as annotated construct (without indentation but preceded by one or more blank line)`() {
        val code =
            """
            @JvmField @Volatile var foo: String
            """.trimIndent()
        val formattedCode =
            """
            @JvmField @Volatile
            var foo: String
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolation(1, 21, "Multiple annotations should not be placed on the same line as the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotation with a parameter not followed by a space but on same line as annotated construct`() {
        val code =
            """
            @Suppress("AnnotationRule")class FooBar {
                @Suppress("AnnotationRule")var foo: String
                @Suppress("AnnotationRule")fun bar() {}
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("AnnotationRule")
            class FooBar {
                @Suppress("AnnotationRule")
                var foo: String
                @Suppress("AnnotationRule")
                fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 1, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(2, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(3, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline annotation and annotated construct on same line as closing parenthesis of the annotation`() {
        val code =
            """
            class FooBar {
                @Foo(
                    groups = [
                        "a",
                        "b"
                    ]
                ) val bar: Any
            }
            """.trimIndent()
        val formattedCode =
            """
            class FooBar {
                @Foo(
                    groups = [
                        "a",
                        "b"
                    ]
                )
                val bar: Any
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolation(2, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a annotation with parameter followed by an annotation without parameters on the same line as the annotated construct`() {
        val code =
            """
            class FooBar {
                @Foo("foo")
                @Bar val bar: Any
            }
            """.trimIndent()
        val formattedCode =
            """
            class FooBar {
                @Foo("foo")
                @Bar
                val bar: Any
            }
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        annotationRuleAssertThat(code)
            .hasLintViolation(3, 5, "Annotation must be placed on a separate line when it is preceded by another annotation on a separate line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 497 - Given a data class`() {
        val code =
            """
            data class Foo(val bar: String)
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 509 - Given a overridden function class`() {
        val code =
            """
            override fun foo()
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 552 - Given multiple blank lines before annotation`() {
        val code =
            """


            @JvmField
            var foo: String
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 628 - Given an annotation before the primary constructor `() {
        val code =
            """
            class Foo @Inject internal constructor()
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 642 - Given annotations on method parameters on same line as parameter`() {
        val code =
            """
            fun foo1(
                @Path("fooId") fooId: String,
                @Path("bar") bar: String,
                @Body body: Foo
            ): Completable

            fun foo2(@Query("include") include: String? = null, @QueryMap fields: Map<String, String> = emptyMap()): Single

            fun foo3(@Path("fooId") fooId: String): Completable
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 642 - Given annotations on constructor parameters on same line as parameter`() {
        val code =
            """
            class Foo(@Path("fooId") val fooId: String)
            class Bar(
                @NotNull("fooId") val fooId: String,
                @NotNull("bar") bar: String
            )
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 642 - Given annotations on arguments on same line as argument`() {
        val code =
            """
            val foo =
                foo(
                    @ExpressionStringAnn("foo") "test",
                    @ExpressionIntAnn("bar") 42
                )
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function signature having annotated parameters, then allow the annotation to be on the same line as the annotated construct`() {
        val code =
            """
            fun foo(
                a: int,
                @Bar1 b: int,
                @Bar1 @Bar2 c: int,
                @Bar3("bar3") @Bar1 d: int
            ) {}
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 740 - Given multiple annotations and ending with a EOL comment before annotated construct`() {
        val code =
            """
            annotation class A
            annotation class B

            @A // comment
            @B
            fun foo1() {}

            @A
            @B // comment
            fun foo2() {}
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given an annotation at file level` {
        @Test
        fun `Issue 714 - Given a file annotation without parameter on same line as package`() {
            val code =
                """
                @file:JvmName package foo.bar
                """.trimIndent()
            val formattedCode =
                """
                @file:JvmName

                package foo.bar
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasLintViolation(1, 15, "File annotations should be separated from file contents with a blank line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 624 - Given a file annotation with parameter on same line as package`() {
            val code =
                """
                @file:JvmName("FooClass") package foo.bar
                """.trimIndent()
            val formattedCode =
                """
                @file:JvmName("FooClass")

                package foo.bar
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            annotationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 1, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                    LintViolation(1, 27, "File annotations should be separated from file contents with a blank line"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 714 - Given a file annotation on the line above the package statement but without blank line in between`() {
            val code =
                """
                @file:JvmName
                package foo.bar
                """.trimIndent()
            val formattedCode =
                """
                @file:JvmName

                package foo.bar
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasLintViolation(2, 1, "File annotations should be separated from file contents with a blank line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 714 - Given a file annotation followed by an EOL comment, on the line above the package statement but without blank line in between`() {
            val code =
                """
                @file:JvmName  // comment
                package foo.bar
                """.trimIndent()
            val formattedCode =
                """
                @file:JvmName  // comment

                package foo.bar
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasLintViolation(2, 1, "File annotations should be separated from file contents with a blank line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Issue 714 - Given a file annotation followed by an block comment, on the same line as the package statement`() {
            val code =
                """
                @file:JvmName /* comment */ package foo.bar
                """.trimIndent()
            val formattedCode =
                """
                @file:JvmName /* comment */

                package foo.bar
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasLintViolation(1, 29, "File annotations should be separated from file contents with a blank line")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `lint file annotations should be separated with a blank line in script 1`() {
            val code =
                """
                @file:Suppress("UnstableApiUsage")
                pluginManagement {
                }
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("UnstableApiUsage")

                pluginManagement {
                }
                """.trimIndent()
            annotationRuleAssertThat(code)
                .asKotlinScript()
                .hasLintViolation(2, 1, "File annotations should be separated from file contents with a blank line")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a receiver with annotation having a parameter should not be separate line`() {
        val code =
            """
            annotation class Ann(val arg: Int = 0)

            fun @receiver:Ann(1) String.test() {}

            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1539 - Given an annotation with parameter followed by an EOL comment and followed by another annotation`() {
        val code =
            """
            @Suppress("AnnotationRule") // some comment
            @Bar
            class Foo
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1539 - Given an annotation with parameter followed by an EOL comment on separate line before annotated construct`() {
        val code =
            """
            @Suppress("AnnotationRule")
            // some comment between last annotation and annotated construct
            class Foo
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given multiple annotations on the same line without parameter but above the annotated construct`() {
        val code =
            """
            @Foo1 @Foo2
            fun foo() {}
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a line containing multiple annotations without parameters and another line with another annotation`() {
        val code =
            """
            @Foo1
            @Foo2 @Foo3
            fun foo() {}
            """.trimIndent()
        val formattedCode =
            """
            @Foo1
            @Foo2
            @Foo3
            fun foo() {}
            """.trimIndent()
        annotationRuleAssertThat(code)
            .asKotlinScript()
            .hasLintViolation(2, 7, "All annotations should either be on a single line or all annotations should be on a separate line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotated expression on same line as annotated construct and the annotation contains a parameter then report a violation which can not be autocorrected`() {
        val code =
            """
            fun foo() = @Suppress("DEPRECATION") bar()
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(
                1,
                13,
                "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct",
            )
    }

    @Test
    fun `Given an annotation with multiple annotation entries then do not force wrapping of the entries`() {
        val code =
            """
            @[JvmStatic Provides]
            fun foo() = 42
            """.trimIndent()
        annotationRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested inner class `Array syntax annotations, Issue #1765` {
        @Test
        fun `annotation preceded by array syntax annotation`() {
            val code =
                """
                class Main {
                    @[Foo1 Foo2] @Foo3
                    fun foo() {}
                }
                """.trimIndent()
            val formattedCode =
                """
                class Main {
                    @[Foo1 Foo2]
                    @Foo3
                    fun foo() {}
                }
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasLintViolation(2, 5, "@[...] style annotations should be on a separate line from other annotations.")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `annotation followed by array syntax annotation`() {
            val code =
                """
                @Foo3 @[Foo1 Foo2]
                fun foo() {}
                """.trimIndent()
            val formattedCode =
                """
                @Foo3
                @[Foo1 Foo2]
                fun foo() {}
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasLintViolation(1, 7, "@[...] style annotations should be on a separate line from other annotations.")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a single annotation on same line as a type parameter then do not report a violation`() {
        val code =
            """
            @Target(AnnotationTarget.TYPE)
            annotation class Foo
            val foo1: List<@Foo String> = emptyList()
            val foo2 = emptyList<@Foo String>()
            val foo3: Map<@Foo String, @Foo String> = emptyMap()
            val foo4 = emptyMap<@Foo String, @Foo String>()
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Nested
    inner class `Issue 1725 - Given multiple annotations on same line as a type parameter` {
        @Test
        fun `Given a list with multiple annotations on its type`() {
            val code =
                """
                val fooBar: List<@Foo @Bar String> = emptyList()
                """.trimIndent()
            val formattedCode =
                """
                val fooBar: List<
                    @Foo @Bar
                    String
                > = emptyList()
                """.trimIndent()
            annotationRuleAssertThat(code)
                .addAdditionalRuleProvider { TrailingCommaOnDeclarationSiteRule() }
                .addAdditionalRuleProvider { WrappingRule() }
                .hasLintViolations(
                    LintViolation(1, 17, "Expected newline after '<'"),
                    LintViolation(1, 28, "Multiple annotations should not be placed on the same line as the annotated construct"),
                    LintViolation(1, 34, "Expected newline before '>'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a custom type with multiple annotations on it type parameter(s)`() {
            val code =
                """
                val fooBar: FooBar<String, @Foo String, @Foo @Bar String, @Bar("bar") @Foo String> = FooBar()
                """.trimIndent()
            val formattedCode =
                """
                val fooBar: FooBar<
                    String,
                    @Foo String,
                    @Foo @Bar
                    String,
                    @Bar("bar")
                    @Foo
                    String
                    > = FooBar()
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            annotationRuleAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .addAdditionalRuleProvider { WrappingRule() }
                .hasLintViolations(
                    LintViolation(1, 39, "Expected newline after ','"),
                    LintViolation(1, 51, "Multiple annotations should not be placed on the same line as the annotated construct"),
                    LintViolation(1, 57, "Expected newline after ','"),
                    LintViolation(1, 59, "Expected newline before '@'"),
                    LintViolation(1, 59, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                    LintViolation(1, 76, "Multiple annotations should not be placed on the same line as the annotated construct"),
                    LintViolation(1, 82, "Expected newline before '>'"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given an array of annotations on same line as a type parameter` {
        @Test
        fun `Given a list with an array of annotations on its type`() {
            val code =
                """
                val fooBar: List<@[Foo Bar] String> = emptyList()
                """.trimIndent()
            annotationRuleAssertThat(code)
                .hasNoLintViolations()
        }
    }

    @Test
    fun `Given property allow trailing comma on declaration site is not set then remove trailing commas`() {
        val code =
            """
            enum class Foo {
                A,
                @Bar1 B,
                @Bar1 @Bar2 C,
                @Bar3("bar3") @Bar1 D
            }
            """.trimIndent()
        val formattedCode =
            """
            enum class Foo {
                A,
                @Bar1 B,
                @Bar1 @Bar2
                C,
                @Bar3("bar3")
                @Bar1
                D
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 17, "Multiple annotations should not be placed on the same line as the annotated construct"),
                LintViolation(5, 5, "Annotation with parameter(s) should be placed on a separate line prior to the annotated construct"),
                LintViolation(5, 25, "Multiple annotations should not be placed on the same line as the annotated construct"),
            ).isFormattedAs(formattedCode)
    }
}
