package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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

    @Test
    fun `empty line between imports and aliases - ok`() {
        val emptyLineBeforeAliasPattern = mapOf("kotlin_imports_layout" to "*,|,^*")

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

        assertThat(ImportOrderingRule().lint(formattedImports, emptyLineBeforeAliasPattern)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, emptyLineBeforeAliasPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `empty line between imports and aliases - no empty line`() {
        val emptyLineBeforeAliasPattern = mapOf("kotlin_imports_layout" to "*,|,^*")

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

        assertThat(ImportOrderingRule().lint(imports, emptyLineBeforeAliasPattern)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, emptyLineBeforeAliasPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `empty line between imports and aliases - wrong order`() {
        val emptyLineBeforeAliasPattern = mapOf("kotlin_imports_layout" to "*,|,^*")

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
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import kotlin.concurrent.Thread

            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, emptyLineBeforeAliasPattern)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, emptyLineBeforeAliasPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `default idea java pattern - ok`() {
        val defaultIdeaJavaPattern = mapOf("kotlin_imports_layout" to "android.*,|,org.junit.*,|,net.*,|,org.*,|,java.*,|,com.*,|,javax.*,|,*")

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

        assertThat(ImportOrderingRule().lint(formattedImports, defaultIdeaJavaPattern)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, defaultIdeaJavaPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `default idea java pattern - wrong order`() {
        val defaultIdeaJavaPattern = mapOf("kotlin_imports_layout" to "android.*,|,org.junit.*,|,net.*,|,org.*,|,java.*,|,com.*,|,javax.*,|,*")

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

        assertThat(ImportOrderingRule().lint(imports, defaultIdeaJavaPattern)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, defaultIdeaJavaPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `multiple empty lines - ignored`() {
        val multipleEmptyLinesPattern = mapOf("kotlin_imports_layout" to "java.*,|,|,|,kotlin.*,*")

        val formattedImports =
            """
            import java.util.List

            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.app.Activity
            import android.view.View
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(formattedImports, multipleEmptyLinesPattern)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, multipleEmptyLinesPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `multiple empty lines - transformed into one`() {
        val multipleEmptyLinesPattern = mapOf("kotlin_imports_layout" to "java.*,|,|,|,kotlin.*,*")

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

        assertThat(ImportOrderingRule().lint(imports, multipleEmptyLinesPattern)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, multipleEmptyLinesPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `alias pattern - ok`() {
        val aliasPattern = mapOf("kotlin_imports_layout" to "^kotlin.*,^android.*,android.*,|,*,^*")

        val formattedImports =
            """
            import kotlin.io.Closeable as C
            import android.view.View as V
            import android.app.Activity

            import kotlin.concurrent.Thread
            import java.util.List as L
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(formattedImports, aliasPattern)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, aliasPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `alias pattern - wrong order`() {
        val aliasPattern = mapOf("kotlin_imports_layout" to "^kotlin.*,^android.*,android.*,|,*,^*")

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

            import kotlin.concurrent.Thread
            import java.util.List as L
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, aliasPattern)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, aliasPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `full path pattern - ok`() {
        val fullPathPattern = mapOf("kotlin_imports_layout" to "kotlin.io.Closeable,kotlin.*,*")

        val formattedImports =
            """
            import kotlin.io.Closeable
            import kotlin.concurrent.Thread
            import android.app.Activity
            import android.view.View
            import java.util.List
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(formattedImports, fullPathPattern)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, fullPathPattern)).isEqualTo(formattedImports)
    }

    @Test
    fun `full path pattern - wrong order`() {
        val fullPathPattern = mapOf("kotlin_imports_layout" to "kotlin.io.Closeable,kotlin.*,*")

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

        assertThat(ImportOrderingRule().lint(imports, fullPathPattern)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, fullPathPattern)).isEqualTo(formattedImports)
    }
}
