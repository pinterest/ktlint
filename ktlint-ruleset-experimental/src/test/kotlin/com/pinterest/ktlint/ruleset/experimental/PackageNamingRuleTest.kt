package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PackageNamingRuleTest {
    private val packageNamingRuleAssertThat =
        KtLintAssertThat.assertThatRule { PackageNamingRule() }

    @Test
    fun `Given a valid single level package name then do not emit`() {
        val code =
            """
            package foo
            """.trimIndent()
        packageNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a valid multi level package name then do not emit`() {
        val code =
            """
            package foo.foo
            """.trimIndent()
        packageNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "Foo",
            "foo.Foo",
            "foo_bar",
            "foo.foo_bar",
            "foo1",
            "foo.foo1",
        ],
    )
    fun `Given a package name containing a non-lowercase characters then do emit`(packageName: String) {
        val code =
            """
            package $packageName
            """.trimIndent()
        packageNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 9, "Package name should contain lowercase characters only")
    }

    @Test
    fun `Given a package name with containing a non-lowercase characters which is suppressed then do not emit`() {
        val code =
            """
            @file:Suppress("ktlint:experimental:package-naming")
            package foo.fooBar
            """.trimIndent()
        packageNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a package name with containing a non-lowercase characters which is suppressed via ktlint directive in comment then do not emit`() {
        val code =
            """
            package foo.fooBar // ktlint-disable experimental:package-naming
            """.trimIndent()
        packageNamingRuleAssertThat(code).hasNoLintViolations()
    }
}
