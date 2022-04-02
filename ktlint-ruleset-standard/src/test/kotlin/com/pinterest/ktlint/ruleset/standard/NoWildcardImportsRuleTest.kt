package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.NoWildcardImportsRule.Companion.packagesToUseImportOnDemandProperty
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@FeatureInAlphaState
class NoWildcardImportsRuleTest {
    @DisplayName("Given that .editorconfig property packagesToUseImportOnDemandProperty is not set")
    @Nested
    inner class PackagesToUseImportOnDemandPropertyNotSet {
        @Test
        fun `Wildcard imports are detected`() {
            val code =
                """
                import a.*
                import a.b.c.*
                import a.b
                import foo.bar.`**`
                """.trimIndent()
            assertThat(NoWildcardImportsRule().lint(code)).containsExactly(
                LintError(1, 1, "no-wildcard-imports", "Wildcard import"),
                LintError(2, 1, "no-wildcard-imports", "Wildcard import")
            )
        }

        @Test
        fun `Wildcard imports on packages which are accepted by IntelliJ Default are not detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                """.trimIndent()
            assertThat(NoWildcardImportsRule().lint(code)).isEmpty()
        }
    }

    @DisplayName("Given that .editorconfig property packagesToUseImportOnDemandProperty is set")
    @Nested
    inner class PackagesToUseImportOnDemandPropertySet {
        @Test
        fun `Given that the property is set with value 'unset' then packages which are accepted by IntelliJ Default are not detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                import react.*
                import react.dom.*
                """.trimIndent()
            assertThat(
                NoWildcardImportsRule().lint(
                    code,
                    EditorConfigOverride.from(
                        packagesToUseImportOnDemandProperty to "unset"
                    )
                )
            ).containsExactly(
                LintError(3, 1, "no-wildcard-imports", "Wildcard import"),
                LintError(4, 1, "no-wildcard-imports", "Wildcard import")
            )
        }

        @Test
        fun `Given that the property is set to some packages exclusive subpackages then wildcard imports for those directories are not detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                import react.*
                import react.dom.*
                """.trimIndent()
            assertThat(
                NoWildcardImportsRule().lint(
                    code,
                    EditorConfigOverride.from(
                        packagesToUseImportOnDemandProperty to "react.*,react.dom.*"
                    )
                )
            ).containsExactly(
                LintError(2, 1, "no-wildcard-imports", "Wildcard import")
            )
        }

        @Test
        fun `Given that the property is set to some packages inclusive subpackages then wildcard imports for those directories are not detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                import react.*
                import react.dom.*
                """.trimIndent()
            assertThat(
                NoWildcardImportsRule().lint(
                    code,
                    EditorConfigOverride.from(
                        packagesToUseImportOnDemandProperty to "react.**"
                    )
                )
            ).containsExactly(
                LintError(2, 1, "no-wildcard-imports", "Wildcard import")
            )
        }

        @Test
        fun `Given that property is set without a value then the packages which otherwise would be accepted by IntelliJ Default are detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                """.trimIndent()
            assertThat(
                NoWildcardImportsRule().lint(
                    code,
                    EditorConfigOverride.from(
                        packagesToUseImportOnDemandProperty to ""
                    )
                )
            ).containsExactly(
                LintError(2, 1, "no-wildcard-imports", "Wildcard import")
            )
        }
    }
}
