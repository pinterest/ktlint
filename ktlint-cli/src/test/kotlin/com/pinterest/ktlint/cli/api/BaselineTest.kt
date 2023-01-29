package com.pinterest.ktlint.cli.api

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.BASELINE_IGNORED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class BaselineTest {
    @Test
    fun testParseBaselineFile() {
        val filename = "baseline/TestBaselineFile.kt"
        val errorOne = KtlintCliError(
            line = 1,
            col = 1,
            ruleId = "final-new-line",
            detail = "",
            status = BASELINE_IGNORED,
        )
        val errorTwo = KtlintCliError(
            line = 62,
            col = 1,
            ruleId = "no-blank-line-before-rbrace",
            detail = "",
            status = BASELINE_IGNORED,
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

        val baselineFiles = baseline.parseBaseline()

        assertThat(baselineFiles).containsEntry(
            filename,
            listOf(errorOne, errorTwo),
        )
    }
}
