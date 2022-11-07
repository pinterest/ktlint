package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ClassNamingRuleTest {
    private val classNamingRuleAssertThat =
        KtLintAssertThat.assertThatRule { ClassNamingRule() }

    @Test
    fun `Given a valid class name then do not emit`() {
        val code =
            """
            class Foo1
            """.trimIndent()
        classNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "foo",
            "Foo_Bar",
        ],
    )
    fun `Given an invalid class name then do emit`(className: String) {
        val code =
            """
            class $className
            """.trimIndent()
        classNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 7, "Class name should start with an uppercase letter and use camel case")
    }
}
