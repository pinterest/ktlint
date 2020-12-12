package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.EditorConfigTestRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(FeatureInAlphaState::class)
class ImportOrderingRuleCustomTest {

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

    @get:Rule
    val editorConfigTestRule = EditorConfigTestRule()

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

        val testFile = writeCustomImportsOrderingConfig("^,|,*")

        assertThat(
            rule.lint(testFile, formattedImports)
        ).isEmpty()
        assertThat(
            rule.format(testFile, formattedImports)
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

        val testFile = writeCustomImportsOrderingConfig("*,|,^")

        assertThat(
            rule.lint(testFile, imports)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(testFile, imports)
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

        val testFile = writeCustomImportsOrderingConfig("^,|,*")

        assertThat(
            rule.lint(testFile, imports)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(testFile, imports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "android.**,|,org.junit.**,|,net.**,|,org.**,|,java.**,|,com.**,|,javax.**,|,*"
        )

        assertThat(
            rule.lint(testFile, formattedImports)
        ).isEmpty()
        assertThat(
            rule.format(testFile, formattedImports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "android.**,|,org.junit.**,|,net.**,|,org.**,|,java.**,|,com.**,|,javax.**,|,*"
        )

        assertThat(
            rule.lint(testFile, imports)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(testFile, imports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "java.**,|,|,|,kotlin.**,*"
        )

        assertThat(
            rule.lint(testFile, formattedImports)
        ).isEmpty()
        assertThat(
            rule.format(testFile, formattedImports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "java.**,|,|,|,kotlin.**,*"
        )

        assertThat(
            rule.lint(testFile, imports)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(testFile, imports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "^kotlin.**,^android.**,android.**,|,*,^"
        )

        assertThat(
            rule.lint(testFile, formattedImports)
        ).isEmpty()
        assertThat(
            rule.format(testFile, formattedImports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "^kotlin.**,^android.**,android.**,|,^,*"
        )

        assertThat(
            rule.lint(testFile, imports)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(testFile, imports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "kotlin.io.Closeable.*,kotlin.**,*"
        )

        assertThat(
            rule.lint(testFile, formattedImports)
        ).isEmpty()
        assertThat(
            rule.format(testFile, formattedImports)
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

        val testFile = writeCustomImportsOrderingConfig(
            "kotlin.io.Closeable.*,kotlin.**,*"
        )

        assertThat(
            rule.lint(testFile, imports)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(testFile, imports)
        ).isEqualTo(formattedImports)
    }

    private fun writeCustomImportsOrderingConfig(
        importsLayout: String
    ) = editorConfigTestRule
        .writeToEditorConfig(
            mapOf(
                ImportOrderingRule.ideaImportsLayoutProperty.type to importsLayout
            )
        )
        .absolutePath
}
