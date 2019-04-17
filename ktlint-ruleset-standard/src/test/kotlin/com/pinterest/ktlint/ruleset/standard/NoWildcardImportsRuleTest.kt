package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoWildcardImportsRuleTest {

    @Test
    fun testLint() {
        assertThat(
            com.pinterest.ktlint.ruleset.standard.NoWildcardImportsRule().lint(
                """
                import a.*
                import a.b.c.*
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-wildcard-imports", "Wildcard import"),
                LintError(2, 1, "no-wildcard-imports", "Wildcard import")
            )
        )
    }
}
