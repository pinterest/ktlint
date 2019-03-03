package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoMultipleSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoMultipleSpacesRule().lint("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo(listOf(
                LintError(1, 22, "no-multi-spaces", "Unnecessary space(s)")
            ))
    }

    @Test
    fun testFormat() {
        assertThat(NoMultipleSpacesRule().format("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo("fun main() { x(1,3); x(1, 3)\n  \n  }")
    }
}
