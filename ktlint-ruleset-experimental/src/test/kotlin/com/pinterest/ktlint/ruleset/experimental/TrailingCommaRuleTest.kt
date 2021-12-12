package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.experimental.trailingcomma.TrailingCommaRule
import com.pinterest.ktlint.test.EditorConfigTestRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.junit.Rule
import org.junit.Test

@OptIn(FeatureInAlphaState::class)
class TrailingCommaRuleTest {
    @get:Rule
    val editorConfigTestRule = EditorConfigTestRule()

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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 27, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 3, col = 21, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 5, col = 22, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 8, col = 18, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 11, col = 12, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code)).isEqualTo(
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
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
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code)).isEqualTo(
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

        val editorConfigFilePath = writeEditorConfigFile(
            ALLOW_TRAILING_COMMA_ON_CALL_SITE,
            ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE
        ).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 28, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 8, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 8, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 29, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 3, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 6, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 3, col = 16, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 6, col = 16, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 16, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\""),
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 2, col = 9, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 3, col = 9, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 6, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 3, col = 9, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\""),
                LintError(line = 6, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 4, col = 14, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 7, col = 10, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\""),
                LintError(line = 11, col = 10, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 7, col = 10, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\""),
                LintError(line = 11, col = 10, ruleId = "trailing-comma", detail = "Missing trailing comma before \")\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 44, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\""),
                LintError(line = 9, col = 8, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 4, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\""),
                LintError(line = 9, col = 8, ruleId = "trailing-comma", detail = "Missing trailing comma before \"->\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 23, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 3, col = 11, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\""),
                LintError(line = 6, col = 11, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 3, col = 11, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\""),
                LintError(line = 6, col = 11, ruleId = "trailing-comma", detail = "Missing trailing comma before \">\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 2, col = 17, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 7, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 4, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 7, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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

        val editorConfigFilePath = writeEditorConfigFile(DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 3, col = 18, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\""),
                LintError(line = 14, col = 6, ruleId = "trailing-comma", detail = "Unnecessary trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
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
            """.trimIndent()

        val editorConfigFilePath = writeEditorConfigFile(ALLOW_TRAILING_COMMA_ON_CALL_SITE).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEqualTo(
            listOf(
                LintError(line = 8, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\""),
                LintError(line = 14, col = 6, ruleId = "trailing-comma", detail = "Missing trailing comma before \"]\"")
            )
        )
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
            .isEqualTo(autoCorrectedCode)
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

        val editorConfigFilePath = writeEditorConfigFile(
            ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE,
            ALLOW_TRAILING_COMMA_ON_CALL_SITE
        ).absolutePath

        assertThat(TrailingCommaRule().lint(editorConfigFilePath, code)).isEmpty()
        assertThat(TrailingCommaRule().format(editorConfigFilePath, code))
            .isEqualTo(code)
    }

    private fun writeEditorConfigFile(vararg editorConfigProperties: Pair<PropertyType<Boolean>, String>) = editorConfigTestRule
        .writeToEditorConfig(
            mapOf(*editorConfigProperties)
        )

    private companion object {
        val ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE = TrailingCommaRule.allowTrailingCommaProperty.type to true.toString()
        val DO_NOT_ALLOW_TRAILING_COMMA_ON_DECLARATION_SITE = TrailingCommaRule.allowTrailingCommaProperty.type to false.toString()

        val ALLOW_TRAILING_COMMA_ON_CALL_SITE = TrailingCommaRule.allowTrailingCommaOnCallSiteProperty.type to true.toString()
        val DO_NOT_ALLOW_TRAILING_COMMA_ON_CALL_SITE = TrailingCommaRule.allowTrailingCommaOnCallSiteProperty.type to false.toString()
    }
}
