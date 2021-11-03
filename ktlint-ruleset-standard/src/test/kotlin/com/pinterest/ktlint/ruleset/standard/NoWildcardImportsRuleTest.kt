package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.test.EditorConfigOverride
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class NoWildcardImportsRuleTest {
    private val rule = NoWildcardImportsRule()

    @Test
    fun testLint() {
        val imports =
            """
            import a.*
            import a.b.c.*
            import a.b
            import kotlinx.android.synthetic.main.layout_name.*
            import foo.bar.`**`
            """.trimIndent()
        assertThat(
            rule.lint(imports, IDEA_DEFAULT_ALLOWED_WILDCARD_IMPORTS)
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-wildcard-imports", "Wildcard import"),
                LintError(2, 1, "no-wildcard-imports", "Wildcard import"),
            )
        )
    }

    @Test
    fun testAllowedWildcardImports() {
        assertThat(
            rule.lint(
                """
                import a.*
                import a.b.c.*
                import a.b
                import foo.bar.`**`
                import react.*
                import react.dom.*
                import kotlinx.css.*
                """.trimIndent(),
                EditorConfigOverride.from(
                    NoWildcardImportsRule.ideaPackagesToUseImportOnDemandProperty to "react.*,react.dom.*"
                )
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "no-wildcard-imports", "Wildcard import"),
                LintError(2, 1, "no-wildcard-imports", "Wildcard import"),
                LintError(7, 1, "no-wildcard-imports", "Wildcard import")
            )
        )
    }

    private companion object {
        val IDEA_DEFAULT_ALLOWED_WILDCARD_IMPORTS = EditorConfigOverride.from(
            NoWildcardImportsRule.ideaPackagesToUseImportOnDemandProperty to "java.util.*,kotlinx.android.synthetic.**"
        )
    }
}
