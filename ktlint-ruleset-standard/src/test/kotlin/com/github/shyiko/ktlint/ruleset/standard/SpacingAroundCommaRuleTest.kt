package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundCommaRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundCommaRule().lint("fun main() { x(1,3); x(1, 3); println(\",\") }"))
            .isEqualTo(listOf(
                LintError(1, 18, "comma-spacing", "Missing spacing after \",\"")
            ))
        assertThat(SpacingAroundCommaRule().lint(
            """
            enum class E {
                A, B,C
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 10, "comma-spacing", "Missing spacing after \",\"")
        ))
        assertThat(SpacingAroundCommaRule().lint(
            """
            some.method(1 , 2)
            """.trimIndent(),
            script = true
        )).isEqualTo(listOf(
            LintError(1, 14, "comma-spacing", "Unexpected spacing before \",\"")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundCommaRule().format("fun main() { x(1,3); x(1, 3) }"))
            .isEqualTo("fun main() { x(1, 3); x(1, 3) }")
        assertThat(SpacingAroundCommaRule().format(
            """
            fun fn(
                arg1: Int ,
                arg2: Int
                ,

                arg3: Int
            ) = Unit
            """.trimIndent()
        )).isEqualTo(
            """
            fun fn(
                arg1: Int,
                arg2: Int,

                arg3: Int
            ) = Unit
            """.trimIndent())
    }
}
