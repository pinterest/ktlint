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

    @Test
    fun `format imports containing "as" in the path - does not remove them`() {
        val formattedImports =
            """
            import assertk.all
            import assertk.assertThat
            import assertk.assertions.contains
            import assertk.assertions.doesNotContain
            import assertk.assertions.isEqualTo
            import assertk.assertions.isFailure
            import assertk.assertions.isInstanceOf
            import assertk.assertions.isNotNull
            import assertk.assertions.isNull
            import assertk.assertions.message
            import com.fasterxml.jackson.annotation.JsonIgnore
            import com.fasterxml.jackson.databind.JsonMappingException
            import com.fasterxml.jackson.databind.ObjectMapper
            import com.fasterxml.jackson.module.kotlin.readValue
            import com.google.inject.multibindings.MapBinder
            import com.google.inject.name.Names
            import com.trib3.json.modules.ObjectMapperModule
            import dev.misfitlabs.kotlinguice4.KotlinModule
            import dev.misfitlabs.kotlinguice4.typeLiteral
            import org.testng.annotations.Guice
            import org.testng.annotations.Test
            import org.threeten.extra.YearQuarter
            import java.time.LocalDate
            import javax.inject.Inject
            import kotlin.reflect.KClass
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(formattedImports, userData)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, userData)).isEqualTo(formattedImports)
    }

    @Test
    fun `formats aliases correctly`() {
        val formattedImports =
            """
            import android.view.ViewGroup.LayoutParams.MATCH_PARENT as MATCH
            import android.view.ViewGroup.LayoutParams.WRAP_CONTENT as WRAP
            """.trimIndent()

        assertThat(ImportOrderingRule().lint(formattedImports, userData)).isEmpty()
        assertThat(ImportOrderingRule().format(formattedImports, userData)).isEqualTo(formattedImports)
    }
}
