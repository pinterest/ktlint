package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AnnotationRuleTest {

    @Test
    fun `lint single annotation may be placed on line before annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                @FunctionalInterface class A {
                    @JvmField
                    var x: String
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint single annotation with parameters ends with a comment`() {
        assertThat(
            AnnotationRule().lint(
                """
                @Suppress("AnnotationRule") // this is a comment
                class A
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint single annotation with parameters on the same line with a comment`() {
        assertThat(
            AnnotationRule().lint(
                """
                @Suppress("AnnotationRule") /* this is a comment */ class A
                """.trimIndent()
            )
        ).hasSize(1)
    }

    @Test
    fun `lint single annotation with parameters on the same line with a comment and many spaces`() {
        assertThat(
            AnnotationRule().lint(
                """
                @Suppress("AnnotationRule") /* this is a comment */    class A
                """.trimIndent()
            )
        ).hasSize(1)
    }

    @Test
    fun `format single annotation may be placed on line before annotated construct`() {
        val code =
            """
            @FunctionalInterface class A {
                @JvmField
                var x: String
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint single annotation may be placed on same line as annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                @FunctionalInterface class A {
                    @JvmField var x: String

                    @Test fun myTest() {}
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format single annotation may be placed on same line as annotated construct`() {
        val code =
            """
            @FunctionalInterface class A {
                @JvmField var x: String

                @Test fun myTest() {}
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint multiple annotations should not be placed on same line as annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @JvmField @Volatile var x: String

                    @JvmField @Volatile
                    var y: String
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2,
                5,
                "annotation",
                AnnotationRule.multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage
            )
        )
    }

    @Test
    fun `format multiple annotations should not be placed on same line as annotated construct`() {
        assertThat(
            AnnotationRule().format(
                """
                class A {
                    @JvmField @Volatile var x: String

                    @JvmField @Volatile
                    var y: String
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A {
                @JvmField @Volatile
                var x: String

                @JvmField @Volatile
                var y: String
            }
            """.trimIndent()
        )
    }

    @Test
    fun `format multiple annotations should not be placed on same line as annotated construct (with no previous whitespace)`() {
        assertThat(AnnotationRule().format("@JvmField @Volatile var x: String"))
            .isEqualTo(
                """
                @JvmField @Volatile
                var x: String
                """.trimIndent()
            )
    }

    @Test
    fun `format multiple annotations should not be placed on same line as annotated construct (with no previous indent)`() {
        assertThat(
            AnnotationRule().format(
                """

                @JvmField @Volatile var x: String
                """.trimIndent()
            )
        ).isEqualTo(
            """

            @JvmField @Volatile
            var x: String
            """.trimIndent()
        )
    }

    @Test
    fun `lint spacing after annotations`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @SomeAnnotation("value")val x: String
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2,
                28,
                "annotation",
                "Missing spacing after @SomeAnnotation(\"value\")"
            )
        )
    }

    @Test
    fun `lint annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @JvmName("xJava") var x: String

                    @JvmName("yJava")
                    var y: String
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2,
                5,
                "annotation",
                AnnotationRule.annotationsWithParametersAreNotOnSeparateLinesErrorMessage
            )
        )
    }

    @Test
    fun `format annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().format(
                """
                class A {
                    @JvmName("xJava") var x: String

                    @JvmName("yJava")
                    var y: String
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            class A {
                @JvmName("xJava")
                var x: String

                @JvmName("yJava")
                var y: String
            }
            """.trimIndent()
        )
    }

    @Test
    fun `lint multiple annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().lint(
                """
                @Retention(SOURCE) @Target(FUNCTION, PROPERTY_SETTER, FIELD) annotation class A

                @Retention(SOURCE)
                @Target(FUNCTION, PROPERTY_SETTER, FIELD)
                annotation class B
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                1,
                1,
                "annotation",
                AnnotationRule.multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage
            ),
            LintError(
                1,
                1,
                "annotation",
                AnnotationRule.annotationsWithParametersAreNotOnSeparateLinesErrorMessage
            )
        )
    }

    @Test
    fun `format multiple annotations with params should not be placed on same line before annotated construct`() {
        assertThat(
            AnnotationRule().format(
                """
                @Retention(SOURCE) @Target(FUNCTION, PROPERTY_SETTER, FIELD) annotation class A

                @Retention(SOURCE)
                @Target(FUNCTION, PROPERTY_SETTER, FIELD)
                annotation class B
                """.trimIndent()
            )
        ).isEqualTo(
            """
            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY_SETTER, FIELD)
            annotation class A

            @Retention(SOURCE)
            @Target(FUNCTION, PROPERTY_SETTER, FIELD)
            annotation class B
            """.trimIndent()
        )
    }

    @Test
    fun `lint annotation after keyword`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    private @Test fun myTest() {}
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format annotation after keyword`() {
        val code =
            """
            class A {
                private @Test fun myTest() {}
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint multi-line annotation`() {
        assertThat(
            AnnotationRule().lint(
                """
                class A {
                    @JvmField @Volatile @Annotation(
                        enabled = true,
                        groups = [
                            "a",
                            "b",
                            "c"
                        ]
                    ) val a: Any
                }
                """.trimIndent()
            )
        ).containsExactly(
            LintError(
                2,
                5,
                "annotation",
                AnnotationRule.multipleAnnotationsOnSameLineAsAnnotatedConstructErrorMessage
            ),
            LintError(
                2,
                5,
                "annotation",
                AnnotationRule.annotationsWithParametersAreNotOnSeparateLinesErrorMessage
            )
        )
    }

    @Test
    fun `format multi-line annotation`() {
        val code =
            """
            class A {
                @JvmField @Volatile @Annotation(
                    enabled = true,
                    groups = [
                        "a",
                        "b",
                        "c"
                    ]
                ) val a: Any
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(
            """
            class A {
                @JvmField
                @Volatile
                @Annotation(
                    enabled = true,
                    groups = [
                        "a",
                        "b",
                        "c"
                    ]
                )
                val a: Any
            }
            """.trimIndent()
        )
    }

    @Test
    fun `no annotation present for data class passes`() {
        val code =
            """
            package com.example.application.a.b

            data class FileModel(val uri: String, val name: String)
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `no annotation present for function override passes`() {
        val code =
            """
            package com.example.application.a.b

            override fun foo()
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `no annotation present succeeds for class`() {
        val code =
            """
            package com.example.application.a

            import android.os.Environment

            class PathProvider {
                fun gallery(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
            }
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `multiple newlines preceding annotation`() {
        val code =
            """
            fun foo() {




                @Subscribe(threadMode = ThreadMode.MAIN) fun onEventMainThread(e: ModalContainer.ShowEvent) {
                    modalContainer?.show(e)
                }
            }
            """.trimIndent()
        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            fun foo() {




                @Subscribe(threadMode = ThreadMode.MAIN)
                fun onEventMainThread(e: ModalContainer.ShowEvent) {
                    modalContainer?.show(e)
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `no error with formatting annotation for primary constructor`() {
        val code =
            """
            class Foo @Inject internal constructor()
            """.trimIndent()
        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            class Foo @Inject internal constructor()
            """.trimIndent()
        )
    }

    @Test
    fun `lint annotations on method parameters may be placed on same line`() {
        val code =
            """
            interface FooService {

                fun foo1(
                    @Path("fooId") fooId: String,
                    @Path("bar") bar: String,
                    @Body body: Foo
                ): Completable

                fun foo2(@Query("include") include: String? = null, @QueryMap fields: Map<String, String> = emptyMap()): Single

                fun foo3(@Path("fooId") fooId: String): Completable
            }
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format annotations on method parameters may be placed on same line`() {
        val code =
            """
            interface FooService {

                fun foo1(
                    @Path("fooId") fooId: String,
                    @Path("bar") bar: String,
                    @Body body: Foo
                ): Completable

                fun foo2(@Query("include") include: String? = null, @QueryMap fields: Map<String, String> = emptyMap()): Single

                fun foo3(@Path("fooId") fooId: String): Completable
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint annotations on constructor parameters may be placed on same line`() {
        val code =
            """
            class Foo(@Path("fooId") val fooId: String)
            class Bar(
                @NotNull("fooId") val fooId: String,
                @NotNull("bar") bar: String
            )
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format annotations on constructor parameters may be placed on same line`() {
        val code =
            """
            class Foo(@Path("fooId") val fooId: String)
            class Bar(
                @NotNull("fooId") val fooId: String,
                @NotNull("bar") bar: String
            )
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint annotations on arguments may be placed on same line`() {
        val code =
            """
            fun doSomething() {
                actuallyDoSomething(
                    @ExpressionStringAnn("foo") "test",
                    @ExpressionIntAnn("bar") 42
                )
            }
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format annotations on arguments may be placed on same line`() {
        val code =
            """
            fun doSomething() {
                actuallyDoSomething(
                    @ExpressionStringAnn("foo") "test",
                    @ExpressionIntAnn("bar") 42
                )
            }
            """.trimIndent()
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint annotations on type arguments may be placed on same line`() {
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
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format annotations on type arguments may be placed on same line`() {
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
        assertThat(AnnotationRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `annotation at top of file`() {
        val code =
            """
            @file:JvmName("FooClass") package foo.bar
            """.trimIndent()
        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @file:JvmName("FooClass")

            package foo.bar
            """.trimIndent()
        )
    }

    @Test
    fun `lint multiple annotations ends with a comment`() {
        val code =
            """
            annotation class A
            annotation class B

            @A
            @B // comment
            fun test() {
            }
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `lint multiple annotations ends with a comment 2`() {
        val code =
            """
            annotation class A
            annotation class B

            @A // comment
            @B
            fun test() {
            }
            """.trimIndent()
        assertThat(AnnotationRule().lint(code)).isEmpty()
    }

    @Test
    fun `format file annotations should be separated with a blank line 1`() {
        assertThat(
            AnnotationRule().format(
                """
                @file:JvmName package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            """
            @file:JvmName

            package foo.bar

            """.trimIndent()
        )
    }

    @Test
    fun `format file annotations should be separated with a blank line 2`() {
        assertThat(
            AnnotationRule().format(
                """
                /*
                 * Copyright 2000-2020 XXX
                 */

                @file:JvmName
                package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            """
            /*
             * Copyright 2000-2020 XXX
             */

            @file:JvmName

            package foo.bar

            """.trimIndent()
        )
    }

    @Test
    fun `format file annotations should be separated with a blank line 3`() {
        assertThat(
            AnnotationRule().format(
                """
                @file:JvmName
                fun foo() {}

                """.trimIndent()
            )
        ).isEqualTo(
            """
            @file:JvmName

            fun foo() {}

            """.trimIndent()
        )
    }

    @Test
    fun `format file annotations should be separated with a blank line 4`() {
        assertThat(
            AnnotationRule().format(
                """
                @file:JvmName // comment
                package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            """
            @file:JvmName // comment

            package foo.bar

            """.trimIndent()
        )
    }

    @Test
    fun `format file annotations should be separated with a blank line 5`() {
        assertThat(
            AnnotationRule().format(
                """
                @file:JvmName /* comment */ package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            """
            @file:JvmName /* comment */

            package foo.bar

            """.trimIndent()
        )
    }

    @Test
    fun `format file annotations should be separated with a blank line 6`() {
        assertThat(
            AnnotationRule().format(
                """
                @file:JvmName
                // comment
                package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            """
            @file:JvmName

            // comment
            package foo.bar

            """.trimIndent()
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 1`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 13, "annotation", AnnotationRule.fileAnnotationsShouldBeSeparated)
            )
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 2`() {
        assertThat(
            AnnotationRule().lint(
                """
                /*
                 * Copyright 2000-2020 XXX
                 */

                @file:JvmName
                package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(5, 13, "annotation", AnnotationRule.fileAnnotationsShouldBeSeparated)
            )
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 3`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName
                fun foo() {}

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 13, "annotation", AnnotationRule.fileAnnotationsShouldBeSeparated)
            )
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 4`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName // comment
                package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 13, "annotation", AnnotationRule.fileAnnotationsShouldBeSeparated)
            )
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 5`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName /* comment */ package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 13, "annotation", AnnotationRule.fileAnnotationsShouldBeSeparated)
            )
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 6`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName
                // comment
                package foo.bar

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 13, "annotation", AnnotationRule.fileAnnotationsShouldBeSeparated)
            )
        )
    }

    @Test
    fun `lint file annotations should be separated with a blank line 7`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint file annotations should be separated with a blank line 8`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName

                package foo.bar

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint file annotations should be separated with a blank line 9`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName


                package foo.bar

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint file annotations should be separated with a blank line 10`() {
        assertThat(
            AnnotationRule().lint(
                """
                @file:JvmName

                fun foo() {}

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint no empty lines between an annotation and object`() {
        assertThat(
            AnnotationRule().lint(
                """
                @JvmField
                fun foo() {}

                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `lint there should not be empty lines between an annotation and object`() {
        assertThat(
            AnnotationRule().lint(
                """
                @JvmField

                fun foo() {}

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 9, "annotation", AnnotationRule.fileAnnotationsLineBreaks)
            )
        )
    }

    @Test
    fun `lint there should not be empty lines between an annotation and object autocorrected`() {
        val code =
            """
            @JvmField

            fun foo() {}

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            fun foo() {}

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between an annotation and object autocorrected with control`() {
        val code =
            """
            @JvmField

            fun foo() {
              @JvmStatic
              val r = A()
            }

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            fun foo() {
              @JvmStatic
              val r = A()
            }

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between an annotation and object autocorrected multiple lines`() {
        val code =
            """
            @JvmField



            fun foo() {}

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            fun foo() {}

            """.trimIndent()
        )
    }

    @Test
    fun `lint annotation on the same line remains there`() {
        val code =
            """
            @JvmField fun foo() {}

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField fun foo() {}

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between multiple annotations`() {
        val code =
            """
            @JvmField @JvmStatic

            fun foo() = Unit

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField @JvmStatic
            fun foo() = Unit

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between multiple annotations on multiple lines`() {
        val code =
            """
            @JvmField
            @JvmStatic

            fun foo() = Unit

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            @JvmStatic
            fun foo() = Unit

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between multiple annotations with inline annotation`() {
        val code =
            """
            @JvmField

            @JvmName

            @JvmStatic fun foo() = Unit

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            @JvmName
            @JvmStatic
            fun foo() = Unit

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between two or more annotations`() {
        val code =
            """
            @JvmField

            @JvmName


            @JvmStatic


            fun foo() = Unit

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            @JvmName
            @JvmStatic
            fun foo() = Unit

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be empty lines between an annotation and object autocorrected multiple annotations`() {
        val code =
            """
            @JvmField



            fun foo() {
              @JvmStatic

              val foo = Foo()
            }

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            fun foo() {
              @JvmStatic
              val foo = Foo()
            }

            """.trimIndent()
        )
    }

    @Test
    fun `lint there should not be an error on multiple lines assertion while additional formatting ongoing`() {
        val code =
            """
            package a.b.c

            class Test {
                fun bloop() {
                    asdfadsf(asdfadsf, asdfasdf, asdfasdfasdfads,
                    asdfasdf, asdfasdf, asdfasdf)
                }

                @Blah
                val test: Int
            }

            """.trimIndent()

        assertThat(
            AnnotationRule().format(code)
        ).isEqualTo(
            """
            package a.b.c

            class Test {
                fun bloop() {
                    asdfadsf(asdfadsf, asdfasdf, asdfasdfasdfads,
                    asdfasdf, asdfasdf, asdfasdf)
                }

                @Blah
                val test: Int
            }

            """.trimIndent()
        )
    }
}
