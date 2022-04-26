package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule.Companion.allowTrailingCommaOnCallSiteProperty
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule.Companion.allowTrailingCommaProperty
import com.pinterest.ktlint.ruleset.standard.NoUnusedImportsRule
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class TrailingCommaRuleTest {
    private val trailingCommaRuleAssertThat = TrailingCommaRule().assertThat()

    @Test
    fun `Given property allow trailing comma on call site is not set then remove trailing comma's`() {
        val code =
            """
            val foo1 = listOf("a", "b",)

            val foo2 = Pair(1, 2,)

            val foo3: List<String,> = emptyList()

            val foo4 = Array(2) { 42 }
            val bar4 = foo4[1,]

            annotation class Foo5(val params: IntArray)
            @Foo5([1, 2,])
            val foo5: Int = 0
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = listOf("a", "b")

            val foo2 = Pair(1, 2)

            val foo3: List<String> = emptyList()

            val foo4 = Array(2) { 42 }
            val bar4 = foo4[1]

            annotation class Foo5(val params: IntArray)
            @Foo5([1, 2])
            val foo5: Int = 0
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, "Unnecessary trailing comma before \")\""),
                LintViolation(3, 21, "Unnecessary trailing comma before \")\""),
                LintViolation(5, 22, "Unnecessary trailing comma before \">\""),
                LintViolation(8, 18, "Unnecessary trailing comma before \"]\""),
                LintViolation(11, 12, "Unnecessary trailing comma before \"]\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given property allow trailing comma on declaration site is not set then remove trailing comma's`() {
        val code =
            """
            data class Foo1(val bar: Int,)

            class Foo2<A, B,> {}

            fun foo3(bar: Int): String = when(bar) {
                1, 2, -> "a"
                else -> "b"
            }

            fun foo4() {
                fun bar(): Pair<Int, Int> = Pair(1, 2)

                val (x, y,) = bar()
            }

            val foo5: (Int, Int,) -> Int = 42

            val foo6: (Int, Int,) -> Int = { foo, bar, -> foo * bar }
            """.trimIndent()
        val formattedCode =
            """
            data class Foo1(val bar: Int)

            class Foo2<A, B> {}

            fun foo3(bar: Int): String = when(bar) {
                1, 2 -> "a"
                else -> "b"
            }

            fun foo4() {
                fun bar(): Pair<Int, Int> = Pair(1, 2)

                val (x, y) = bar()
            }

            val foo5: (Int, Int) -> Int = 42

            val foo6: (Int, Int) -> Int = { foo, bar -> foo * bar }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 29, "Unnecessary trailing comma before \")\""),
                LintViolation(3, 16, "Unnecessary trailing comma before \">\""),
                LintViolation(6, 9, "Unnecessary trailing comma before \"->\""),
                LintViolation(13, 14, "Unnecessary trailing comma before \")\""),
                LintViolation(16, 20, "Unnecessary trailing comma before \")\""),
                LintViolation(18, 20, "Unnecessary trailing comma before \")\""),
                LintViolation(18, 42, "Unnecessary trailing comma before \"->\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that properties to force trailing comma's on call and declaration site have been enabled`() {
        val code =
            """
            fun test(
                x: Int,
                y: Int,
                block: (Int, Int) -> Int
            ): (
                Int, Int
            ) -> Int = when (x) {
                1, 2
                -> {
                    foo,
                    bar /* The comma should be inserted before the comment */
                    ->
                    block(
                        foo * bar,
                        foo + bar
                    )
                }
                else -> { _, _ -> block(0, 0) }
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test(
                x: Int,
                y: Int,
                block: (Int, Int) -> Int,
            ): (
                Int, Int,
            ) -> Int = when (x) {
                1, 2,
                -> {
                    foo,
                    bar, /* The comma should be inserted before the comment */
                    ->
                    block(
                        foo * bar,
                        foo + bar,
                    )
                }
                else -> { _, _ -> block(0, 0) }
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(4, 29, "Missing trailing comma before \")\""),
                LintViolation(6, 13, "Missing trailing comma before \")\""),
                LintViolation(8, 9, "Missing trailing comma before \"->\""),
                LintViolation(11, 12, "Missing trailing comma before \"->\""),
                LintViolation(15, 22, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from an argument list when present`() {
        val code =
            """
            val list1 = listOf("a", "b",)
            val list2 = listOf(
                "a",
                "b", // The comma before the comment should be removed without removing the comment itself
            )
            val list3 = listOf(
                "a",
                "b", /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        val formattedCode =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b" // The comma before the comment should be removed without removing the comment itself
            )
            val list3 = listOf(
                "a",
                "b" /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
            .hasLintViolations(
                LintViolation(1, 28, "Unnecessary trailing comma before \")\""),
                LintViolation(4, 8, "Unnecessary trailing comma before \")\""),
                LintViolation(8, 8, "Unnecessary trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the argument list when missing`() {
        val code =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b" // The comma should be inserted before the comment
            )
            val list3 = listOf(
                "a",
                "b" /* The comma should be inserted before the comment */
            )
            """.trimIndent()
        val formattedCode =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b", // The comma should be inserted before the comment
            )
            val list3 = listOf(
                "a",
                "b", /* The comma should be inserted before the comment */
            )
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(4, 8, "Missing trailing comma before \")\""),
                LintViolation(8, 8, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on declaration site then remove it from an parameter list when present`() {
        val code =
            """
            data class Foo1(val bar: Int,)
            data class Foo2(
               val bar: Int, // The comma before the comment should be removed without removing the comment itself
            )
            data class Foo3(
               val bar: Int, /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        val formattedCode =
            """
            data class Foo1(val bar: Int)
            data class Foo2(
               val bar: Int // The comma before the comment should be removed without removing the comment itself
            )
            data class Foo3(
               val bar: Int /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to false)
            .hasLintViolations(
                LintViolation(1, 29, "Unnecessary trailing comma before \")\""),
                LintViolation(3, 16, "Unnecessary trailing comma before \")\""),
                LintViolation(6, 16, "Unnecessary trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on declaration site then add it to the parameter list when missing`() {
        val code =
            """
            data class Foo1(val bar: Int)
            data class Foo2(
               val bar: Int // The comma should be inserted before the comment
            )
            data class Foo3(
               val bar: Int /* The comma should be inserted before the comment */
            )
            """.trimIndent()
        val formattedCode =
            """
            data class Foo1(val bar: Int)
            data class Foo2(
               val bar: Int, // The comma should be inserted before the comment
            )
            data class Foo3(
               val bar: Int, /* The comma should be inserted before the comment */
            )
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(3, 16, "Missing trailing comma before \")\""),
                LintViolation(6, 16, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on declaration site then remove it from an type parameter list when present`() {
        val code =
            """
            class Foo1<A, B,> {}
            class Foo2<
                A,
                B, // The comma before the comment should be removed without removing the comment itself
            > {}
            class Foo3<
                A,
                B, /* The comma before the comment should be removed without removing the comment itself */
            > {}
            """.trimIndent()
        val formattedCode =
            """
            class Foo1<A, B> {}
            class Foo2<
                A,
                B // The comma before the comment should be removed without removing the comment itself
            > {}
            class Foo3<
                A,
                B /* The comma before the comment should be removed without removing the comment itself */
            > {}
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to false)
            .hasLintViolations(
                LintViolation(1, 16, "Unnecessary trailing comma before \">\""),
                LintViolation(4, 6, "Unnecessary trailing comma before \">\""),
                LintViolation(8, 6, "Unnecessary trailing comma before \">\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on declaration site then add it to the type parameter list when missing`() {
        val code =
            """
            class Foo1<A, B> {}
            class Foo2<
                A,
                B // The comma should be inserted before the comment
            > {}
            class Foo3<
                A,
                B /* The comma should be inserted before the comment */
            > {}
            """.trimIndent()
        val formattedCode =
            """
            class Foo1<A, B> {}
            class Foo2<
                A,
                B, // The comma should be inserted before the comment
            > {}
            class Foo3<
                A,
                B, /* The comma should be inserted before the comment */
            > {}
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(4, 6, "Missing trailing comma before \">\""),
                LintViolation(8, 6, "Missing trailing comma before \">\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on declaration site then remove it from when-condition when present`() {
        val code =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2, -> "a"
                3, 4, // The comma before the comment should be removed without removing the comment itself
                -> "a"
                5,
                6, /* The comma before the comment should be removed without removing the comment itself */
                -> "a"
                else -> "b"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2 -> "a"
                3, 4 // The comma before the comment should be removed without removing the comment itself
                -> "a"
                5,
                6 /* The comma before the comment should be removed without removing the comment itself */
                -> "a"
                else -> "b"
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to false)
            .hasLintViolations(
                LintViolation(2, 9, "Unnecessary trailing comma before \"->\""),
                LintViolation(3, 9, "Unnecessary trailing comma before \"->\""),
                LintViolation(6, 6, "Unnecessary trailing comma before \"->\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on declaration site then add it to the when-condition when missing`() {
        val code =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2 -> "a"
                3, 4 // The comma should be inserted before the comment
                -> "a"
                5,
                6 /* The comma should be inserted before the comment */
                -> "a"
                else -> "b"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2 -> "a"
                3, 4, // The comma should be inserted before the comment
                -> "a"
                5,
                6, /* The comma should be inserted before the comment */
                -> "a"
                else -> "b"
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(3, 9, "Missing trailing comma before \"->\""),
                LintViolation(6, 6, "Missing trailing comma before \"->\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on declaration site then remove it from the destructuring declaration when present`() {
        val code =
            """
            fun foo() {
                fun bar(): Pair<Int, Int> = Pair(1, 2)

                val (x, y,) = bar()
                val (
                    x,
                    y, // The comma before the comment should be removed without removing the comment itself
                ) = bar()
                val (
                    x,
                    y, /* The comma before the comment should be removed without removing the comment itself */
                ) = bar()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                fun bar(): Pair<Int, Int> = Pair(1, 2)

                val (x, y) = bar()
                val (
                    x,
                    y // The comma before the comment should be removed without removing the comment itself
                ) = bar()
                val (
                    x,
                    y /* The comma before the comment should be removed without removing the comment itself */
                ) = bar()
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to false)
            .hasLintViolations(
                LintViolation(4, 14, "Unnecessary trailing comma before \")\""),
                LintViolation(7, 10, "Unnecessary trailing comma before \")\""),
                LintViolation(11, 10, "Unnecessary trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on declaration site then add it to the destructuring declaration when missing`() {
        val code =
            """
            fun foo() {
                fun bar(): Pair<Int, Int> = Pair(1, 2)

                val (x, y) = bar()
                val (
                    x,
                    y // The comma should be inserted before the comment
                ) = bar()
                val (
                    x,
                    y /* The comma should be inserted before the comment */
                ) = bar()
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                fun bar(): Pair<Int, Int> = Pair(1, 2)

                val (x, y) = bar()
                val (
                    x,
                    y, // The comma should be inserted before the comment
                ) = bar()
                val (
                    x,
                    y, /* The comma should be inserted before the comment */
                ) = bar()
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(7, 10, "Missing trailing comma before \")\""),
                LintViolation(11, 10, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on declaration site then remove it from the function literal when present`() {
        val code =
            """
            val fooBar1: (Int, Int) -> Int = { foo, bar, -> foo * bar }
            val fooBar2: (Int, Int) -> Int = {
                foo,
                bar, // The comma before the comment should be removed without removing the comment itself
                -> foo * bar
            }
            val fooBar3: (Int, Int) -> Int = {
                foo,
                bar, /* The comma before the comment should be removed without removing the comment itself */
                -> foo * bar
            }
            """.trimIndent()
        val formattedCode =
            """
            val fooBar1: (Int, Int) -> Int = { foo, bar -> foo * bar }
            val fooBar2: (Int, Int) -> Int = {
                foo,
                bar // The comma before the comment should be removed without removing the comment itself
                -> foo * bar
            }
            val fooBar3: (Int, Int) -> Int = {
                foo,
                bar /* The comma before the comment should be removed without removing the comment itself */
                -> foo * bar
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to false)
            .hasLintViolations(
                LintViolation(1, 44, "Unnecessary trailing comma before \"->\""),
                LintViolation(4, 8, "Unnecessary trailing comma before \"->\""),
                LintViolation(9, 8, "Unnecessary trailing comma before \"->\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on declaration site then add it to function literal when missing`() {
        val code =
            """
            val fooBar1: (Int, Int) -> Int = { foo, bar -> foo * bar }
            val fooBar2: (Int, Int) -> Int = {
                foo,
                bar // The comma should be inserted before the comment
                -> foo * bar
            }
            val fooBar3: (Int, Int) -> Int = {
                foo,
                bar /* The comma should be inserted before the comment */
                -> foo * bar
            }
            """.trimIndent()
        val formattedCode =
            """
            val fooBar1: (Int, Int) -> Int = { foo, bar -> foo * bar }
            val fooBar2: (Int, Int) -> Int = {
                foo,
                bar, // The comma should be inserted before the comment
                -> foo * bar
            }
            val fooBar3: (Int, Int) -> Int = {
                foo,
                bar, /* The comma should be inserted before the comment */
                -> foo * bar
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(4, 8, "Missing trailing comma before \"->\""),
                LintViolation(9, 8, "Missing trailing comma before \"->\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from the type argument list when present`() {
        val code =
            """
            val list1: List<String,> = emptyList()
            val list2: List<
                String, // The comma before the comment should be removed without removing the comment itself
            > = emptyList()
            val list3: List<
                String, /* The comma before the comment should be removed without removing the comment itself */
            > = emptyList()
            """.trimIndent()
        val formattedCode =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String // The comma before the comment should be removed without removing the comment itself
            > = emptyList()
            val list3: List<
                String /* The comma before the comment should be removed without removing the comment itself */
            > = emptyList()
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
            .hasLintViolations(
                LintViolation(1, 23, "Unnecessary trailing comma before \">\""),
                LintViolation(3, 11, "Unnecessary trailing comma before \">\""),
                LintViolation(6, 11, "Unnecessary trailing comma before \">\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the type argument list when missing`() {
        val code =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String // The comma should be inserted before the comment
            > = emptyList()
            val list3: List<
                String /* The comma should be inserted before the comment */
            > = emptyList()
            """.trimIndent()
        val formattedCode =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String, // The comma should be inserted before the comment
            > = emptyList()
            val list3: List<
                String, /* The comma should be inserted before the comment */
            > = emptyList()
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(3, 11, "Missing trailing comma before \">\""),
                LintViolation(6, 11, "Missing trailing comma before \">\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from the array index when present`() {
        val code =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1,]
            val bar2 = foo[
                1, // The comma before the comment should be removed without removing the comment itself
            ]
            val bar3 = foo[
                1, /* The comma before the comment should be removed without removing the comment itself */
            ]
            """.trimIndent()
        val formattedCode =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1]
            val bar2 = foo[
                1 // The comma before the comment should be removed without removing the comment itself
            ]
            val bar3 = foo[
                1 /* The comma before the comment should be removed without removing the comment itself */
            ]
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
            .hasLintViolations(
                LintViolation(2, 17, "Unnecessary trailing comma before \"]\""),
                LintViolation(4, 6, "Unnecessary trailing comma before \"]\""),
                LintViolation(7, 6, "Unnecessary trailing comma before \"]\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the array index when missing`() {
        val code =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1]
            val bar2 = foo[
                1 // The comma should be inserted before the comment
            ]
            val bar3 = foo[
                1 /* The comma should be inserted before the comment */
            ]
            """.trimIndent()
        val formattedCode =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1]
            val bar2 = foo[
                1, // The comma should be inserted before the comment
            ]
            val bar3 = foo[
                1, /* The comma should be inserted before the comment */
            ]
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(4, 6, "Missing trailing comma before \"]\""),
                LintViolation(7, 6, "Missing trailing comma before \"]\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from the collection literal when present`() {
        val code =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2,])
            val foo1: Int = 0

            @Annotation([
                1,
                2, // The comma before the comment should be removed without removing the comment itself
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2, /* The comma before the comment should be removed without removing the comment itself */
            ])
            val foo3: Int = 0
            """.trimIndent()
        val formattedCode =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2])
            val foo1: Int = 0

            @Annotation([
                1,
                2 // The comma before the comment should be removed without removing the comment itself
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2 /* The comma before the comment should be removed without removing the comment itself */
            ])
            val foo3: Int = 0
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
            .hasLintViolations(
                LintViolation(3, 18, "Unnecessary trailing comma before \"]\""),
                LintViolation(8, 6, "Unnecessary trailing comma before \"]\""),
                LintViolation(14, 6, "Unnecessary trailing comma before \"]\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the collection literal when missing`() {
        val code =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2])
            val foo1: Int = 0

            @Annotation([
                1,
                2 // The comma should be inserted before the comment
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2 /* The comma should be inserted before the comment */
            ])
            val foo3: Int = 0

            @Annotation(
                [
                    1,
                    2 /* The comma should be inserted before the comment */
                ],
                [
                    3,
                    4 /* The comma should be inserted before the comment */
                ]
            )
            val foo4: Int = 0
            """.trimIndent()
        val formattedCode =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2])
            val foo1: Int = 0

            @Annotation([
                1,
                2, // The comma should be inserted before the comment
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2, /* The comma should be inserted before the comment */
            ])
            val foo3: Int = 0

            @Annotation(
                [
                    1,
                    2, /* The comma should be inserted before the comment */
                ],
                [
                    3,
                    4, /* The comma should be inserted before the comment */
                ],
            )
            val foo4: Int = 0
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(8, 6, "Missing trailing comma before \"]\""),
                LintViolation(14, 6, "Missing trailing comma before \"]\""),
                LintViolation(21, 10, "Missing trailing comma before \"]\""),
                LintViolation(25, 10, "Missing trailing comma before \"]\""),
                LintViolation(26, 6, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `1297 - Given that the trailing comma is required on call site the a trailing comma to collection literal when missing`() {
        val code =
            """
            annotation class FooBar(
                val foo1: Array<String> = [],
                val foo2: Array<String> = [],
                val bar1: String = ""
            )

            @FooBar(
                foo1 = [
                    "foo-1" // Add trailing comma as the argument value list foo1 is a multiline statement
                ],
                foo2 = ["foo-2"], // Do not add trailing comma in the array as the argument value list foo2 is a single line statement
                bar1 = "bar-1" // Add trailing comma as the outer argument value list of the annotation is a multiline statement
            )
            val fooBar = null
            """.trimIndent()
        val formattedCode =
            """
            annotation class FooBar(
                val foo1: Array<String> = [],
                val foo2: Array<String> = [],
                val bar1: String = ""
            )

            @FooBar(
                foo1 = [
                    "foo-1", // Add trailing comma as the argument value list foo1 is a multiline statement
                ],
                foo2 = ["foo-2"], // Do not add trailing comma in the array as the argument value list foo2 is a single line statement
                bar1 = "bar-1", // Add trailing comma as the outer argument value list of the annotation is a multiline statement
            )
            val fooBar = null
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(9, 16, "Missing trailing comma before \"]\""),
                LintViolation(12, 19, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that a trailing comma is required on call site and declaration site then still it should not be added to the setter`() {
        val code =
            """
            class Test {
              var foo = Bar()
                set(value) {
                }
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasNoLintViolations()
    }

    @Test
    fun `Issue 1312 - Given that a trailing comma is required on declaration site and multiple elements then force lambda arrow to next line`() {
        val code =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2 -> "a"
                3,
                4 -> "b"
                5,
                6-> "c"
                else -> "d"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2 -> "a"
                3,
                4,
                -> "b"
                5,
                6,
                -> "c"
                else -> "d"
            }
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolations(
                LintViolation(4, 6, "Missing trailing comma and newline before \"->\""),
                LintViolation(6, 6, "Missing trailing comma and newline before \"->\"")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that a trailing comma is required on declaration site and unused imports do not affect each other`() {
        val code =
            """
            package com.pinterest.ktlint
            import com.pinterest.ktlint.enum.Enum
            import com.pinterest.ktlint.enum.EnumThree
            import com.pinterest.ktlint.enum.EnumTwo
            data class TrailingCommaTest(
                val foo: String,
                val bar: Enum,
                val bar2: EnumTwo,
                val bar3: EnumThree
            )
            """.trimIndent()
        val formattedCode =
            """
            package com.pinterest.ktlint
            import com.pinterest.ktlint.enum.Enum
            import com.pinterest.ktlint.enum.EnumThree
            import com.pinterest.ktlint.enum.EnumTwo
            data class TrailingCommaTest(
                val foo: String,
                val bar: Enum,
                val bar2: EnumTwo,
                val bar3: EnumThree,
            )
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            // When running format mode, the rules are first executed in parallel to find linting errors. In this
            // process, no unused import are found because the trailing comma is not yet added to variable "bar3". Then
            // in the next stage the rules are run consecutively. Now the trailing comma rule is adding a trailing comma
            // after the type of variable "bar3". When the no-unused-import rule runs after the trailing-comma rule, it
            // was incorrectly seen as part of the type of variable "bar3" and a reference "EnumThree," (with the
            // trailing comma was added) which in turn resulted in not recognizing that the import of EnumThree actually
            // was used.
            .addAdditionalFormattingRule(NoUnusedImportsRule())
            .withEditorConfigOverride(allowTrailingCommaProperty to true)
            .hasLintViolation(9, 24, "Missing trailing comma before \")\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1379 - Given that a trailing comma is required on call site then add trailing comma after array in annotation when missing`() {
        val code =
            """
            import kotlin.reflect.KClass

            @Foo(
                values = [
                    Foo::class,
                    Foo::class
                ]
            )
            annotation class Foo(val values: Array<KClass<*>>)
            """.trimIndent()
        val formattedCode =
            """
            import kotlin.reflect.KClass

            @Foo(
                values = [
                    Foo::class,
                    Foo::class,
                ],
            )
            annotation class Foo(val values: Array<KClass<*>>)
            """.trimIndent()
        trailingCommaRuleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(6, 19, "Missing trailing comma before \"]\""),
                LintViolation(7, 6, "Missing trailing comma before \")\"")
            ).isFormattedAs(formattedCode)
    }
}
