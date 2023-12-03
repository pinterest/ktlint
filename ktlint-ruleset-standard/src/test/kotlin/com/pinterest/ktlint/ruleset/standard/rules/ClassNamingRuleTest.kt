package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ClassNamingRuleTest {
    private val classNamingRuleAssertThat =
        KtLintAssertThat.assertThatRule { ClassNamingRule() }

    @Nested
    inner class `Given a class declaration` {
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
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            classNamingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 7, "Class or object name should start with an uppercase letter and use camel case")
        }

        @Nested
        inner class `Given a class with name between backticks` {
            @Test
            fun `Given a file that does not import a class from the JUnit Jupiter Api then do emit`() {
                val code =
                    """
                    class `foo`
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                classNamingRuleAssertThat(code)
                    .hasLintViolationWithoutAutoCorrect(1, 7, "Class or object name should start with an uppercase letter and use camel case")
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

        @ParameterizedTest(name = "Suppression annotation: {0}")
        @ValueSource(
            strings = [
                "ktlint:standard:class-naming",
                "ClassName", // IntelliJ IDEA suppression
            ],
        )
        fun `Given class with a disallowed name which is suppressed`(suppressionName: String) {
            val code =
                """
                @Suppress("$suppressionName")
                class Foo_Bar
                """.trimIndent()
            classNamingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given an object declaration` {
        @Test
        fun `Given a valid object name then do not emit`() {
            val code =
                """
                object Foo
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
        fun `Given an invalid object name then do emit`(objectName: String) {
            val code =
                """
                object $objectName
                """.trimIndent()
            classNamingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(1, 8, "Class or object name should start with an uppercase letter and use camel case")
        }

        @ParameterizedTest(name = "Suppression annotation: {0}")
        @ValueSource(
            strings = [
                "ktlint:standard:class-naming",
                "ClassName", // IntelliJ IDEA suppression
            ],
        )
        fun `Given object with a disallowed name which is suppressed`(suppressionName: String) {
            val code =
                """
                @Suppress("$suppressionName")
                object Foo_Bar
                """.trimIndent()
            classNamingRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Issue 1757 - Given a class name containing diacritics is allowed`() {
        val code =
            """
            class ŸèśThîsIsAllowed123
            """.trimIndent()
        classNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = "Keyword: {0}")
    @Suppress("ktlint:standard:argument-list-wrapping")
    @ValueSource(
        strings = [
            "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion", "const", "constructor", "context",
            "continue", "contract", "crossinline", "data", "delegate", "do", "dynamic", "else", "enum", "expect", "external", "false",
            "field", "file", "final", "finally", "for", "fun", "get", "header", "if", "impl", "import", "in", "infix", "init", "inline",
            "inner", "interface", "internal", "is", "lateinit", "noinline", "null", "object", "open", "operator", "out", "override",
            "package", "param", "private", "property", "protected", "public", "receiver", "reified", "return", "sealed", "set", "setparam",
            "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "typeof", "val", "value", "var", "vararg", "when",
            "where", "while",
        ],
    )
    fun `Issue 2352 - Given a keyword then allow it to be wrapped between backticks`(keyword: String) {
        val code =
            """
            class `$keyword`
            """.trimIndent()
        classNamingRuleAssertThat(code).hasNoLintViolations()
    }
}
