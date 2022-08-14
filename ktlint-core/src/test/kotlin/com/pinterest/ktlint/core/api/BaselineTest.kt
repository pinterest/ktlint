package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BaselineTest {
    @Test
    fun testParseBaselineFile() {
        val filename = "TestBaselineFile.kt"
        val errorOne = LintError(
            line = 1,
            col = 1,
            ruleId = "final-new-line",
            detail = "",
        )
        val errorTwo = LintError(
            line = 62,
            col = 1,
            ruleId = "no-blank-line-before-rbrace",
            detail = "",
        )

        val baseline: InputStream =
            ByteArrayInputStream(
                """
                <file name="$filename">
                        <error line="${errorOne.line}" column="${errorOne.col}" source="${errorOne.ruleId}" />
                        <error line="${errorTwo.line}" column="${errorTwo.col}" source="${errorTwo.ruleId}" />
                    </file>
                """.trimIndent()
                    .toByteArray(),
            )

        val baselineFiles = parseBaseline(baseline)

        assertThat(baselineFiles).containsEntry(
            filename,
            listOf(errorOne, errorTwo),
        )
    }
}
