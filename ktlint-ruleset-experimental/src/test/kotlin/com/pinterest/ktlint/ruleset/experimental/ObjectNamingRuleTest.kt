package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ObjectNamingRuleTest {
    private val objectNamingRuleAssertThat =
        KtLintAssertThat.assertThatRule { ObjectNamingRule() }

    @Test
    fun `Given a valid class name then do not emit`() {
        val code =
            """
            object Foo
            """.trimIndent()
        objectNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "foo",
            "Foo1",
            "Foo_Bar",
        ],
    )
    fun `Given an invalid object name then do emit`(className: String) {
        val code =
            """
            object $className
            """.trimIndent()
        objectNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 8, "Object name should start with an uppercase letter and use camel case")
    }
}
