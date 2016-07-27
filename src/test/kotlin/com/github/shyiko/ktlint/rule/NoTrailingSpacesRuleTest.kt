package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoTrailingSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoTrailingSpacesRule().lint("fun main() {\n    val a = 1\n\n \n} "))
            .isEqualTo(listOf(
                LintError(4, 1, "rule-id", "Trailing space(s)"),
                LintError(5, 2, "rule-id", "Trailing space(s)")
            ))
    }

    @Test
    fun testFormat() {
        assertThat(NoTrailingSpacesRule().format("fun main() {\n    val a = 1 \n  \n \n} "))
            .isEqualTo("fun main() {\n    val a = 1\n\n\n}")
    }

}
