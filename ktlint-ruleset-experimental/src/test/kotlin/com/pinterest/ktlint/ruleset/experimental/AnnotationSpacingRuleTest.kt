package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AnnotationSpacingRuleTest {

    @Test
    fun `lint no empty lines between an annotation and object`() {
        assertThat(
            AnnotationSpacingRule().lint(
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
            AnnotationSpacingRule().lint(
                """
                @JvmField

                fun foo() {}

                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 9, "annotation-spacing", AnnotationSpacingRule.ERROR_MESSAGE)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
        ).isEqualTo(
            """
            @JvmField
            @JvmName
            @JvmStatic fun foo() = Unit

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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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
            AnnotationSpacingRule().format(code)
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

    @Test
    fun `lint there should not be an error on multiline assertion while additional formatting ongoing from file`() {
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
            ArrayList<LintError>().apply {
                KtLint.lint(
                    KtLint.Params(
                        text = code,
                        ruleSets = mutableListOf(
                            ExperimentalRuleSetProvider().get()
                        ),
                        cb = { e, _ -> add(e) }
                    )
                )
            }
        ).allMatch {
            it.ruleId == "experimental:argument-list-wrapping"
        }
    }

    @Test
    fun `annotations should not be separated by comments from the annotated construct`() {
        val code =
            """
                @Suppress("DEPRECATION") @Hello
                /**
                 * block comment
                 */
                class Foo {
                }
            """.trimIndent()
        assertThat(
            AnnotationSpacingRule().lint(code)
        ).isEqualTo(
            listOf(
                LintError(1, 31, "annotation-spacing", AnnotationSpacingRule.ERROR_MESSAGE)
            )
        )
    }

    @Test
    fun `annotations should be moved after comments`() {
        val code =
            """
                @Suppress("DEPRECATION") @Hello
                /**
                 * block comment
                 */
                class Foo {
                }
            """.trimIndent()
        assertThat(
            AnnotationSpacingRule().format(code)
        ).isEqualTo(
            """
                /**
                 * block comment
                 */
                @Suppress("DEPRECATION") @Hello
                class Foo {
                }
            """.trimIndent()
        )

        val codeEOL =
            """
                @Suppress("DEPRECATION") @Hello
                // hello
                class Foo {
                }
            """.trimIndent()
        assertThat(
            AnnotationSpacingRule().format(codeEOL)
        ).isEqualTo(
            """
                // hello
                @Suppress("DEPRECATION") @Hello
                class Foo {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `preceding whitespaces are preserved`() {
        val code =
            """
                package a.b.c

                val hello = 5


                @Suppress("DEPRECATION") @Hello
                /**
                 * block comment
                 */
                class Foo {
                }
            """.trimIndent()
        assertThat(
            AnnotationSpacingRule().format(code)
        ).isEqualTo(
            """
                package a.b.c

                val hello = 5


                /**
                 * block comment
                 */
                @Suppress("DEPRECATION") @Hello
                class Foo {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `lint eol comment on the same line as the annotation`() {
        assertThat(
            AnnotationSpacingRule().lint(
                """
                @SuppressWarnings // foo
                fun bar() {
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format eol comment on the same line as the annotation`() {
        val code =
            """
                @SuppressWarnings // foo

                fun bar() {
                }
            """.trimIndent()
        assertThat(
            AnnotationSpacingRule().format(code)
        ).isEqualTo(
            """
                @SuppressWarnings // foo
                fun bar() {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `format eol comment on the same line as the annotation 2`() {
        val code =
            """
                @SuppressWarnings // foo
                // bar
                fun bar() {
                }
            """.trimIndent()
        assertThat(
            AnnotationSpacingRule().format(code)
        ).isEqualTo(
            """
                // bar
                @SuppressWarnings // foo
                fun bar() {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `lint block comment on the same line as the annotation`() {
        assertThat(
            AnnotationSpacingRule().lint(
                """
                @SuppressWarnings /* foo */
                fun bar() {
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
