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
            enum class NetworkInfo {
                WiFi, Mobile
            }
            """.trimIndent()

        assertThat(EnumEntryNameCaseRule().lint(formatted)).isEmpty()
        assertThat(EnumEntryNameCaseRule().format(formatted)).isEqualTo(formatted)
    }

    @Test
    fun `invalid formats should trigger lint errors`() {
        val formatted =
            """
            enum class FirstEnum {
                helloWorld,
                ALMOST_xVALID
            }
            """.trimIndent()
        assertThat(EnumEntryNameCaseRule().lint(formatted))
            .isEqualTo(
                listOf(
                    LintError(
                        2,
                        5,
                        "enum-entry-name-case",
                        EnumEntryNameCaseRule.ERROR_MESSAGE
                    ),
                    LintError(
                        3,
                        5,
                        "enum-entry-name-case",
                        EnumEntryNameCaseRule.ERROR_MESSAGE
                    ),
                )
            )
    }
}
