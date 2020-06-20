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
}
