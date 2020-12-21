package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringTemplateRuleTest {

    @Test
    fun testLint() {
        assertThat(StringTemplateRule().diffFileLint("spec/string-template/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            StringTemplateRule().diffFileFormat(
                "spec/string-template/format.kt.spec",
                "spec/string-template/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatIssue996() {
        assertThat(
            StringTemplateRule().format(
                """
                fun getDrafts(val draftsIds: List<Long>) {
                    println("draftIds=[${'$'}{draftsIds.toString()}]")
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            fun getDrafts(val draftsIds: List<Long>) {
                println("draftIds=[${'$'}draftsIds]")
            }
            """.trimIndent()
        )
    }
}
