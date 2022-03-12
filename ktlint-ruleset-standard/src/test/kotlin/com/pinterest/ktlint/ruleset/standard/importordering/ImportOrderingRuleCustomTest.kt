package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.EditorConfigOverride
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class ImportOrderingRuleCustomTest {
    private val rule = ImportOrderingRule()

    @Test
    fun `empty line between imports and aliases - ok`() {
        val formattedImports =
            """
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F

            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride("^,|,*")

        assertThat(
            rule.lint(formattedImports, editorConfigOverride)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `empty line between imports and aliases - no empty line`() {
        val imports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread

            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride("*,|,^")

        assertThat(
            rule.lint(imports, editorConfigOverride)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `empty line between imports and aliases - wrong order`() {
        val imports =
            """
            import android.app.Activity
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F


            import android.view.View
            import android.view.ViewGroup

            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        val formattedImports =
            """
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F

            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride("^,|,*")

        assertThat(
            rule.lint(imports, editorConfigOverride)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `default idea java pattern - ok`() {
        val formattedImports =
            """
            import android.app.Activity
            import android.view.View

            import org.junit.Assert

            import net.stuff.A

            import org.foo.Bar

            import java.util.List

            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "android.**,|,org.junit.**,|,net.**,|,org.**,|,java.**,|,com.**,|,javax.**,|,*"
        )

        assertThat(
            rule.lint(formattedImports, editorConfigOverride)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `default idea java pattern - wrong order`() {
        val imports =
            """
            import android.app.Activity
            import android.view.View

            import net.stuff.A
            import org.junit.Assert
            import kotlin.io.Closeable
            import kotlin.concurrent.Thread




            import java.util.List
            import org.foo.Bar
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View

            import org.junit.Assert

            import net.stuff.A

            import org.foo.Bar

            import java.util.List

            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "android.**,|,org.junit.**,|,net.**,|,org.**,|,java.**,|,com.**,|,javax.**,|,*"
        )

        assertThat(
            rule.lint(imports, editorConfigOverride)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `multiple empty lines - ignored`() {
        val formattedImports =
            """
            import java.util.List

            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.app.Activity
            import android.view.View
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "java.**,|,|,|,kotlin.**,*"
        )

        assertThat(
            rule.lint(formattedImports, editorConfigOverride)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `multiple empty lines - transformed into one`() {
        val imports =
            """
            import java.util.List



            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.app.Activity
            import android.view.View
            """.trimIndent()

        val formattedImports =
            """
            import java.util.List

            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.app.Activity
            import android.view.View
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "java.**,|,|,|,kotlin.**,*"
        )

        assertThat(
            rule.lint(imports, editorConfigOverride)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `alias pattern - ok`() {
        val formattedImports =
            """
            import kotlin.io.Closeable as C
            import android.view.View as V
            import android.app.Activity

            import kotlin.concurrent.Thread
            import java.util.List as L
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "^kotlin.**,^android.**,android.**,|,*,^"
        )

        assertThat(
            rule.lint(formattedImports, editorConfigOverride)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `alias pattern - wrong order`() {
        val imports =
            """
            import android.app.Activity
            import android.view.View as V
            import java.util.List as L
            import kotlin.concurrent.Thread
            import kotlin.io.Closeable as C
            """.trimIndent()

        val formattedImports =
            """
            import kotlin.io.Closeable as C
            import android.view.View as V
            import android.app.Activity

            import java.util.List as L
            import kotlin.concurrent.Thread
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "^kotlin.**,^android.**,android.**,|,^,*"
        )

        assertThat(
            rule.lint(imports, editorConfigOverride)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `full path pattern - ok`() {
        val formattedImports =
            """
            import kotlin.io.Closeable
            import kotlin.concurrent.Thread
            import android.app.Activity
            import android.view.View
            import java.util.List
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "kotlin.io.Closeable.*,kotlin.**,*"
        )

        assertThat(
            rule.lint(formattedImports, editorConfigOverride)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `full path pattern - wrong order`() {
        val imports =
            """
            import android.app.Activity
            import android.view.View
            import java.util.List

            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            """.trimIndent()

        val formattedImports =
            """
            import kotlin.io.Closeable
            import kotlin.concurrent.Thread
            import android.app.Activity
            import android.view.View
            import java.util.List
            """.trimIndent()

        val editorConfigOverride = getCustomImportOrderEditorConfigOverride(
            "kotlin.io.Closeable.*,kotlin.**,*"
        )

        assertThat(
            rule.lint(imports, editorConfigOverride)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, editorConfigOverride)
        ).isEqualTo(formattedImports)
    }

    private fun getCustomImportOrderEditorConfigOverride(
        importsLayout: String
    ) = EditorConfigOverride.from(
        ImportOrderingRule.ideaImportsLayoutProperty to importsLayout
    )

    companion object {
        private val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered according to the pattern specified in .editorconfig"
            )
        )
    }
}
