package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class EnumEntryNameCaseRuleTest {
    private val enumEntryNameCaseRuleAssertThat = EnumEntryNameCaseRule().assertThat()

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
        enumEntryNameCaseRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(2, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""),
                LintViolation(3, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""),
                LintViolation(4, 5, "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\"")
            )
    }
}
