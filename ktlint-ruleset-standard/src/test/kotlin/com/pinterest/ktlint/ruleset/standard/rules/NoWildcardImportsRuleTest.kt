package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.intellij_idea
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.ruleset.standard.rules.NoWildcardImportsRule.Companion.IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoWildcardImportsRuleTest {
    private val noWildcardImportsRuleAssertThat = assertThatRule { NoWildcardImportsRule() }

    @Nested
    inner class `Given that editorconfig property ij_kotlin_packages_to_use_import_on_demand is not set` {
        @Test
        fun `Given the ktlint_official codestyle then the default wildcard imports allowed in other code styles are no longer allowed`() {
            val code =
                """
                import a
                import a.b.*
                import a.b.c
                import a.d.*
                import foo.bar.`**`
                import java.util.*
                import kotlinx.android.synthetic.*
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 1, "Wildcard import"),
                    LintViolation(4, 1, "Wildcard import"),
                    LintViolation(6, 1, "Wildcard import"),
                    LintViolation(7, 1, "Wildcard import"),
                )
        }

        @Test
        fun `Given a non-ktlint_official codestyle then the default wildcard imports allowed in other code styles are no longer allowed`() {
            val code =
                """
                import a
                import a.b.*
                import a.b.c
                import a.d.*
                import foo.bar.`**`
                import java.util.*
                import kotlinx.android.synthetic.*
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to intellij_idea)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 1, "Wildcard import"),
                    LintViolation(4, 1, "Wildcard import"),
                )
        }
    }

    @Nested
    inner class `Given that editorconfig property ij_kotlin_packages_to_use_import_on_demand is set` {
        @Test
        fun `Given ktlint_official code style and the property is set with value 'unset' then packages which are accepted by IntelliJ Default are detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                import react.*
                import react.dom.*
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .withEditorConfigOverride(IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND to "unset")
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(2, 1, "Wildcard import"),
                    LintViolation(3, 1, "Wildcard import"),
                    LintViolation(4, 1, "Wildcard import"),
                )
        }

        @Test
        fun `Given non-ktlint_official code style and the property is set with value 'unset' then packages which are accepted by IntelliJ Default are not detected`() {
            val code =
                """
                import a.b
                import kotlinx.android.synthetic.main.layout_name.*
                import react.*
                import react.dom.*
                """.trimIndent()
            noWildcardImportsRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to intellij_idea)
                .withEditorConfigOverride(IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND to "unset")
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(3, 1, "Wildcard import"),
                    LintViolation(4, 1, "Wildcard import"),
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
                .withEditorConfigOverride(IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND to "react.*,react.dom.*")
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
                .withEditorConfigOverride(IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND to "react.**")
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
                .withEditorConfigOverride(IJ_KOTLIN_PACKAGES_TO_USE_IMPORT_ON_DEMAND to "")
                .hasLintViolationWithoutAutoCorrect(2, 1, "Wildcard import")
        }
    }
}
