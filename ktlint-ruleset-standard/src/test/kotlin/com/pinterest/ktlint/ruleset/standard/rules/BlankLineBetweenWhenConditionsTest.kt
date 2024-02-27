package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.ruleset.standard.rules.BlankLineBetweenWhenConditions.Companion.LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BlankLineBetweenWhenConditionsTest {
    private val blankLineAfterWhenConditionRuleAssertThat = assertThatRule { BlankLineBetweenWhenConditions() }

    @Test
    fun `Given a when-statement with single line when-conditions only then do no reformat`() {
        val code =
            """
            val foo =
                when (bar) {
                    BAR1 -> "bar1"
                    BAR2 -> "bar2"
                    else -> null
                }
            """.trimIndent()
        blankLineAfterWhenConditionRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a when-statement with single line when-conditions only which are separated by a blank line then remove the blank lines`() {
        val code =
            """
            val foo =
                when (bar) {
                    BAR1 -> "bar1"

                    BAR2 -> "bar2"

                    else -> null
                }
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                when (bar) {
                    BAR1 -> "bar1"
                    BAR2 -> "bar2"
                    else -> null
                }
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        blankLineAfterWhenConditionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 1, "Unexpected blank lines between when-condition if all when-conditions are single lines"),
                LintViolation(6, 1, "Unexpected blank lines between when-condition if all when-conditions are single lines"),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given when statement with multiline when-condition` {
        @Nested
        inner class `Given linebreaks have to be added after when-condition` {
            @Test
            fun `Given a when-statement with a single line when-condition after a multiline when-condition then add a blank line between the when-conditions`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR1 -> "bar1"
                            BAR2 ->
                                "bar2"
                            else -> null
                        }
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo =
                        when (bar) {
                            BAR1 -> "bar1"

                            BAR2 ->
                                "bar2"

                            else -> null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(4, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement"),
                        LintViolation(6, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a when-statement with a single line when-condition before a multiline when-condition then add a blank line between the when-conditions`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR1 -> "bar1"
                            BAR2 -> "bar2"
                            else ->
                                null
                        }
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo =
                        when (bar) {
                            BAR1 -> "bar1"

                            BAR2 -> "bar2"

                            else ->
                                null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(4, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement"),
                        LintViolation(5, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a when-condition with a single line body on the next line then add blank lines`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR1 ->
                                "bar1"
                            BAR2 ->
                                "bar2"
                            else ->
                                null
                        }
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo =
                        when (bar) {
                            BAR1 ->
                                "bar1"

                            BAR2 ->
                                "bar2"

                            else ->
                                null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .hasLintViolations(
                        LintViolation(5, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement"),
                        LintViolation(7, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement"),
                    ).isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a when-condition preceded by a comment and the when-condition needs to be preceded by blank line then add this line before the comment`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR -> "bar"
                            // Some comment
                            else ->
                                null
                        }
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo =
                        when (bar) {
                            BAR -> "bar"

                            // Some comment
                            else ->
                                null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .hasLintViolation(4, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement")
                    .isFormattedAs(formattedCode)
            }

            @Test
            fun `Given a simple when-statement in which a when condition is preceded by a comment then add blank lines between the when-conditions`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR -> "bar"
                            // Some comment
                            else -> null
                        }
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo =
                        when (bar) {
                            BAR -> "bar"

                            // Some comment
                            else -> null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .hasLintViolation(4, 1, "Add a blank line between all when-condition in case at least one multiline when-condition is found in the statement")
                    .isFormattedAs(formattedCode)
            }
        }

        @Nested
        inner class `Given no linebreaks may be added after when-condition` {
            @Test
            fun `Given a when-statement with a single line when-condition after a multiline when-condition then add a blank line between the when-conditions`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR1 -> "bar1"
                            BAR2 ->
                                "bar2"
                            else -> null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .withEditorConfigOverride(LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY to false)
                    .hasNoLintViolations()
            }

            @Test
            fun `Given a when-statement with a single line when-condition before a multiline when-condition then add a blank line between the when-conditions`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR1 -> "bar1"
                            BAR2 -> "bar2"
                            else ->
                                null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .withEditorConfigOverride(LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY to false)
                    .hasNoLintViolations()
            }

            @Test
            fun `Given a when-condition preceded by a comment and the when-condition needs to be preceded by blank line then add this line before the comment`() {
                val code =
                    """
                    val foo =
                        when (bar) {
                            BAR -> "bar"
                            // Some comment
                            else ->
                                null
                        }
                    """.trimIndent()
                @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
                blankLineAfterWhenConditionRuleAssertThat(code)
                    .withEditorConfigOverride(LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY to false)
                    .hasNoLintViolations()
            }
        }
    }
}
