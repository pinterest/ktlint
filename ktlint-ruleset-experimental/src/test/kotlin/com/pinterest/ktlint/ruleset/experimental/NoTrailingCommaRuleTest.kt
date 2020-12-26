package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoTrailingCommaRuleTest {
    @Test
    fun testFormatIsCorrectWithArgumentList() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 28, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 4, col = 8, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 8, col = 8, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithValueList() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 29, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 3, col = 16, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 6, col = 16, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithClassTypeParameters() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 16, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 4, col = 6, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 8, col = 6, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithWhenEntry() {
        val code =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2, -> "a"
                3, 4, // The comma before the comment should be removed without removing the comment itself
                    -> "a"
                5, 6, /* The comma before the comment should be removed without removing the comment itself */ -> "a"
                else -> "b"
            }
            """.trimIndent()
        val autoCorrectedCode =
            """
            fun foo(bar: Int): String = when(bar) {
                1, 2 -> "a"
                3, 4 // The comma before the comment should be removed without removing the comment itself
                    -> "a"
                5, 6 /* The comma before the comment should be removed without removing the comment itself */ -> "a"
                else -> "b"
            }
            """.trimIndent()

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 2, col = 9, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 3, col = 9, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 5, col = 9, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithDestructuringDeclaration() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 4, col = 14, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 7, col = 10, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 11, col = 10, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithFunctionParameters() {
        val code =
            """
            val foo1 = Pair(1, 2,)
            val foo2 = Pair(
                1,
                2, // The comma before the comment should be removed without removing the comment itself
            )
            val foo3 = Pair(
                1,
                2, /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        val autoCorrectedCode =
            """
            val foo1 = Pair(1, 2)
            val foo2 = Pair(
                1,
                2 // The comma before the comment should be removed without removing the comment itself
            )
            val foo3 = Pair(
                1,
                2 /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 21, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 4, col = 6, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 8, col = 6, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithFunctionType() {
        val code =
            """
            val fooBar1: (Int, Int,) -> Int = 42
            val fooBar2: (
                Int,
                Int, // The comma before the comment should be removed without removing the comment itself
            ) -> Int = 42
            val fooBar3: (
                Int,
                Int, /* The comma before the comment should be removed without removing the comment itself */
            ) -> Int = 42
            """.trimIndent()
        val autoCorrectedCode =
            """
            val fooBar1: (Int, Int) -> Int = 42
            val fooBar2: (
                Int,
                Int // The comma before the comment should be removed without removing the comment itself
            ) -> Int = 42
            val fooBar3: (
                Int,
                Int /* The comma before the comment should be removed without removing the comment itself */
            ) -> Int = 42
            """.trimIndent()

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 23, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 4, col = 8, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 8, col = 8, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithFunctionLiteral() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 44, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 4, col = 8, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 9, col = 8, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithTypeArgumentList() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 23, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 3, col = 11, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 6, col = 11, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithArrayIndexExpression() {
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

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 2, col = 17, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 4, col = 6, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
                LintError(line = 7, col = 6, ruleId = "no-trailing-comma", detail = "Trailing comma is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }
}
