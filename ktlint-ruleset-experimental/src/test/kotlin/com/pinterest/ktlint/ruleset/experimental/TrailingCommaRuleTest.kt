package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule
import com.pinterest.ktlint.ruleset.standard.NoUnusedImportsRule
import com.pinterest.ktlint.test.EditorConfigOverride
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class TrailingCommaRuleTest {
    @Test
    fun testTrailingCommaOnCallSiteIsRedundant() {
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 27, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 3, col = 21, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 5, col = 22, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 8, col = 18, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 11, col = 12, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
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
        )
    }

    @Test
    fun testTrailingCommaOnDeclarationSiteIsRedundant() {
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 29, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 3, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 6, col = 9, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 13, col = 14, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 16, col = 20, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 18, col = 20, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 18, col = 42, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
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
        )
    }

    @Test
    fun testComplexCodePath() {
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA)).isEqualTo(
            listOf(
                LintError(line = 4, col = 29, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 6, col = 13, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 8, col = 9, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\""),
                LintError(line = 11, col = 12, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\""),
                LintError(line = 15, col = 22, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
    }

    @Test
    fun `trailing comma not allowed for argument list`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 28, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 8, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for argument list`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 8, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for parameter list`() {
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
        val autoCorrectedCode =
            """
            data class Foo1(val bar: Int)
            data class Foo2(
               val bar: Int // The comma before the comment should be removed without removing the comment itself
            )
            data class Foo3(
               val bar: Int /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 29, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 3, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 6, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for parameter list`() {
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
        val autoCorrectedCode =
            """
            data class Foo1(val bar: Int)
            data class Foo2(
               val bar: Int, // The comma should be inserted before the comment
            )
            data class Foo3(
               val bar: Int, /* The comma should be inserted before the comment */
            )
            """.trimIndent()

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 3, col = 16, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 6, col = 16, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for type parameter list`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for type parameter list`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\""),
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for when condition`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 2, col = 9, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 3, col = 9, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 6, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for when condition`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 3, col = 9, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\""),
                LintError(line = 6, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for destructuring declaration`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 4, col = 14, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 7, col = 10, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 11, col = 10, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for destructuring declaration`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 7, col = 10, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 11, col = 10, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for function literal`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 44, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 9, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for function literal`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\""),
                LintError(line = 9, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for type argument list`() {
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
        val autoCorrectedCode =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String // The comma before the comment should be removed without removing the comment itself
            > = emptyList()
            val list3: List<
                String /* The comma before the comment should be removed without removing the comment itself */
            > = emptyList()
            """.trimIndent()

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 1, col = 23, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 3, col = 11, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 6, col = 11, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for type argument list`() {
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
        val autoCorrectedCode =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String, // The comma should be inserted before the comment
            > = emptyList()
            val list3: List<
                String, /* The comma should be inserted before the comment */
            > = emptyList()
            """.trimIndent()

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 3, col = 11, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\""),
                LintError(line = 6, col = 11, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for array index`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 2, col = 17, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 7, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for array index`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 7, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma not allowed for collection literal`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 3, col = 18, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 14, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `trailing comma required for collection literal`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 14, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 21, col = 10, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 25, col = 10, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 26, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `1297 - trailing comma required for collection literal`() {
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
        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE)).isEqualTo(
            listOf(
                LintError(line = 9, col = 16, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 12, col = 19, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_CALL_SITE))
            .isEqualTo(formattedCode)
    }

    @Test
    fun `Trailing comma is not added for property setter`() {
        val code =
            """
            class Test {
              var foo = Bar()
                set(value) {
                }
            }
            """.trimIndent()

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA)).isEmpty()
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA))
            .isEqualTo(code)
    }

    @Test
    fun `Issue 1312 - multiple element in when clause and trailing comma required then force lambda arrow to next line`() {
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
        val autoCorrectedCode =
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

        assertThat(TrailingCommaRule().lint(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE)).isEqualTo(
            listOf(
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma and newline before \"->\""),
                LintError(line = 6, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma and newline before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(code, ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun `Trailing comma and unused imports do not affect each other`() {
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

        val rules = listOf(TrailingCommaRule(), NoUnusedImportsRule())
        assertThat(rules.lint(code, ALLOW_TRAILING_COMMA)).containsExactly(
            LintError(9, 24, "trailing-comma", "Missing trailing comma before \")\"")
        )
        assertThat(rules.format(code, ALLOW_TRAILING_COMMA)).isEqualTo(formattedCode)

        // When running format mode, the rules are first executed in parallel to find linting errors. In this process,
        // no unused import are found because the trailing comma is not yet added to variable "bar3". Then in the next
        // stage the rules are run consecutively. Now the trailing comma rule is adding a trailing comma after the type
        // of variable "bar3". When the no-unused-import rule runs after the trailing-comma rule, it was incorrectly
        // seen as part of the type of variable "bar3" and a reference "EnumThree," (with the trailing comma was added)
        // which in turn resulted in not recognizing that the import of EnumThree actually was used.
        val afterFormatLintErrors = ArrayList<LintError>()
        val formatResult =
            rules.format(code, ALLOW_TRAILING_COMMA, cb = { e, _ -> afterFormatLintErrors.add(e) })
        assertThat(afterFormatLintErrors).isEmpty()
        assertThat(formatResult).isEqualTo(formattedCode)
    }

    private companion object {
        val ALLOW_TRAILING_COMMA =
            EditorConfigOverride.from(
                TrailingCommaRule.allowTrailingCommaProperty to true,
                TrailingCommaRule.allowTrailingCommaOnCallSiteProperty to true
            )

        val ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE =
            EditorConfigOverride.from(TrailingCommaRule.allowTrailingCommaProperty to true)
        val DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE =
            EditorConfigOverride.from(TrailingCommaRule.allowTrailingCommaProperty to false)

        val ALLOW_TRAILING_COMMA_ON_CALL_SITE =
            EditorConfigOverride.from(TrailingCommaRule.allowTrailingCommaOnCallSiteProperty to true)
        val DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE =
            EditorConfigOverride.from(TrailingCommaRule.allowTrailingCommaOnCallSiteProperty to false)
    }
}
