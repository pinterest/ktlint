package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class ImportOrderingRuleAsciiTest {
    private val importOrderingRuleAssertThat = assertThatRule { ImportOrderingRule() }

    @Test
    fun `Given some imports which are sorted incorrectly then do return lint errors`() {
        val code =
            """
            import android.view.ViewGroup
            import android.view.View
            import android.app.Activity
            import kotlin.concurrent.Thread
            import java.util.List
            """.trimIndent()
        val formattedCode =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolation(1, 1, "Imports must be ordered in lexicographic order without any empty lines in-between")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given imports which are sorted correctly then do not return lint errors`() {
        val code =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasNoLintViolations()
    }

    @Test
    fun `Given some imports including duplicates then do return a lint error`() {
        val code =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.view.View
            """.trimIndent()
        val formattedCode =
            """
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolation(3, 1, "Duplicate 'import android.view.View' found")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports in correct order and containing blank lines then do return a lint error`() {
        val code =
            """
            import android.app.Activity
            import android.view.View

            import android.view.ViewGroup
            import java.util.List


            import kotlin.concurrent.Thread
            """.trimIndent()
        val formattedCode =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolation(1, 1, "Imports must be ordered in lexicographic order without any empty lines in-between")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports having an EOL comment but in incorrect order then retain the comment and do return a lint error`() {
        val code =
            """
            import android.view.View
            import android.app.Activity // comment
            import android.view.ViewGroup
            """.trimIndent()
        val formattedCode =
            """
            import android.app.Activity // comment
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolation(1, 1, "Imports must be ordered in lexicographic order without any empty lines in-between")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports separated by a block comment then do return a lint error`() {
        val code =
            """
            import android.view.View
            /* comment */
            import android.app.Activity
            import android.view.ViewGroup
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolationWithoutAutoCorrect(
                1,
                1,
                "Imports must be ordered in lexicographic order without any empty lines in-between -- no autocorrection due to comments in the import list",
            )
    }

    @Test
    fun `Given some imports separated by a EOL comment on a separate line then do return a lint error`() {
        val code =
            """
            import android.view.View
            // comment
            import android.app.Activity
            import android.view.ViewGroup
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolationWithoutAutoCorrect(
                1,
                1,
                "Imports must be ordered in lexicographic order without any empty lines in-between -- no autocorrection due to comments in the import list",
            )
    }

    @Test
    fun `Given some imports with aliases then retain them and do return a lint error`() {
        val code =
            """
            import android.view.ViewGroup as VG
            import android.view.View as V
            import android.app.Activity
            import kotlin.concurrent.Thread
            import java.util.List as L
            """.trimIndent()
        val formattedCode =
            """
            import android.app.Activity
            import android.view.View as V
            import android.view.ViewGroup as VG
            import java.util.List as L
            import kotlin.concurrent.Thread
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolation(1, 1, "Imports must be ordered in lexicographic order without any empty lines in-between")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports between backticks then ignore the backticks for the sort order`() {
        val code =
            """
            import org.mockito.Mockito.`when`
            import org.mockito.Mockito.verify
            """.trimIndent()
        val formattedCode =
            """
            import org.mockito.Mockito.verify
            import org.mockito.Mockito.`when`
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(ASCII_IMPORT_ORDERING)
            .hasLintViolation(1, 1, "Imports must be ordered in lexicographic order without any empty lines in-between")
            .isFormattedAs(formattedCode)
    }

    private companion object {
        val ASCII_IMPORT_ORDERING = ImportOrderingRule.ideaImportsLayoutProperty to "*"
    }
}
