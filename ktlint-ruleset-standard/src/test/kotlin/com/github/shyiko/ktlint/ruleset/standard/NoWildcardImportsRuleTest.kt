package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.NoWildcardImportsRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
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
            LintError(1, 10, "no-wildcard-imports", "Wildcard import"),
            LintError(2, 14, "no-wildcard-imports", "Wildcard import")
        ))
    }

}
