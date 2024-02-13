package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.android_studio
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.intellij_idea
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.KtlintDocumentationTest
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class BackingPropertyRuleTest {
    private val backingPropertyNamingRuleAssertThat = assertThatRule { BackingPropertyNamingRule() }

    @Nested
    inner class `Given a backing property correlating with a property` {
        @ParameterizedTest(name = "Correlated property name: {0}")
        @ValueSource(
            strings = [
                "foo",
                "føø",
            ],
        )
        fun `Given a valid property name then do not emit`(propertyName: String) {
            val code =
                """
                class Foo {
                    private var _$propertyName = "some-value"

                    val $propertyName: String
                        get() = _$propertyName
                }
                """.trimIndent()
            backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
        }

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
            backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
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
            backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
        }

        @ParameterizedTest(name = "Modifier: {0}")
        @ValueSource(
            strings = [
                "private",
                "protected",
                "internal",
            ],
        )
        fun `Given ktlint_official code style, and the correlated property is non-public then emit`(modifier: String) {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    $modifier val elementList: List<Element>
                        get() = _elementList
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            backingPropertyNamingRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property is only allowed when the matching property or function is public")
        }

        @ParameterizedTest(name = "Modifier: {0}")
        @ValueSource(
            strings = [
                "private",
                "protected",
                "internal",
            ],
        )
        fun `Given intellij_idea code style, and the correlated property is non-public then emit`(modifier: String) {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    $modifier val elementList: List<Element>
                        get() = _elementList
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            backingPropertyNamingRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to intellij_idea)
                .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property is only allowed when the matching property or function is public")
        }

        @ParameterizedTest(name = "Modifier: {0}")
        @ValueSource(
            strings = [
                "private",
                "protected",
                "internal",
            ],
        )
        fun `Given android_studio code style, and the correlated property is non-public then do not emit`(modifier: String) {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    $modifier val elementList: List<Element>
                        get() = _elementList
                }
                """.trimIndent()
            backingPropertyNamingRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to android_studio)
                .hasNoLintViolations()
        }
    }

    @Nested
    inner class `Given a backing property correlating with a function` {
        @ParameterizedTest(name = "Correlated property name: {0}")
        @CsvSource(
            value = [
                "foo,getFoo",
                "føø,getFøø",
            ],
        )
        fun `Given a valid backing property then do not emit`(
            propertyName: String,
            functionName: String,
        ) {
            val code =
                """
                class Foo {
                    private var _$propertyName = "some-value"

                    fun $functionName(): String = _$propertyName
                }
                """.trimIndent()
            backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given that the correlated function is implicitly public then do not emit`() {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    fun getElementList(): List<Element> = _elementList
                }
                """.trimIndent()
            backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
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
            backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
        }

        @ParameterizedTest(name = "Modifier: {0}")
        @ValueSource(
            strings = [
                "private",
                "protected",
                "internal",
            ],
        )
        fun `Given ktlint_official code style, and the correlated function is non-public then emit`(modifier: String) {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    $modifier fun getElementList(): List<Element> = _elementList
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            backingPropertyNamingRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to ktlint_official)
                .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property is only allowed when the matching property or function is public")
        }

        @ParameterizedTest(name = "Modifier: {0}")
        @ValueSource(
            strings = [
                "private",
                "protected",
                "internal",
            ],
        )
        fun `Given intellij_idea code style, and the correlated function is non-public then emit`(modifier: String) {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    $modifier fun getElementList(): List<Element> = _elementList
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            backingPropertyNamingRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to intellij_idea)
                .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property is only allowed when the matching property or function is public")
        }

        @ParameterizedTest(name = "Modifier: {0}")
        @ValueSource(
            strings = [
                "private",
                "protected",
                "internal",
            ],
        )
        fun `Given android_studio code style, and the correlated function is non-public then do not emit`(modifier: String) {
            val code =
                """
                class Foo {
                    private val _elementList = mutableListOf<Element>()

                    $modifier fun getElementList(): List<Element> = _elementList
                }
                """.trimIndent()
            backingPropertyNamingRuleAssertThat(code)
                .withEditorConfigOverride(CODE_STYLE_PROPERTY to android_studio)
                .hasNoLintViolations()
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
            backingPropertyNamingRuleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(2, 17, "Backing property is only allowed when a matching property or function exists")
        }
    }

    @ParameterizedTest(name = "Suppression annotation: {0}")
    @ValueSource(
        strings = [
            "ktlint:standard:backing-property-naming",
            "PropertyName", // IntelliJ IDEA suppression
        ],
    )
    fun `Given class with a disallowed name which is suppressed`(suppressionName: String) {
        val code =
            """
            @Suppress("$suppressionName")
            val foo = Foo()
            """.trimIndent()
        backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @KtlintDocumentationTest
    fun `Ktlint allowed examples`() {
        val code =
            """
            class Bar {
                // Backing property
                private val _elementList = mutableListOf<Element>()
                val elementList: List<Element>
                    get() = _elementList
            }
            """.trimIndent()
        backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
    }

    @KtlintDocumentationTest
    fun `Ktlint disallowed examples`() {
        val code =
            """
            class Bar1 {
                // Incomplete backing property as public property 'elementList' or function `getElementList` is missing
                private val _elementList = mutableListOf<Element>()
            }
            class Bar2 {
                // Invalid backing property as '_elementList' is not a private property
                val _elementList = mutableListOf<Element>()
                val elementList: List<Element>
                    get() = _elementList2
            }
            class Bar3 {
                // Invalid backing property as 'elementList' is not a public property
                // Note: code below is allowed in `android_studio` code style!
                private val _elementList = mutableListOf<Element>()
                internal val elementList: List<Element>
                    get() = _elementList2
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        backingPropertyNamingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 17, "Backing property is only allowed when a matching property or function exists", canBeAutoCorrected = false),
                LintViolation(7, 9, "Backing property not allowed when 'private' modifier is missing", canBeAutoCorrected = false),
                LintViolation(14, 17, "Backing property is only allowed when the matching property or function is public", canBeAutoCorrected = false),
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
        backingPropertyNamingRuleAssertThat(code).hasNoLintViolations()
    }
}
