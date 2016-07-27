package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoMultipleSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoMultipleSpacesRule().lint("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo(listOf(
                LintError(1, 22, "rule-id", "Unnecessary space(s)")
            ))
    }

    @Test
    fun testFormat() {
        assertThat(NoMultipleSpacesRule().format("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo("fun main() { x(1,3); x(1, 3)\n  \n  }")
    }

}
