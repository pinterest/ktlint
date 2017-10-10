package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoMultipleSpacesRuleTest {

    @Test
    fun testLint() {
        assertThat(NoMultipleSpacesRule().lint("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo(listOf(
                LintError(1, 22, "no-multi-spaces", "Unnecessary space(s)")
            ))
        // allow vertical alignment of comments
        assertThat(NoMultipleSpacesRule().lint(
            """
            // square/kotlinpoet/src/main/java/com/squareup/kotlinpoet/FunSpec.kt case
            fun characterLiteralWithoutSingleQuotes(c: Char): String {
              return when {
                c == '\b' -> "\\b"   // \u0008: backspace (BS)
                c == '\t' -> "\\t"   // \u0009: horizontal tab (HT)
                c == '\n' -> "\\n"   // \u000a: linefeed (LF)
                c == '\r' -> "\\r"   // \u000d: carriage return (CR)
                c == '\"' -> "\""    // \u0022: double quote (")
                c == '\'' -> "\\'"   // \u0027: single quote (')
                c == '\\' -> "\\\\"  // \u005c: backslash (\)
                isISOControl(c) -> String.format("\\u%04x", c.toInt())
                else -> Character.toString(c)
              }
            }
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(NoMultipleSpacesRule().format("fun main() { x(1,3);  x(1, 3)\n  \n  }"))
            .isEqualTo("fun main() { x(1,3); x(1, 3)\n  \n  }")
    }

}
