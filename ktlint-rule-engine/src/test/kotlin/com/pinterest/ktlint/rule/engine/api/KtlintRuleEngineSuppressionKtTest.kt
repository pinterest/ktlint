package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.standard.rules.CONDITION_WRAPPING_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.NO_CONSECUTIVE_BLANK_LINES_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.NO_LINE_BREAK_BEFORE_ASSIGNMENT_RULE_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KtlintRuleEngineSuppressionKtTest {
    private val ktLintRuleEngine =
        KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { SomeRule() }),
        )

    @Test
    fun `Given a FileSuppression then add the suppression at file level`() {
        val code =
            """
            import foo.Foo
            """.trimIndent()
        val formattedCode =
            """
            @file:Suppress("ktlint:standard:some-rule-id")

            import foo.Foo
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionForFile(SOME_RULE_ID),
                )
        assertThat(actual).isEqualTo(formattedCode)
    }

    @Test
    fun `Given an OffsetSuppression at an import statement then add the suppression at file level`() {
        val code =
            """
            import foo.Foo
            """.trimIndent()
        val formattedCode =
            """
            @file:Suppress("ktlint:standard:some-rule-id")

            import foo.Foo
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(1, 1, SOME_RULE_ID),
                )
        assertThat(actual).isEqualTo(formattedCode)
    }

    @Test
    fun `Given an OffsetSuppression in a top level declaration before the assignment then add the suppression on the declaration`() {
        val code =
            """
            val foo = "Foo"
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:some-rule-id")
            val foo = "Foo"
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(1, 1, SOME_RULE_ID),
                )
        assertThat(actual).isEqualTo(formattedCode)
    }

    @ParameterizedTest(name = "Index: {0}")
    @ValueSource(
        strings = [
            "11", // Opening quotes
            "12", // Character F
            "13", // Character o (first occurrence)
            "14", // Character o (second occurrence)
            "15", // Closing quotes
        ],
    )
    fun `Given an OffsetSuppression in a string template then add the suppression on top of the string template`(index: Int) {
        val code =
            """
            val foo = "Foo"
            """.trimIndent()
        val formattedCode =
            """
            val foo = @Suppress("ktlint:standard:some-rule-id")
            "Foo"
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(1, index, SOME_RULE_ID),
                )

        assertThat(actual).isEqualTo(formattedCode)
    }

    @Test
    fun `Given an OffsetSuppression inside a string template value argument then add the suppression to the value argument`() {
        val code =
            """
            fun foo() {
                bar("Foo")
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                bar(@Suppress("ktlint:standard:some-rule-id")
                "Foo")
            }
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(2, 10, SOME_RULE_ID),
                )

        assertThat(actual).isEqualTo(formattedCode)
    }

    @Test
    fun `Given an OffsetSuppression on a value argument then add the suppression to the parent of the value argument list`() {
        val code =
            """
            fun foo(): String {
                bar(
                    "Foo",
                )
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String {
                @Suppress("ktlint:standard:some-rule-id")
                bar(
                    "Foo",
                )
            }
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(3, 14, SOME_RULE_ID),
                )

        assertThat(actual).isEqualTo(formattedCode)
    }

    @Test
    fun `Given an OffsetSuppression in a string template which is part of a return statement then add the suppression on top of the return statement`() {
        val code =
            """
            fun foo(): String {
                return "Foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo(): String {
                @Suppress("ktlint:standard:some-rule-id")
                return "Foo"
            }
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(2, 13, SOME_RULE_ID),
                )

        assertThat(actual).isEqualTo(formattedCode)
    }

    @Nested
    inner class `Issue 2462 - Given binary expression on which a suppression is added` {
        @Test
        fun `Given a value argument with a multiline condition to which a suppression is to be added`() {
            val code =
                """
                fun foo() {
                    require(
                        true && false ||
                            true,
                    )
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    @Suppress("ktlint:standard:condition-wrapping")
                    require(
                        true && false ||
                            true,
                    )
                }
                """.trimIndent()
            val actual =
                ktLintRuleEngine
                    .insertSuppression(
                        Code.fromSnippet(code, false),
                        KtlintSuppressionAtOffset(3, 17, CONDITION_WRAPPING_RULE_ID),
                    )

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a property assignment with a multiline condition to which a suppression is to be added`() {
            val code =
                """
                val bar =
                    true && false ||
                        true
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:condition-wrapping")
                val bar =
                    true && false ||
                        true
                """.trimIndent()
            val actual =
                ktLintRuleEngine
                    .insertSuppression(
                        Code.fromSnippet(code, false),
                        KtlintSuppressionAtOffset(2, 17, CONDITION_WRAPPING_RULE_ID),
                    )

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an if statement with a multiline condition to which a suppression is to be added`() {
            val code =
                """
                fun foo() {
                    if (true && false ||
                        true
                    ) {}
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    @Suppress("ktlint:standard:condition-wrapping")
                    if (true && false ||
                        true
                    ) {}
                }
                """.trimIndent()
            val actual =
                ktLintRuleEngine
                    .insertSuppression(
                        Code.fromSnippet(code, false),
                        KtlintSuppressionAtOffset(2, 17, CONDITION_WRAPPING_RULE_ID),
                    )

            assertThat(actual).isEqualTo(formattedCode)
        }
    }

    @Test
    fun `Given a violation with offset exactly at the EOL of a line`() {
        val code =
            """
            fun foo(): String // Some comment
                = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:no-line-break-before-assignment")
            fun foo(): String // Some comment
                = "some-result"
            """.trimIndent()
        val actual =
            ktLintRuleEngine
                .insertSuppression(
                    Code.fromSnippet(code, false),
                    KtlintSuppressionAtOffset(1, 34, NO_LINE_BREAK_BEFORE_ASSIGNMENT_RULE_ID),
                )

        assertThat(actual).isEqualTo(formattedCode)
    }

//
    @Nested
    inner class `Consecutive line suppression can not be placed at line` {
        @Test
        fun `Given some consecutive blank lines at top level`() {
            val code =
                """
                val foo = "foo"


                val bar = "bar"
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:no-consecutive-blank-lines")

                val foo = "foo"


                val bar = "bar"
                """.trimIndent()
            val actual =
                ktLintRuleEngine
                    .insertSuppression(
                        Code.fromSnippet(code, false),
                        KtlintSuppressionAtOffset(3, 1, NO_CONSECUTIVE_BLANK_LINES_RULE_ID),
                    )

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given some consecutive blank lines inside a function`() {
            val code =
                """
                fun foobar() {
                    val foo = "foo"


                    val bar = "bar"
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:no-consecutive-blank-lines")
                fun foobar() {
                    val foo = "foo"


                    val bar = "bar"
                }
                """.trimIndent()
            val actual =
                ktLintRuleEngine
                    .insertSuppression(
                        Code.fromSnippet(code, false),
                        KtlintSuppressionAtOffset(3, 1, NO_CONSECUTIVE_BLANK_LINES_RULE_ID),
                    )

            assertThat(actual).isEqualTo(formattedCode)
        }
    }

    private companion object {
        val SOME_RULE_ID = RuleId("standard:some-rule-id")
    }

    private class SomeRule : Rule(ruleId = SOME_RULE_ID, about = About())
}
