package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.KtlintDocumentationTest
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PropertyNamingRuleTest {
    private val propertyNamingRuleAssertThat = assertThatRule { PropertyNamingRule() }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "foo",
            "føø",
            "_foo",
            "_føø",
        ],
    )
    fun `Given an valid property name then do not emit`(propertyName: String) {
        val code =
            """
            var $propertyName = "foo"
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = ": {0}")
    @ValueSource(
        strings = [
            "Foo",
            "foo_bar",
        ],
    )
    fun `Given an invalid property name then do emit`(propertyName: String) {
        val code =
            """
            var $propertyName = "foo"
            """.trimIndent()
        propertyNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 5, "Property name should start with a lowercase letter and use camel case")
    }

    @Test
    fun `Given a const property name not in screaming case notation then do emit`() {
        val code =
            """
            const val foo = "foo"
            const val FOO_BAR_2 = "foo-bar-2"
            const val ŸÈŠ_THÎS_IS_ALLOWED_123 = "Yes this is allowed"
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        propertyNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 11, "Property name should use the screaming snake case notation when the value can not be changed")
    }

    @Test
    fun `Issue 2140 - Given a top level val property name not in screaming case notation then do not emit as it can not be reliably be determined whether the value is (deeply) immutable`() {
        val code =
            """
            val foo = Foo()
            val FOO_BAR = FooBar()
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2140 - Given an object val property name not having a custom get function and not in screaming case notation then do not emit as it can not be reliably be determined whether the value is (deeply) immutable`() {
        val code =
            """
            class Foo {
                companion object {
                    val foo = Foo()
                    val FOO_BAR = FooBar()
                }
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an object override val property name not having a custom get function and not in screaming case notation then do not emit`() {
        val code =
            """
            open class Foo {
                open val foo = "foo"
            }

            val BAR = object : Foo() {
                override val foo = "bar"
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an object val property name having a custom get function and not in screaming case notation then do not emit`() {
        val code =
            """
            class Foo {
                companion object {
                    val foo
                        get() = foobar() // Lint can not check whether data is immutable
                }
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a top level property extension function having a custom get function and not in screaming case notation then do not emit`() {
        val code =
            """
            val fooBar1: Any
                get() = foobar() // Lint can not check whether data is immutable
            val fooBar2
                inline get() = foobar() // Lint can not check whether data is immutable
            val fooBar3
                @Bar get() = foobar() // Lint can not check whether data is immutable
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a local variable then do not emit`() {
        val code =
            """
            fun foo() {
                val bar2 = "bar"
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @ParameterizedTest(name = "Suppression annotation: {0}")
    @ValueSource(
        strings = [
            "ktlint:standard:property-naming",
            "PropertyName", // IntelliJ IDEA suppression
        ],
    )
    fun `Given a function with a disallowed name which is suppressed`(suppressionName: String) {
        val code =
            """
            @Suppress("$suppressionName")
            val foo = Foo()
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Issue 2017 - Given property is serialVersionUID` {
        @Test
        fun `Given property is present in companion object`() {
            val code =
                """
                class Foo1 {
                    companion object {
                        private const val serialVersionUID: Long = 123
                    }
                }
                class Foo2 {
                    companion object {
                        private const val serialVersionUID = 123L
                    }
                }
                """.trimIndent()
            propertyNamingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given property defined object is private const`() {
            val code =
                """
                object Foo1 {
                    private const val serialVersionUID: Long = 123
                }
                object Foo2 {
                    private const val serialVersionUID = 123L
                }
                """.trimIndent()
            propertyNamingRuleAssertThat(code).hasNoLintViolations()
        }
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
            var `$keyword` = "some-value"
            fun foo() {
                var `$keyword` = "some-value"
                val `$keyword` = "some-value"
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @KtlintDocumentationTest
    fun `Ktlint allowed examples`() {
        val code =
            """
            val foo1 = Foo() // In case developer want to communicate that Foo is mutable
            val FOO1 = Foo() // In case developer want to communicate that Foo is deeply immutable

            const val FOO_BAR = "FOO-BAR" // By definition deeply immutable

            var foo2: Foo = Foo() // By definition not immutable

            class Bar {
                val foo1 = "foo1" // Class properties always start with lowercase, const is not allowed

                const val FOO_BAR = "FOO-BAR" // By definition deeply immutable

                var foo2: Foo = Foo() // By definition not immutable

                companion object {
                    val foo1 = Foo() // In case developer want to communicate that Foo is mutable
                    val FOO1 = Foo() // In case developer want to communicate that Foo is deeply immutable
                }
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @KtlintDocumentationTest
    fun `Ktlint disallowed examples`() {
        val code =
            """
            const val fooBar = "FOO-BAR" // By definition deeply immutable

            var FOO2: Foo = Foo() // By definition not immutable

            class Bar {
                val FOO_BAR = "FOO-BAR" // Class properties always start with lowercase, const is not allowed
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        propertyNamingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Property name should use the screaming snake case notation when the value can not be changed", canBeAutoCorrected = false),
                LintViolation(3, 5, "Property name should start with a lowercase letter and use camel case", canBeAutoCorrected = false),
                LintViolation(6, 9, "Property name should start with a lowercase letter and use camel case", canBeAutoCorrected = false),
            )
    }

    @Test
    fun `Given a property name suppressed via 'PropertyName' then also suppress the ktlint violation`() {
        val code =
            """
            class Foo {
                @Suppress("PropertyName")
                var FOO = "foo"
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2441 - Given a property name suppressed via 'ConstPropertyName' then also suppress the ktlint violation`() {
        val code =
            """
            interface Bar {
                companion object {
                    @Suppress("ConstPropertyName")
                    const val bar: String = ""
                }
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2612 - Given a property name suppressed via 'PrivatePropertyName' then also suppress the ktlint violation`() {
        val code =
            """
            class Foo {
                @Suppress("PrivatePropertyName")
                private val Bar = "Bar"
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2612 - Given a property name suppressed via 'ObjectPropertyName' then also suppress the ktlint violation`() {
        val code =
            """
            @Suppress("ObjectPropertyName")
            private const val _Foo_Bar = ""
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }
}
