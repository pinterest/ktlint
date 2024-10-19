package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.rules.EnumEntryNameCaseRule.Companion.ENUM_ENTRY_NAME_CASING_PROPERTY
import com.pinterest.ktlint.ruleset.standard.rules.EnumEntryNameCaseRule.Companion.EnumEntryNameCasing.camel_cases
import com.pinterest.ktlint.ruleset.standard.rules.EnumEntryNameCaseRule.Companion.EnumEntryNameCasing.upper_cases
import com.pinterest.ktlint.ruleset.standard.rules.EnumEntryNameCaseRule.Companion.EnumEntryNameCasing.upper_or_camel_cases
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EnumEntryNameCaseRuleTest {
    private val enumEntryNameCaseRuleAssertThat = assertThatRule { EnumEntryNameCaseRule() }

    @Test
    fun `Given enum values in upper-underscores notation are allowed`() {
        val code =
            """
            enum class SomeEnum {
                FOO,
                FOO_BAR,
                FOO__BAR,
                FOO_BAR_
            }
            """.trimIndent()
        enumEntryNameCaseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given enum values in upper-underscores notation and starting with underscore is not allowed`() {
        val code =
            """
            enum class SomeEnum {
                _FOO
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        enumEntryNameCaseRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\"")
    }

    @Test
    fun `Given enum values in Upper CamelCase notation are allowed`() {
        val code =
            """
            enum class SomeEnum {
                FooBar
            }
            """.trimIndent()
        enumEntryNameCaseRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given enum values in invalid formats should trigger lint errors`() {
        val code =
            """
            enum class FirstEnum {
                foo,
                bAr,
                Foo_Bar,
            }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        enumEntryNameCaseRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""),
                LintViolation(3, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""),
                LintViolation(4, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""),
            )
    }

    @Test
    fun `Issue 1530 - Given enum values containing diacritics are allowed`() {
        val code =
            """
            enum class SomeEnum {
                ŸÈŚ_THÎS_IS_ALLOWED_123,
                ŸèśThîsIsAllowed123,
            }
            """.trimIndent()
        enumEntryNameCaseRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Issue 2835 - Given enum entries in both upper cases and camel cases` {
        val code =
            """
            enum class SomeEnum {
                UPPER_CASE,
                CamelCase,
            }
            """.trimIndent()

        @Test
        fun `Given that 'ktlint_enum_entry_name_casing' is not set, then allow both upper cases and camel cases`() {
            enumEntryNameCaseRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given that 'ktlint_enum_entry_name_casing' is set to 'UPPER_OR_CAMEL_CASES', then allow both upper cases and camel cases`() {
            enumEntryNameCaseRuleAssertThat(code)
                .withEditorConfigOverride(ENUM_ENTRY_NAME_CASING_PROPERTY to upper_or_camel_cases)
                .hasNoLintViolations()
        }

        @Test
        fun `Given that 'ktlint_enum_entry_name_casing' is set to 'UPPER_CASES', then allow only upper cases`() {
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            enumEntryNameCaseRuleAssertThat(code)
                .withEditorConfigOverride(ENUM_ENTRY_NAME_CASING_PROPERTY to upper_cases)
                .hasLintViolationWithoutAutoCorrect(3, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\"")
        }

        @Test
        fun `Given that 'ktlint_enum_entry_name_casing' is set to 'CAMEL_CASES', then allow only camel cases`() {
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            enumEntryNameCaseRuleAssertThat(code)
                .withEditorConfigOverride(ENUM_ENTRY_NAME_CASING_PROPERTY to camel_cases)
                .hasLintViolationWithoutAutoCorrect(2, 5, "Enum entry name should be upper camel-case like \"EnumEntry\"")
        }
    }
}
