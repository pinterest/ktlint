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
class ImportOrderingRuleIdeaTest {
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
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
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

        assertThat(
            rule.lint(formattedImports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
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

        assertThat(
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `remove duplicate class imports unless they have distinct aliases`() {
        val imports =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.view.View
            import android.content.Context as Ctx1
            import android.content.Context as Ctx2
            import android.content.Context as Ctx1
            """.trimIndent()

        val formattedImports =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.content.Context as Ctx1
            import android.content.Context as Ctx2
            """.trimIndent()

        assertThat(
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).containsExactly(
            LintError(3, 1, "import-ordering", "Duplicate 'import android.view.View' found"),
            LintError(6, 1, "import-ordering", "Duplicate 'import android.content.Context as Ctx1' found")
        )
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
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

        assertThat(
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
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

        assertThat(
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
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

        assertThat(
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(expectedErrors)
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
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

        assertThat(
            rule.lint(formattedImports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `formats aliases correctly`() {
        val formattedImports =
            """
            import android.view.ViewGroup.LayoutParams.MATCH_PARENT as MATCH
            import android.view.ViewGroup.LayoutParams.WRAP_CONTENT as WRAP
            """.trimIndent()

        assertThat(
            rule.lint(formattedImports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEmpty()
        assertThat(
            rule.format(formattedImports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    @Test
    fun `Issue 1243 - Format should not remove imports when having a distinct alias`() {
        val imports =
            """
            import foo.Bar as Bar1
            import foo.Bar as Bar2
            import foo.Bar as Bar2

            val bar1 = Bar1()
            val bar2 = Bar2()
            """.trimIndent()
        val formattedImports =
            """
            import foo.Bar as Bar1
            import foo.Bar as Bar2

            val bar1 = Bar1()
            val bar2 = Bar2()
            """.trimIndent()

        assertThat(
            rule.lint(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).containsExactly(
            LintError(3, 1, "import-ordering", "Duplicate 'import foo.Bar as Bar2' found")
        )
        assertThat(
            rule.format(imports, IDEA_DEFAULT_IMPORT_ORDERING)
        ).isEqualTo(formattedImports)
    }

    private companion object {
        val expectedErrors = listOf(
            LintError(
                1,
                1,
                "import-ordering",
                "Imports must be ordered in lexicographic order without any empty lines in-between with \"java\", \"javax\", \"kotlin\" and aliases in the end"
            )
        )

        val IDEA_DEFAULT_IMPORT_ORDERING = EditorConfigOverride.from(
            ImportOrderingRule.ideaImportsLayoutProperty to "*,java.**,javax.**,kotlin.**,^"
        )
    }
}
