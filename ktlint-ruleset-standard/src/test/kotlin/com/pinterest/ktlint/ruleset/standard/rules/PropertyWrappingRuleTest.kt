package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

internal class PropertyWrappingRuleTest {
    private val propertyWrappingRuleAssertThat =
        assertThatRuleBuilder { PropertyWrappingRule() }
            .addAdditionalRuleProvider { MaxLineLengthRule() }
            .assertThat()

    @Test
    fun `Given that the variable name and the following colon do not fit on the same line as val or var keyword then wrap after the colon`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
            val aVariableWithALooooooongName: String
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER     $EOL_CHAR
            val aVariableWithALooooooongName:
                String
            """.trimIndent()
        propertyWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 34, "Missing newline after \":\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the type does not fit on the same line as the variable name`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            val aVariableWithALongerName: TypeWithALongName
            val aVariableWithALongName2: TypeWithALongName
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            val aVariableWithALongerName:
                TypeWithALongName
            val aVariableWithALongName2: TypeWithALongName
            """.trimIndent()
        propertyWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 30, "Missing newline before \"TypeWithALongName\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that equals sign before the value does not fit on the same line as the type and variable name then wrap after the equals sign`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                     $EOL_CHAR
            val aVariableWithALongName: TypeWithALongerName = TypeWithALongName(123)
            val aVariableWithALongName: TypeWithALongName2 = TypeWithALongName(123)
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                     $EOL_CHAR
            val aVariableWithALongName: TypeWithALongerName =
                TypeWithALongName(123)
            val aVariableWithALongName: TypeWithALongName2 =
                TypeWithALongName(123)
            """.trimIndent()
        propertyWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 50, "Missing newline after \"=\""),
                LintViolation(3, 49, "Missing newline before \"TypeWithALongName(123)\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the value does not fit on the same line as the type and variable name then wrap after the equals sign`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                            $EOL_CHAR
            val aVariableWithALongName: TypeWithALongerName = TypeWithALongName(123)
            val aVariableWithALongName: TypeWithALongName2 = TypeWithALongName(123)
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                            $EOL_CHAR
            val aVariableWithALongName: TypeWithALongerName =
                TypeWithALongName(123)
            val aVariableWithALongName: TypeWithALongName2 = TypeWithALongName(123)
            """.trimIndent()
        propertyWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 50, "Missing newline before \"TypeWithALongName(123)\"")
            .isFormattedAs(formattedCode)
    }
}
