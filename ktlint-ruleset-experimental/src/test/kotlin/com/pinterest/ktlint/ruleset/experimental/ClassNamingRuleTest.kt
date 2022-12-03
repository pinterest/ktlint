package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class `Given a class with name between backticks` {
        @Test
        fun `Given a file that does not import a class from the JUnit Jupiter Api then do emit`() {
            val code =
                """
                class `foo`
                """.trimIndent()
            classNamingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 7, "Class name should start with an uppercase letter and use camel case")
        }

        @Test
        fun `Given file which imports a class from the JUnit Jupiter Api class then do not emit`() {
            val code =
                """
                import org.junit.jupiter.api.Nested
                import org.junit.jupiter.api.Test

                class FunTest {
                    @Nested
                    inner class `Some descriptive class name` {
                        @Test
                        fun `Some descriptive test name`() {}
                    }
                }
                """.trimIndent()
            classNamingRuleAssertThat(code).hasNoLintViolations()
        }
    }
}
