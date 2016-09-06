package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.SpacingAfterKeywordRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
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
            LintError(2, 7, "keyword-spacing", "Missing spacing after \"if\""),
            LintError(3, 10, "keyword-spacing", "Missing spacing after \"while\""),
            LintError(4, 16, "keyword-spacing", "Missing spacing after \"while\"")
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

