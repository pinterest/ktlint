package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class PackageNameRuleTest {
    private val packageNameRuleAssertThat = assertThatRule { PackageNameRule() }

    @Test
    fun `Given a package name containing an underscore`() {
        val code =
            """
            package foo.bar.foo_bar
            """.trimIndent()
        packageNameRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 1, "Package name must not contain underscore")
    }
}
