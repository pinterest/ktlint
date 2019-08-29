package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImportOrderingRuleTest {

    @Test
    fun testLint() {
        assertThat(ImportOrderingRule().diffFileLint("spec/import-ordering/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            ImportOrderingRule().diffFileFormat(
                "spec/import-ordering/format.kt.spec",
                "spec/import-ordering/format-expected.kt.spec"
            )
        ).isEmpty()
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

        assertThat(ImportOrderingRule().lint(formattedImports)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports)).isEqualTo(formattedImports)
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

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between"
            )
        )
        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatDuplicate() {
        val imports =
            """
            import android.view.ViewGroup
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between"
            )
        )
        val formattedImports =
            """
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
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

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between"
            )
        )
        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
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

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between"
            )
        )
        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatImportsWithEOLComments() {
        val imports =
            """
            import android.view.View
            import android.app.Activity // comment
            import android.view.ViewGroup
            """.trimIndent()

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between"
            )
        )
        val formattedImports =
            """
            import android.app.Activity // comment
            import android.view.View
            import android.view.ViewGroup
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
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

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between -- no autocorrection due to comments in the import list"
            )
        )
        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(imports)
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

        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between -- no autocorrection due to comments in the import list"
            )
        )
        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(imports)
    }
}
