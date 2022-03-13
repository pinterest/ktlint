package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpacingBetweenDeclarationsWithAnnotationsRuleTest {
    @Test
    fun `annotation at top of file should do nothing`() {
        val code =
            """
            @Foo
            fun a()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `multiple annotations should do nothing`() {
        val code =
            """
            @Foo
            @Bar
            fun a()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `missing space after comment should do nothing`() {
        val code =
            """
            // hello
            @Foo
            fun a()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `missing space before declaration with annotation should cause error`() {
        val code =
            """
            fun a()
            @Foo
            fun b()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).containsExactly(
            LintError(
                2,
                1,
                "spacing-between-declarations-with-annotations",
                "Declarations and declarations with annotations should have an empty space between."
            )
        )
    }

    @Test
    fun `missing space before declaration with multiple annotations should cause error`() {
        val code =
            """
            fun a()
            @Foo
            @Bar
            fun b()
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).containsExactly(
            LintError(2, 1, "spacing-between-declarations-with-annotations", "Declarations and declarations with annotations should have an empty space between.")
        )
    }

    @Test
    fun `autoformat should work correctly`() {
        val code =
            """
            @Annotation1
            fun one() = 1
            @Annotation1
            @Annotation2
            fun two() = 2
            fun three() = 42
            @Annotation1
            fun four() = 44
            """.trimIndent()
        val formattedCode =
            """
            @Annotation1
            fun one() = 1

            @Annotation1
            @Annotation2
            fun two() = 2
            fun three() = 42

            @Annotation1
            fun four() = 44
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `missing space before primary constructor`() {
        val code =
            """
            annotation class E

            private class A
            @E
            constructor(a: Int)
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `missing space before function parameter`() {
        val code =
            """
            annotation class E

            fun foo(
                a: String,
                @E
                b: String
            ) = 1
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `missing space before member function`() {
        val code =
            """
            annotation class E

            class C {
                fun foo() = 1
                @E
                fun bar() = 2
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).hasSize(1)
    }

    @Test
    fun `format missing space before member function`() {
        val code =
            """
            annotation class E

            class C {
                fun foo() = 1
                @E
                fun bar() = 2
            }
            """.trimIndent()
        val formattedCode =
            """
            annotation class E

            class C {
                fun foo() = 1

                @E
                fun bar() = 2
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().format(code)
        ).isEqualTo(formattedCode)
    }

    @Test
    fun `two new lines before member function`() {
        val code =
            """
            annotation class E
            class C {
                fun foo() = 1


                @E
                fun bar() = 2
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `missing space after comment with previous member function should do nothing`() {
        val code =
            """
            annotation class E
            class C {
                fun foo() = 1

                // Hello
                @E
                fun bar() = 2
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `No blank line is required between comment and an annotated declaration`() {
        val code =
            """
            fun foo() {

                val a = 1

                // hello
                @Foo
                val b = 2
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }

    @Test
    fun `Issue 1281 - No blank line is required between comment and an annotated declaration when previous declaration end with a comment`() {
        val code =
            """
            class KotlinPluginTest {
                // tag::setUp[]
                @BeforeEach
                fun setUp() {
                }
                // end::setUp[]

                // tag::testQuery[]
                @Test
                    fun testFindById() {
                }
                // end::testQuery[]
            }
            """.trimIndent()
        assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(code)
        ).isEmpty()
    }
}
