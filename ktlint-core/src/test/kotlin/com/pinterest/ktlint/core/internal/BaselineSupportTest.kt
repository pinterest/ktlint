package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.LintError
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineSupportTest {

    @Test
    fun testParseBaselineFile() {
        val filename = "TestBaselineFile.kt"
        val errorOne = LintError(
            line = 1,
            col = 1,
            ruleId = "final-new-line",
            detail = ""
        )
        val errorTwo = LintError(
            line = 62,
            col = 1,
            ruleId = "no-blank-line-before-rbrace",
            detail = ""
        )

        val baseline: InputStream = ByteArrayInputStream(
            """
                <file name="$filename">
                        <error line="${errorOne.line}" column="${errorOne.col}" source="${errorOne.ruleId}" />
                        <error line="${errorTwo.line}" column="${errorTwo.col}" source="${errorTwo.ruleId}" />
                    </file>
            """.toByteArray()
        )

        val baselineFiles = parseBaseline(baseline)

        assertTrue(baselineFiles.containsKey(filename))
        assertEquals(2, baselineFiles[filename]?.size)
        assertTrue(true == baselineFiles[filename]?.containsLintError(errorOne))
        assertTrue(true == baselineFiles[filename]?.containsLintError(errorTwo))
    }
}
