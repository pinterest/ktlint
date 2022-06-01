package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class PackageNameRuleTest {
    private val packageNameRuleAssertThat = PackageNameRule().assertThat()

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
