package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImportOrderingRuleIdeaTest {

    companion object {
        private val userData = mapOf("kotlin_imports_layout" to "idea")

        private val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between with \"java\", \"javax\", \"kotlin\" and aliases in the end"
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

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatOk() {
        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            import java.util.List
            import javax.net.ssl.SSLHandshakeException
            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(formattedImports, userData)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatWrongOrder() {
        val imports =
            """
            import android.app.Activity
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            import javax.net.ssl.SSLHandshakeException
            import kotlin.io.Closeable
            import kotlin.concurrent.Thread
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            import java.util.List
            import javax.net.ssl.SSLHandshakeException
            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatDuplicate() {
        val imports =
            """
            import android.view.ViewGroup
            import android.view.View
            import android.view.ViewGroup
            import android.content.Context as Ctx1
            import android.content.Context as Ctx2
            """.trimIndent()

        val formattedImports =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.content.Context as Ctx1
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatWrongOrderAndBlankLines() {
        val imports =
            """
            import android.app.Activity



            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            import android.view.View


            import android.view.ViewGroup
            import java.util.List
            import javax.net.ssl.SSLHandshakeException

            import kotlin.io.Closeable

            import kotlin.concurrent.Thread
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            import java.util.List
            import javax.net.ssl.SSLHandshakeException
            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatBlankLines() {
        val imports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup


            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            import java.util.List
            import javax.net.ssl.SSLHandshakeException
            import kotlin.concurrent.Thread

            import kotlin.io.Closeable


            import android.content.Context as Ctx

            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            import java.util.List
            import javax.net.ssl.SSLHandshakeException
            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun testFormatImportsWithEOLComments() {
        val imports =
            """
            import android.app.Activity
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F // comment 3
            import android.view.ViewGroup
            import android.view.View // comment 1
            import java.util.List // comment 2
            import javax.net.ssl.SSLHandshakeException
            import kotlin.io.Closeable
            import kotlin.concurrent.Thread
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            """.trimIndent()

        val formattedImports =
            """
            import android.app.Activity
            import android.view.View // comment 1
            import android.view.ViewGroup
            import kotlinx.coroutines.CoroutineDispatcher
            import ru.example.a
            import java.util.List // comment 2
            import javax.net.ssl.SSLHandshakeException
            import kotlin.concurrent.Thread
            import kotlin.io.Closeable
            import android.content.Context as Ctx
            import androidx.fragment.app.Fragment as F // comment 3
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(imports, userData)).isEqualTo(expectedErrors)
        assertThat(ImportOrderingRule().format(imports, userData)).isEqualTo(formattedImports)
    }
}
