package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoWildcardImportsRuleTest {

    @Test
    fun testLint() {
        assertThat(NoWildcardImportsRule().lint(
            """
            import a.*
            import a.b.c.*
            import a.b
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 10, "rule-id", "Wildcard import"),
            LintError(2, 14, "rule-id", "Wildcard import")
        ))
    }

}
