package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FunctionNamingRuleTest {
    private val functionNamingRuleAssertThat =
        KtLintAssertThat.assertThatRule { FunctionNamingRule() }

    @Test
    fun `Given a valid function name then do not emit`() {
        val code =
            """
            fun foo1() = "foo"
            """.trimIndent()
        functionNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a factory method then do not emit`() {
        val code =
            """
            interface Foo
            class FooImpl : Foo
            fun Foo(): Foo = FooImpl()
            fun Bar.Foo(): Foo = FooImpl()
            """.trimIndent()
        functionNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given a function name between backticks` {
        @Test
        fun `Given a file that does not import a class from the JUnit Jupiter then do emit`() {
            val code =
                """
                fun `Some name`() {}
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            functionNamingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 5, "Function name should start with a lowercase letter (except factory methods) and use camel case")
        }

        @ParameterizedTest(name = "Junit import: {0}")
        @ValueSource(
            strings = [
                "org.junit.jupiter.api.Test",
                "org.junit.jupiter.api.*",
                "org.junit.jupiter.*",
                "org.junit.*",
            ],
        )
        fun `Given file which imports a class from the JUnit Jupiter then do not emit`(import: String) {
            val code =
                """
                import $import

                class FunTest {
                    @Test
                    fun `Some descriptive test name`() {}
                }
                """.trimIndent()
            functionNamingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a function name containing an underscore` {
        @Test
        fun `Given a file that does not import a class from the JUnit Jupiter then do emit`() {
            val code =
                """
                fun do_something() {}
                """.trimIndent()
            @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
            functionNamingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 5, "Function name should start with a lowercase letter (except factory methods) and use camel case")
        }

        @ParameterizedTest(name = "Junit import: {0}")
        @ValueSource(
            strings = [
                "org.junit.jupiter.api.Test",
                "org.junit.jupiter.api.*",
                "org.junit.jupiter.*",
                "org.junit.*",
                "kotlin.test.*",
                "org.testng.*",
            ],
        )
        fun `Given file which imports a class from the JUnit Jupiter then do not emit`(import: String) {
            val code =
                """
                import $import

                class FunTest {
                    @Test
                    fun givenSomeCondition_whenSomeAction_thenExpectation() {}
                }
                """.trimIndent()
            functionNamingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "Foo",
            "Foo_Bar",
        ],
    )
    fun `Given an invalid function name then do emit`(functionName: String) {
        val code =
            """
            fun $functionName() = "foo"
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        functionNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 5, "Function name should start with a lowercase letter (except factory methods) and use camel case")
    }

    @ParameterizedTest(name = "Suppression annotation: {0}")
    @ValueSource(
        strings = [
            "ktlint:function-naming",
            "FunctionName", // IntelliJ IDEA suppression
        ],
    )
    fun `Given method with a disallowed name which is suppressed`(suppressionName: String) {
        val code =
            """
            @Suppress("$suppressionName")
            fun Foo() {}
            """.trimIndent()
        functionNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1757 - Given a function name containing diacritics then do not report a violation on the diacritics`() {
        val code =
            """
            fun ÿèśThîsIsAllowed123() = "foo"
            """.trimIndent()
        functionNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = "Junit import: {0}")
    @ValueSource(
        strings = [
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.*",
            "org.junit.jupiter.*",
            "org.junit.*",
            "kotlin.test.*",
            "org.testng.*",
        ],
    )
    fun `Given file which imports a class from the JUnit Jupiter then do not emit`(import: String) {
        val code =
            """
            import $import

            class FunTest {
                @Test
                fun `Ÿèś thîs is allowed 123`() {}
            }
            """.trimIndent()
        functionNamingRuleAssertThat(code).hasNoLintViolations()
    }
}
