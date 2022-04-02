package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class ImportOrderingRuleAsciiTest {
    private val rule = ImportOrderingRule()

    @Test
    fun testFormat() {
        val imports =
            """
            import a.A
            import b.C
            import a.AB
            """.trimIndent()

        val formattedImports =
            """
            import a.A
            import a.AB
            import b.C
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatOk() {
        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(
            rule.lint(formattedImports, ASCII_IMPORT_ORDERING)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatWrongOrder() {
        val imports =
            """
            import android.view.ViewGroup
            import android.view.View
            import android.app.Activity
            import kotlin.concurrent.Thread
            import java.util.List
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatDuplicate() {
        val imports =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.view.View
            """.trimIndent()

        val formattedImports =
            """
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).containsExactly(
            LintError(3, 1, "import-ordering", "Duplicate 'import android.view.View' found")
        )
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatWrongOrderAndBlankLines() {
        val imports =
            """
            import android.view.ViewGroup


            import android.view.View
            import android.app.Activity

            import kotlin.concurrent.Thread

            import java.util.List
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatBlankLines() {
        val imports =
            """
            import android.app.Activity
            import android.view.View

            import android.view.ViewGroup
            import java.util.List


            import kotlin.concurrent.Thread
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatImportsWithEOLComments() {
        val imports =
            """
            import android.view.View
            import android.app.Activity // comment
            import android.view.ViewGroup
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity // comment
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun testCannotFormatImportsWithBlockComments() {
        val imports =
            """
            import android.view.View
            /* comment */
            import android.app.Activity
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors(" -- no autocorrection due to comments in the import list"))
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(imports)
    }

    @Test
    fun testCannotFormatImportsWithEOLComments() {
        val imports =
            """
            import android.view.View
            // comment
            import android.app.Activity
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors(" -- no autocorrection due to comments in the import list"))
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(imports)
    }

    @Test
    fun testAliasesAreSortedAmongNormalImports() {
        val imports =
            """
            import android.view.ViewGroup as VG
            import android.view.View as V
            import android.app.Activity
            import kotlin.concurrent.Thread
            import java.util.List as L
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View as V
            import android.view.ViewGroup as VG
            import java.util.List as L
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `backticks should be ignored in imports`() {
        val imports =
            """
            import org.mockito.Mockito.`when`
            import org.mockito.Mockito.verify
            """.trimIndent()

        val formattedImports =
            """
            import org.mockito.Mockito.verify
            import org.mockito.Mockito.`when`
            """.trimIndent()

        assertThat(
            rule.lint(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors())
        assertThat(
            rule.format(imports, ASCII_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    private companion object {
        fun expectedErrors(additionalMessage: String = "") = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between$additionalMessage"
            )
        )

        val ASCII_IMPORT_ORDERING = EditorConfigOverride.from(
            ImportOrderingRule.ideaImportsLayoutProperty to "*"
        )
    }
}
