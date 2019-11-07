package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EnumEntryNameCaseRuleTest {

    @Test
    fun testFormatIsCorrect() {
        val formatted =
            """
            enum class FirstEnum {
                ENUM_ENTRY
            }
            enum class SecondEnum {
                EnumEntry
            }
            """.trimIndent()

        assertThat(EnumEntryNameCaseRule().lint(formatted)).isEmpty()
        assertThat(EnumEntryNameCaseRule().format(formatted)).isEqualTo(formatted)
    }

    @Test
    fun testFormat() {
        val unformatted =
            """
            enum class FirstEnum {
                enumEntry
            }
            enum class SecondEnum {
                enum_entry
            }
            """.trimIndent()
        val formatted =
            """
            enum class FirstEnum {
                ENUMENTRY
            }
            enum class SecondEnum {
                ENUM_ENTRY
            }
            """.trimIndent()

        assertThat(EnumEntryNameCaseRule().lint(unformatted)).isEqualTo(
            listOf(
                LintError(2, 5, "enum-entry-name-case", EnumEntryNameCaseRule.ERROR_MESSAGE),
                LintError(5, 5, "enum-entry-name-case", EnumEntryNameCaseRule.ERROR_MESSAGE)
            )
        )
        assertThat(EnumEntryNameCaseRule().format(unformatted)).isEqualTo(formatted)
    }
}
