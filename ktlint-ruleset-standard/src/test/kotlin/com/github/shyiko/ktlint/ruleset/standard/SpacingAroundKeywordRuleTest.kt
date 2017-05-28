package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class SpacingAroundKeywordRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundKeywordRule().lint(
            """
            fun main() {
                if(true) {}
                while(true) {}
                do {} while(true)

                if (true) {}
                else {}

                try { "".trim() }
                catch (e: Exception) {}
                finally {}

                if (true) {
                } else {}
                if (true) {} else {}

                try {
                    "".trim()
                } catch (e: Exception) {
                } finally {}
                try { "".trim() } catch (e: Exception) {} finally {}

                val v = 0
                when (v) {
                    1 -> println("")
                    else -> println("")
                }
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 7, "keyword-spacing", "Missing spacing after \"if\""),
            LintError(3, 10, "keyword-spacing", "Missing spacing after \"while\""),
            LintError(4, 16, "keyword-spacing", "Missing spacing after \"while\""),
            LintError(7, 5, "keyword-spacing", "Unexpected newline before \"else\""),
            LintError(10, 5, "keyword-spacing", "Unexpected newline before \"catch\""),
            LintError(11, 5, "keyword-spacing", "Unexpected newline before \"finally\"")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(SpacingAroundKeywordRule().format(
            """
            fun main() {
                if(true) {}
                if (true) {}
                while(true) {}
                do {} while(true)

                if (true) {}
                else {}
                if (true) {
                }
                else {}

                try { "".trim() }
                catch (e: Exception) {}
                finally {}
            }
            """.trimIndent()
        )).isEqualTo(
            """
            fun main() {
                if (true) {}
                if (true) {}
                while (true) {}
                do {} while (true)

                if (true) {} else {}
                if (true) {
                } else {}

                try { "".trim() } catch (e: Exception) {} finally {}
            }
            """.trimIndent()
        )
    }

}

