package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundDotRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundDotRule().lint("fun String .foo() = \"foo . \""))
            .isEqualTo(listOf(
                    LintError(1, 11, "dot-spacing", "Unexpected spacing before \".\"")
            ))

        assertThat(SpacingAroundDotRule().lint("fun String. foo() = \"foo . \""))
            .isEqualTo(listOf(
                    LintError(1, 12, "dot-spacing", "Unexpected spacing after \".\"")
            ))

        assertThat(SpacingAroundDotRule().lint("fun String . foo() = \"foo . \""))
            .isEqualTo(listOf(
                    LintError(1, 11, "dot-spacing", "Unexpected spacing before \".\""),
                    LintError(1, 13, "dot-spacing", "Unexpected spacing after \".\"")
            ))

        assertThat(SpacingAroundDotRule().lint(
            """
            |fun String.foo() {
            |    (2..10).map { it + 1 }
            |        .map { it * 2 }
            |        .toSet()
            |}
            """.trimMargin()
        )).isEqualTo(
            emptyList<LintError>()
        )

        assertThat(SpacingAroundDotRule().lint(
            """
            |fun String.foo() {
            |    (2..10).map { it + 1 }
            |        . map { it * 2 }
            |        .toSet()
            |}
            """.trimMargin()
        )).isEqualTo(listOf(
                LintError(3, 10, "dot-spacing", "Unexpected spacing after \".\"")
        ))

        assertThat(SpacingAroundDotRule().lint(
            """
            |fun String.foo() {
            |    (2..10).map { it + 1 }
            |        // Some comment
            |        . map { it * 2 }
            |        .toSet()
            |}
            """.trimMargin()
        )).isEqualTo(listOf(
                LintError(4, 10, "dot-spacing", "Unexpected spacing after \".\"")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundDotRule().format("fun String .foo() = \"foo . \""))
            .isEqualTo("fun String.foo() = \"foo . \"")

        assertThat(SpacingAroundDotRule().format("fun String. foo() = \"foo . \""))
            .isEqualTo("fun String.foo() = \"foo . \"")

        assertThat(SpacingAroundDotRule().format("fun String . foo() = \"foo . \""))
            .isEqualTo("fun String.foo() = \"foo . \"")

        assertThat(SpacingAroundDotRule().format(
            """
            |fun String.foo() {
            |    (2..10).map { it + 1 }
            |        . map { it * 2 }
            |        .toSet()
            |}
            """.trimMargin()
        )).isEqualTo(
            """
            |fun String.foo() {
            |    (2..10).map { it + 1 }
            |        .map { it * 2 }
            |        .toSet()
            |}
            """.trimMargin()
        )
    }
}
