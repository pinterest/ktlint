package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PackageNameRuleTest {
    private val packageNameRuleAssertThat = assertThatRule { PackageNameRule() }

    @Test
    fun `Given a valid single level package name then do not emit`() {
        val code =
            """
            package foo
            """.trimIndent()
        packageNameRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a valid multi level package name then do not emit`() {
        val code =
            """
            package foo.foo
            """.trimIndent()
        packageNameRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single level package name containing an underscore`() {
        val code =
            """
            package foo_bar
            """.trimIndent()
        packageNameRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "Package name must not contain underscore")
    }

    @Test
    fun `Given a multi level package name containing an underscore`() {
        val code =
            """
            package foo.foo_bar
            """.trimIndent()
        packageNameRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "Package name must not contain underscore")
    }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "Foo",
            "foo.Foo",
            "`foo bar`",
            "foo.`foo bar`",
        ],
    )
    fun `Given a package name containing a forbidden character then do emit`(packageName: String) {
        val code =
            """
            package $packageName
            """.trimIndent()
        packageNameRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "Package name contains a disallowed character")
    }

    @ParameterizedTest(name = "Suppression annotation: {0}")
    @ValueSource(
        strings = [
            "ktlint:standard:package-name",
            "PackageName", // IntelliJ IDEA suppression
        ],
    )
    fun `Given class with a disallowed name which is suppressed`(suppressionName: String) {
        val code =
            """
            @file:Suppress("$suppressionName")
            package foo.foo_bar
            """.trimIndent()
        packageNameRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1757 - Given a package name containing diacritics then do no report a violation`() {
        val code =
            """
            package ÿèś.thîs.can.be.used
            """.trimIndent()
        packageNameRuleAssertThat(code).hasNoLintViolations()
    }
}
