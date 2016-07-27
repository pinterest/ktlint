package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoConsecutiveBlankLinesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoConsecutiveBlankLinesRule().lint("fun main() {\n\n\n}")).isEqualTo(listOf(
            LintError(3, 1, "rule-id", "Needless blank line(s)")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(NoConsecutiveBlankLinesRule().format(
            """
            fun main() {


            }



            """
        )).isEqualTo(
            """
            fun main() {

            }

            """
        )
    }

}
