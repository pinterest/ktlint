package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PropertyNamingRuleTest {
    private val propertyNamingRuleAssertThat =
        KtLintAssertThat.assertThatRule { PropertyNamingRule() }

    @Test
    fun `Given a valid property name then do not emit`() {
        val code =
            """
            var foo = "foo"
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
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        propertyNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 11, "Property name should use the screaming snake case notation when the value can not be changed")
    }

    @Test
    fun `Given a top level val property name not in screaming case notation then do emit`() {
        val code =
            """
            val foo = Foo()
            val FOO_BAR = FooBar()
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        propertyNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 5, "Property name should use the screaming snake case notation when the value can not be changed")
    }

    @Test
    fun `Given an object val property name not having a custom get function and not in screaming case notation then do emit`() {
        val code =
            """
            class Foo {
                companion object {
                    val foo = Foo()
                    val FOO_BAR = FooBar()
                }
            }
            """.trimIndent()
        @Suppress("ktlint:argument-list-wrapping", "ktlint:max-line-length")
        propertyNamingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(3, 13, "Property name should use the screaming snake case notation when the value can not be changed")
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
    fun `Given a backing val property name having a custom get function and not in screaming case notation then do not emit`() {
        val code =
            """
            class Foo {
                private val _elementList = mutableListOf<Element>()

                val elementList: List<Element>
                    get() = _elementList
            }
            """.trimIndent()
        propertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a backing val property name containing diacritics having a custom get function and not in screaming case notation then do not emit`() {
        val code =
            """
            class Foo {
                private val _elementŁîšt = mutableListOf<Element>()

                val elementŁîšt: List<Element>
                    get() = _elementList
            }
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
            "ktlint:property-naming",
            "PropertyName", // IntelliJ IDEA suppression
        ],
    )
    fun `Given class with a disallowed name which is suppressed`(suppressionName: String) {
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
}
