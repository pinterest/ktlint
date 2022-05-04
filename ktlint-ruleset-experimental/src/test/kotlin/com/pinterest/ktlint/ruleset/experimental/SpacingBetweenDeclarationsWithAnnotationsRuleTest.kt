package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class SpacingBetweenDeclarationsWithAnnotationsRuleTest {
    private val spacingBetweenDeclarationsWithAnnotationsRuleAssertThat = SpacingBetweenDeclarationsWithAnnotationsRule().assertThat()

    @Test
    fun `Given an annotation at top of file should do nothing`() {
        val code =
            """
            @Foo
            fun a()
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given multiple annotations should do nothing`() {
        val code =
            """
            @Foo
            @Bar
            fun a()
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a comment on line above annotation should do nothing`() {
        val code =
            """
            // some comment
            @Foo
            fun a()
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given no blank line before declaration with annotation`() {
        val code =
            """
            fun a()
            @Foo
            fun b()
            """.trimIndent()
        val formattedCode =
            """
            fun a()

            @Foo
            fun b()
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code)
            .hasLintViolation(2, 1, "Declarations and declarations with annotations should have an empty space between.")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given no blank line before declaration with multiple annotations`() {
        val code =
            """
            fun a()
            @Foo
            @Bar
            fun b()
            """.trimIndent()
        val formattedCode =
            """
            fun a()

            @Foo
            @Bar
            fun b()
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code)
            .hasLintViolation(2, 1, "Declarations and declarations with annotations should have an empty space between.")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 971 - Given an annotated primary class constructor without blank line above the annotation`() {
        val code =
            """
            annotation class E

            private class A
            @E
            constructor(a: Int)
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 971 - Given an annotated function parameter without blank line above`() {
        val code =
            """
            annotation class E

            fun foo(
                a: String,
                @E
                b: String
            ) = 1
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 971 - missing space before member function`() {
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
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code)
            .hasLintViolation(5, 5, "Declarations and declarations with annotations should have an empty space between.")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotated function preceded by multiple blank lines`() {
        val code =
            """
            annotation class E
            class C {
                fun foo() = 1


                @E
                fun bar() = 2
            }
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1126 - Given a comment on line above annotated class member should do nothing`() {
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
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1126 - Given a comment on line above annotated variable in function should do nothing`() {
        val code =
            """
            fun foo() {
                val a = 1

                // hello
                @Foo
                val b = 2
            }
            """.trimIndent()
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1281 - No blank line is required between comment and an annotated declaration when previous declaration ends with a comment`() {
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
        spacingBetweenDeclarationsWithAnnotationsRuleAssertThat(code).hasNoLintViolations()
    }
}
