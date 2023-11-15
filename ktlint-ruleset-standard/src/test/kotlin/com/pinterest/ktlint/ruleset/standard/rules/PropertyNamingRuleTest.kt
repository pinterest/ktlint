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

    @Nested
    inner class `Given a property name starting with '_', and not in screaming case notation` {
        @Nested
        inner class `Given that a correlated property exists` {
            @Test
            fun `Given that the correlated property is implicitly public then do not emit`() {
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
            fun `Given that the correlated property is explicitly public then do not emit`() {
                val code =
                    """
                    class Foo {
                        private val _elementList = mutableListOf<Element>()

                        public val elementList: List<Element>
                            get() = _elementList
                    }
                    """.trimIndent()
                propertyNamingRuleAssertThat(code).hasNoLintViolations()
            }

            @Test
            fun `Given that the backing and correlated property contain diacritics then do not emit`() {
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

            @ParameterizedTest(name = "Modifier: {0}")
            @ValueSource(
                strings = [
                    "private",
                    "protected",
                ],
            )
            fun `Given that correlated property is non-public then emit`(modifier: String) {
                val code =
                    """
                    class Foo {
                        private val _elementList = mutableListOf<Element>()

                        $modifier val elementList: List<Element>
                            get() = _elementList
                    }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                propertyNamingRuleAssertThat(code)
                    .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property name is only allowed when a matching public property or function exists")
            }
        }

        @Nested
        inner class `Given that a correlated function exists` {
            @Test
            fun `Given that the correlated function is implicitly public then do not emit`() {
                val code =
                    """
                    class Foo {
                        private val _elementList = mutableListOf<Element>()

                        fun getElementList(): List<Element> = _elementList
                    }
                    """.trimIndent()
                propertyNamingRuleAssertThat(code).hasNoLintViolations()
            }

            @Test
            fun `Given that the correlated function is explicitly public then do not emit`() {
                val code =
                    """
                    class Foo {
                        private val _elementList = mutableListOf<Element>()

                        public fun getElementList(): List<Element> = _elementList
                    }
                    """.trimIndent()
                propertyNamingRuleAssertThat(code).hasNoLintViolations()
            }

            @Test
            fun `Given that the backing and correlated function contain diacritics then do not emit`() {
                val code =
                    """
                    class Foo {
                        private val _ëlementŁîšt = mutableListOf<Element>()

                        fun getËlementŁîšt(): List<Element> = _elementList
                    }
                    """.trimIndent()
                propertyNamingRuleAssertThat(code).hasNoLintViolations()
            }

            @ParameterizedTest(name = "Modifier: {0}")
            @ValueSource(
                strings = [
                    "private",
                    "protected",
                    "internal",
                ],
            )
            fun `Given that correlated function is non-public then emit`(modifier: String) {
                val code =
                    """
                    class Foo {
                        private val _elementList = mutableListOf<Element>()

                        $modifier fun getElementList(): List<Element> = _elementList
                    }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                propertyNamingRuleAssertThat(code)
                    .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property name is only allowed when a matching public property or function exists")
            }

            @Test
            fun `Given that the correlated function has at least 1 parameter then emit`() {
                val code =
                    """
                    class Foo {
                        private val _elementList = mutableListOf<Element>()

                        fun getElementList(bar: String): List<Element> = _elementList + bar
                    }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                propertyNamingRuleAssertThat(code)
                    .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property name is only allowed when a matching public property or function exists")
            }
        }
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

                // Backing property
                private val _elementList = mutableListOf<Element>()
                val elementList: List<Element>
                    get() = _elementList

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

                // Incomplete backing property as public property 'elementList1' is missing
                private val _elementList1 = mutableListOf<Element>()

                // Invalid backing property as '_elementList2' is not a private property
                val _elementList2 = mutableListOf<Element>()
                val elementList2: List<Element>
                    get() = _elementList2
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        propertyNamingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 11, "Property name should use the screaming snake case notation when the value can not be changed", canBeAutoCorrected = false),
                LintViolation(3, 5, "Property name should start with a lowercase letter and use camel case", canBeAutoCorrected = false),
                LintViolation(6, 9, "Property name should start with a lowercase letter and use camel case", canBeAutoCorrected = false),
                LintViolation(9, 17, "Backing property name is only allowed when a matching public property or function exists", canBeAutoCorrected = false),
                LintViolation(12, 9, "Backing property name not allowed when 'private' modifier is missing", canBeAutoCorrected = false),
            )
    }
}
