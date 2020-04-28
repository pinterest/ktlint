package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FinalNewlineRuleTest {

    @Test
    fun testLint() {
        // neither true nor false
        assertThat(FinalNewlineRule().lint("fun name() {\n}")).isEqualTo(
            listOf(
                LintError(1, 1, "final-newline", "File must end with a newline (\\n)")
            )
        )
        assertThat(FinalNewlineRule().lint("fun name() {\n}\n")).isEmpty()
        // true
        assertThat(
            FinalNewlineRule().lint(
                "",
                mapOf("insert_final_newline" to "true")
            )
        ).isEmpty()
        assertThat(
            FinalNewlineRule().lint(
                "fun name() {\n}",
                mapOf("insert_final_newline" to "true")
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "final-newline", "File must end with a newline (\\n)")
            )
        )
        assertThat(
            FinalNewlineRule().lint(
                "fun name() {\n}\n",
                mapOf("insert_final_newline" to "true")
            )
        ).isEmpty()
        assertThat(
            FinalNewlineRule().lint(
                "fun main() {\n}\n\n\n",
                mapOf("insert_final_newline" to "true"),
                script = true
            )
        ).isEmpty()
        // false
        assertThat(
            FinalNewlineRule().lint(
                "",
                mapOf("insert_final_newline" to "false")
            )
        ).isEmpty()
        assertThat(
            FinalNewlineRule().lint(
                "fun name() {\n}",
                mapOf("insert_final_newline" to "false")
            )
        ).isEmpty()
        assertThat(
            FinalNewlineRule().lint(
                "fun name() {\n}\n",
                mapOf("insert_final_newline" to "false")
            )
        ).isEqualTo(
            listOf(
                LintError(2, 2, "final-newline", "Redundant newline (\\n) at the end of file")
            )
        )
    }

    @Test
    fun testFormat() {
        assertThat(
            FinalNewlineRule().format(
                "fun name() {\n}",
                mapOf("insert_final_newline" to "true")
            )
        ).isEqualTo(
            "fun name() {\n}\n"
        )
        assertThat(
            FinalNewlineRule().format(
                "fun name() {\n}\n",
                mapOf("insert_final_newline" to "false")
            )
        ).isEqualTo(
            "fun name() {\n}"
        )
    }
}
