package com.pinterest.ktlint.ruleset.standard.importordering

import com.pinterest.ktlint.ruleset.standard.ImportOrderingRule
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ImportOrderingRuleIdeaTest {
    private val importOrderingRuleAssertThat = assertThatRule { ImportOrderingRule() }

    @Test
    fun `Given some imports which are sorted incorrectly then do return lint errors`() {
        val code =
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
        val formattedCode =
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
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasLintViolation(
                1,
                1,
                "Imports must be ordered in lexicographic order without any empty lines in-between with \"java\", \"javax\", \"kotlin\" and aliases in the end",
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given imports which are sorted correctly then do not return lint errors`() {
        val code =
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
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasNoLintViolations()
    }

    @Test
    fun `Given some duplicate class imports unless they have distinct aliases`() {
        val code =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.view.View
            import android.content.Context as Ctx1
            import android.content.Context as Ctx2
            import android.content.Context as Ctx1
            """.trimIndent()
        val formattedCode =
            """
            import android.view.View
            import android.view.ViewGroup
            import android.content.Context as Ctx1
            import android.content.Context as Ctx2
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasLintViolations(
                LintViolation(3, 1, "Duplicate 'import android.view.View' found"),
                LintViolation(6, 1, "Duplicate 'import android.content.Context as Ctx1' found"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports in correct order and containing blank lines then do return a lint error`() {
        val code =
            """
            import android.app.Activity
            import android.view.View

            import android.view.ViewGroup
            import java.util.List
            """.trimIndent()
        val formattedCode =
            """
            import android.app.Activity
            import android.view.View
            import android.view.ViewGroup
            import java.util.List
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasLintViolation(
                1,
                1,
                "Imports must be ordered in lexicographic order without any empty lines in-between with \"java\", \"javax\", \"kotlin\" and aliases in the end",
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports having an EOL comment but in incorrect order then retain the comment and do return a lint error`() {
        val code =
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
        val formattedCode =
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
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasLintViolation(
                1,
                1,
                "Imports must be ordered in lexicographic order without any empty lines in-between with \"java\", \"javax\", \"kotlin\" and aliases in the end",
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some imports containing 'as' in the path then do not remove them`() {
        val code =
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
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasNoLintViolations()
    }

    @Test
    fun `Given some imports with aliases`() {
        val code =
            """
            import android.view.ViewGroup.LayoutParams.MATCH_PARENT as MATCH
            import android.view.ViewGroup.LayoutParams.WRAP_CONTENT as WRAP
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasNoLintViolations()
    }

    @Test
    fun `Issue 1243 - Format should not remove imports when having a distinct alias`() {
        val code =
            """
            import foo.Bar as Bar1
            import foo.Bar as Bar2
            import foo.Bar as Bar2

            val bar1 = Bar1()
            val bar2 = Bar2()
            """.trimIndent()
        val formattedCode =
            """
            import foo.Bar as Bar1
            import foo.Bar as Bar2

            val bar1 = Bar1()
            val bar2 = Bar2()
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .withEditorConfigOverride(IDEA_DEFAULT_IMPORT_ORDERING)
            .hasLintViolation(3, 1, "Duplicate 'import foo.Bar as Bar2' found")
            .isFormattedAs(formattedCode)
    }

    private companion object {
        val IDEA_DEFAULT_IMPORT_ORDERING = ImportOrderingRule.ideaImportsLayoutProperty to "*,java.**,javax.**,kotlin.**,^"
    }
}
