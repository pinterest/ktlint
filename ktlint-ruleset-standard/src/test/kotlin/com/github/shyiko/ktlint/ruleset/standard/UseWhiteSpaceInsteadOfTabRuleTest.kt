package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class UseWhiteSpaceInsteadOfTabRuleTest {
    @Test
    fun testTab_Success() {
        val ktScript = """fun main() {
            return 0
        }"""
        assertThat(format(ktScript)).isEqualTo(ktScript)
        assertThat(lint(ktScript)).isEqualTo(emptyList<LintError>())
    }

    @Test
    fun testTab_Fail() {
        val ktScript = "fun main() {\n\t\treturn 0\n\t}"
        assertThat(format(ktScript)).isEqualTo("fun main() {\n        return 0\n    }")
        assertThat(lint(ktScript)).isEqualTo(listOf(
            LintError(line = 1, col = 13, ruleId = "use-whitespace-instead-of-tab-rule", detail = "Use 4 spaces for indentation. Do not use tabs."),
            LintError(line = 2, col = 11, ruleId = "use-whitespace-instead-of-tab-rule", detail = "Use 4 spaces for indentation. Do not use tabs.")
        ))
    }

    @Test
    fun testTab_FailWithEditorConfig() {
        val ktScript = "fun main() {\n\t\treturn 0\n\t}"
        assertThat(format(ktScript, mapOf("indent_size" to "3"))).isEqualTo("fun main() {\n      return 0\n   }")
        assertThat(lint(ktScript, mapOf("indent_size" to "3"))).isEqualTo(listOf(
            LintError(line = 1, col = 13, ruleId = "use-whitespace-instead-of-tab-rule", detail = "Use 3 spaces for indentation. Do not use tabs."),
            LintError(line = 2, col = 11, ruleId = "use-whitespace-instead-of-tab-rule", detail = "Use 3 spaces for indentation. Do not use tabs.")
        ))
    }

    private fun format(kotlinScript: String, userData: Map<String, String> = emptyMap()): String {
        return UseWhiteSpaceInsteadOfTabRule().format(kotlinScript, userData)
    }

    private fun lint(kotlinScript: String, userData: Map<String, String> = emptyMap()): List<LintError> {
        return UseWhiteSpaceInsteadOfTabRule().lint(kotlinScript, userData)
    }
}
