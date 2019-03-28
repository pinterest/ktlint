package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.diffFileFormat
import com.github.shyiko.ktlint.test.diffFileLint
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

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

    private val formattedImports =
        """
        import android.app.Activity
        import android.view.View
        import android.view.ViewGroup
        import java.util.List
        import kotlin.concurrent.Thread
        """.trimIndent()

    private val errorMessage = "Imports must be ordered in lexicographic order in a single group"

    @Test
    fun testFormatOk() {
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
        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(
            listOf(LintError(1, 1, "import-ordering", errorMessage))
        )
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
        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(
            listOf(LintError(1, 1, "import-ordering", errorMessage))
        )
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatBlankLinesForMajorGroups() {
        val imports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup

            import java.util.List

            import kotlin.concurrent.Thread
            """.trimIndent()
        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(
            listOf(LintError(1, 1, "import-ordering", errorMessage))
        )
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
        assertThat(ImportOrderingRule().lint(imports)).isEqualTo(
            listOf(LintError(1, 1, "import-ordering", errorMessage))
        )
        assertThat(ImportOrderingRule().format(imports)).isEqualTo(formattedImports)
    }
}
