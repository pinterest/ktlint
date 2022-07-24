package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.ruleset.standard.NoWildcardImportsRule.Companion.packagesToUseImportOnDemandProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoWildcardImportsRuleTest {
    private val noWildcardImportsRuleAssertThat = assertThatRule { NoWildcardImportsRule() }

    @DisplayName("Given that .editorconfig property packagesToUseImportOnDemandProperty is not set")
    @Nested
    inner class PackagesToUseImportOnDemandPropertyNotSet {
        @Test
        fun `Wildcard imports are detected`() {
            val code =
                """
                import a
                import a.b.*
                import a.b.c
                import a.d.*
                import foo.bar.`**`
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 1, "Wildcard import"),
                    LintViolation(4, 1, "Wildcard import")
                )
        }

        @Test
        fun `Wildcard imports on packages which are accepted by IntelliJ Default are not detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code).hasNoLintViolations()
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
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(packagesToUseImportOnDemandProperty to "unset")
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(3, 1, "Wildcard import"),
                    LintViolation(4, 1, "Wildcard import")
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
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(packagesToUseImportOnDemandProperty to "react.*,react.dom.*")
                .hasLintViolationWithoutAutoCorrect(2, 1, "Wildcard import")
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
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(packagesToUseImportOnDemandProperty to "react.**")
                .hasLintViolationWithoutAutoCorrect(2, 1, "Wildcard import")
        }

        @Test
        fun `Given that property is set without a value then the packages which otherwise would be accepted by IntelliJ Default are detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(packagesToUseImportOnDemandProperty to "")
                .hasLintViolationWithoutAutoCorrect(2, 1, "Wildcard import")
        }
    }
}
