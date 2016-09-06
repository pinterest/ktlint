package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.NoConsecutiveBlankLinesRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoConsecutiveBlankLinesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoConsecutiveBlankLinesRule().lint("fun main() {\n\n\n}")).isEqualTo(listOf(
            LintError(3, 1, "no-consecutive-blank-lines", "Needless blank line(s)")
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
