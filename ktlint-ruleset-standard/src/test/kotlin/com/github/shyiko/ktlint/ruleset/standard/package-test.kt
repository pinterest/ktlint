package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat

fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()

fun testLintUsingResource(rule: Rule, qualifier: String = "", userData: Map<String, String> = emptyMap()) {
    val resource = "spec/${rule.id}/lint${if (qualifier.isEmpty()) "" else "-$qualifier"}.kt.spec"
    val resourceText = getResourceAsText(resource)
    val dividerIndex = resourceText.lastIndexOf("\n// expect\n")
    if (dividerIndex == -1) {
        throw RuntimeException("$resource must contain '// expect' line")
    }
    val input = resourceText.substring(0, dividerIndex)
    val errors = resourceText.substring(dividerIndex + 1).split('\n').mapNotNull { line ->
        if (line.isBlank() || line == "// expect") {
            null
        } else {
            line.trimMargin("// ").split(':', limit = 3).let { expectation ->
                if (expectation.size != 3) {
                    throw RuntimeException("$resource expectation must be a triple <line>:<column>:<message>")
                    // " (<message> is not allowed to contain \":\")")
                }
                LintError(expectation[0].toInt(), expectation[1].toInt(), rule.id, expectation[2])
            }
        }
    }
    assertThat(rule.lint(input, userData)).isEqualTo(errors)
}

fun testFormatUsingResource(rule: Rule, qualifier: String = "") {
    val q = if (qualifier.isEmpty()) "" else "-$qualifier"
    assertThat(rule.format(getResourceAsText("spec/${rule.id}/format$q.kt.spec")))
        .isEqualTo(getResourceAsText("spec/${rule.id}/format$q-expected.kt.spec"))
}
