package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacingBetweenDeclarationsWithAnnotationsRuleTest {
    @Test
    fun `annotation at top of file should do nothing`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                @Foo
                fun a()
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `multiple annotations should do nothing`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                @Foo
                @Bar
                fun a()
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space after comment should do nothing`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                // hello
                @Foo
                fun a()
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space before declaration with annotation should cause error`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                fun a()
                @Foo
                fun b()
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(
                    2,
                    1,
                    "spacing-between-declarations-with-annotations",
                    "Declarations and declarations with annotations should have an empty space between."
                )
            )
        )
    }

    @Test
    fun `missing space before declaration with multiple annotations should cause error`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                fun a()
                @Foo
                @Bar
                fun b()
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(
                    2,
                    1,
                    "spacing-between-declarations-with-annotations",
                    "Declarations and declarations with annotations should have an empty space between."
                )
            )
        )
    }

    @Test
    fun `autoformat should work correctly`() {
        assertEquals(
            """
            @Annotation1
            fun one() = 1

            @Annotation1
            @Annotation2
            fun two() = 2
            fun three() = 42

            @Annotation1
            fun four() = 44
            """.trimIndent(),
            SpacingBetweenDeclarationsWithAnnotationsRule().format(
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
            )
        )
    }

    @Test
    fun `missing space before primary constructor`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                annotation class E

                private class A
                @E
                constructor(a: Int)
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space before function parameter`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                annotation class E

                fun foo(
                    a: String,
                    @E
                    b: String
                ) = 1
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space before member function`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                annotation class E

                class C {
                    fun foo() = 1
                    @E
                    fun bar() = 2
                }
                """.trimIndent()
            )
        ).hasSize(1)
    }

    @Test
    fun `format missing space before member function`() {
        assertEquals(
            """
            annotation class E

            class C {
                fun foo() = 1

                @E
                fun bar() = 2
            }
            """.trimIndent(),
            SpacingBetweenDeclarationsWithAnnotationsRule().format(
                """
                annotation class E

                class C {
                    fun foo() = 1
                    @E
                    fun bar() = 2
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `two new lines before member function`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                annotation class E
                class C {
                    fun foo() = 1


                    @E
                    fun bar() = 2
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space after comment with previous member function should do nothing`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                annotation class E
                class C {
                    fun foo() = 1

                    // Hello
                    @E
                    fun bar() = 2
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `missing space after comment with previous variable should do nothing`() {
        Assertions.assertThat(
            SpacingBetweenDeclarationsWithAnnotationsRule().lint(
                """
                fun foo() {

                    val a = 1

                    // hello
                    @Foo
                    val b = 2
                }
                """.trimIndent()
            )
        ).isEmpty()
    }
}
