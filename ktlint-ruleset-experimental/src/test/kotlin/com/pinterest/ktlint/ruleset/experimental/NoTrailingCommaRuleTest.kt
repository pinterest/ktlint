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
            """.trimIndent()
        val autoCorrectedCode =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b" // The comma before the comment should be removed without removing the comment itself
            )
            """.trimIndent()

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 1, col = 28, ruleId = "no-trailing-comma", detail = "Trailing command in argument list is redundant"),
                LintError(line = 4, col = 8, ruleId = "no-trailing-comma", detail = "Trailing command in argument list is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }

    @Test
    fun testFormatIsCorrectWithValueList() {
        val code =
            """
            data class Foo1(
               val bar: Int, // The comma before the comment should be removed without removing the comment itself
            )
            data class Foo2(val bar: Int,)
            """.trimIndent()
        val autoCorrectedCode =
            """
            data class Foo1(
               val bar: Int // The comma before the comment should be removed without removing the comment itself
            )
            data class Foo2(val bar: Int)
            """.trimIndent()

        assertThat(NoTrailingCommaRule().lint(code)).isEqualTo(
            listOf(
                LintError(line = 2, col = 16, ruleId = "no-trailing-comma", detail = "Trailing command in argument list is redundant"),
                LintError(line = 4, col = 29, ruleId = "no-trailing-comma", detail = "Trailing command in argument list is redundant"),
            )
        )
        assertThat(NoTrailingCommaRule().format(code))
            .isEqualTo(autoCorrectedCode)
    }
}
