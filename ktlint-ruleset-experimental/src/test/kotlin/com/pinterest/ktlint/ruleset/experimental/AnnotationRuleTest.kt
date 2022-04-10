package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnnotationRuleTest {
    private val annotationRuleAssertThat = AnnotationRule().assertThat()

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
                LintViolation(1, 1, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(2, 5, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(3, 5, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(5, 1, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(6, 5, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(7, 5, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(9, 1, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(10, 5, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                LintViolation(11, 5, "Annotations with parameters should all be placed on separate lines prior to the annotated construct")
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
                LintViolation(1, 1, "Multiple annotations should not be placed on the same line as the annotated construct"),
                LintViolation(2, 5, "Multiple annotations should not be placed on the same line as the annotated construct"),
                LintViolation(3, 5, "Multiple annotations should not be placed on the same line as the annotated construct")
            ).isFormattedAs(formattedCode)
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
            .hasLintViolation(
                1,
                1,
                "Multiple annotations should not be placed on the same line as the annotated construct"
            )
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
            .hasLintViolation(
                2,
                1,
                "Multiple annotations should not be placed on the same line as the annotated construct"
            )
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
            @Suppress("AnnotationRule") class FooBar {
                @Suppress("AnnotationRule") var foo: String
                @Suppress("AnnotationRule") fun bar() {}
            }
            """.trimIndent()
        annotationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, "Missing spacing after @Suppress(\"AnnotationRule\")"),
                LintViolation(2, 31, "Missing spacing after @Suppress(\"AnnotationRule\")"),
                LintViolation(3, 31, "Missing spacing after @Suppress(\"AnnotationRule\")")
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
            // TODO: Offset and message is not entirely clear
            .hasLintViolation(
                2,
                5,
                "Annotations with parameters should all be placed on separate lines prior to the annotated construct"
            )
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
    fun `Issue 642 - Given annotations on type arguments on same line as argument`() {
        val code =
            """
            val aProperty: Map<@Ann("test") Int, @JvmSuppressWildcards(true) (String) -> Int?>
            val bProperty: Map<
                @Ann String,
                @Ann("test") Int,
                @JvmSuppressWildcards(true) (String) -> Int?
                >

            fun doSomething() {
                funWithGenericsCall<@JvmSuppressWildcards(true) Int>()
            }
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
    inner class FileAnnotation {
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
                .hasLintViolation(1, 13, "File annotations should be separated from file contents with a blank line")
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
            annotationRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 1, "Annotations with parameters should all be placed on separate lines prior to the annotated construct"),
                    LintViolation(1, 25, "File annotations should be separated from file contents with a blank line")
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
                .hasLintViolation(1, 13, "File annotations should be separated from file contents with a blank line")
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
                .hasLintViolation(1, 13, "File annotations should be separated from file contents with a blank line")
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
                .hasLintViolation(1, 13, "File annotations should be separated from file contents with a blank line")
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
                .hasLintViolation(1, 34, "File annotations should be separated from file contents with a blank line")
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
}
