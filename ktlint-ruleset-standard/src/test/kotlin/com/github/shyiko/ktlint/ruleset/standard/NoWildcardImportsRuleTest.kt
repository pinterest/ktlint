package com.github.shyiko.ktlint.ruleset.standard

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
            import kotlinx.android.synthetic.main.layout_name.*
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 1, "no-wildcard-imports", "Wildcard import"),
            LintError(2, 1, "no-wildcard-imports", "Wildcard import")
        ))
    }

    @Test
    fun testLintWithOverride() {
        assertThat(NoWildcardImportsRule().lint(
            """
            import a.*
            import a.b.c.*
            import a.b
            import kotlinx.android.synthetic.main.layout_name.*
            """.trimIndent(),
            mapOf("allow_wildcard_imports" to "true")
        )).isEqualTo(listOf<LintError>())
    }
}
