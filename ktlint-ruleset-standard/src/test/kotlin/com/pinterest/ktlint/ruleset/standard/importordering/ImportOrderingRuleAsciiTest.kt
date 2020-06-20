package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImportOrderingRuleAsciiTest {

    companion object {
        private val userData = mapOf("kotlin_imports_layout" to "ascii")

        private fun expectedErrors(additionalMessage: String = "") = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between$additionalMessage"
            )
        )
    }

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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
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

        assertThat(ImportOrderingRule().lint(formattedImports, userData)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, userData)).isEqualTo(formattedImports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatDuplicate() {
        val imports =
            """
            import android.view.ViewGroup
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        val formattedImports =
            """
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors(" -- no autocorrection due to comments in the import list"))
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(imports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors(" -- no autocorrection due to comments in the import list"))
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(imports)
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors())
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }
}
