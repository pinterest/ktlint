package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAfterKeywordRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAfterKeywordRule().lint(
            """
            fun main() {
                if(true) {}
                while(true) {}
                do {} while(true)
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 7, "rule-id", "Missing spacing after \"if\""),
            LintError(3, 10, "rule-id", "Missing spacing after \"while\""),
            LintError(4, 16, "rule-id", "Missing spacing after \"while\"")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAfterKeywordRule().format(
            """
            fun main() {
                if(true) {}
                if (true) {}
                while(true) {}
                do {} while(true)
            }
            """.trimIndent()
        )).isEqualTo(
            """
            fun main() {
                if (true) {}
                if (true) {}
                while (true) {}
                do {} while (true)
            }
            """.trimIndent()
        )
    }

}

