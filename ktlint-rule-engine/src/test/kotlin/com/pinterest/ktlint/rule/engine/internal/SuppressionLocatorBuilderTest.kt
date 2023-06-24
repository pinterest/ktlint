package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.internal.FormatterTags.Companion.FORMATTER_TAGS_ENABLED_PROPERTY
import com.pinterest.ktlint.rule.engine.internal.FormatterTags.Companion.FORMATTER_TAG_OFF_ENABLED_PROPERTY
import com.pinterest.ktlint.rule.engine.internal.FormatterTags.Companion.FORMATTER_TAG_ON_ENABLED_PROPERTY
import com.pinterest.ktlint.rule.engine.internal.rules.KTLINT_SUPPRESSION_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.IndentationRule
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SuppressionLocatorBuilderTest {
    @Test
    fun `Given that NoFooIdentifierRule finds a violation (eg verifying that the test rules actually works)`() {
        val code =
            """
            val foo = "foo"
            val fooWithSuffix = "fooWithSuffix"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(1, 5, "standard:no-foo-identifier-standard"),
            lintError(1, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
            lintError(2, 5, "standard:no-foo-identifier-standard"),
            lintError(2, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
        )
    }

    @Test
    fun `Given a line having a NoFooIdentifierRule violation and an EOL-comment with a ktlint-directive to disable all rules then do not suppress the violation anymore`() {
        val code =
            """
            val foo = "foo" // ktlint-disable
            """.trimIndent()
        @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
        assertThat(lint(code))
            .contains(
                LintError(1, 5, STANDARD_NO_FOO_IDENTIFIER_RULE_ID, "Line should not contain a foo identifier", false),
            )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at statement level for all rules then do not find a violation`() {
        val code =
            """
            @Suppress("ktlint")
            val fooNotReported = "foo"

            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(4, 5, "standard:no-foo-identifier-standard"),
            lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at statement level for a specific rule then do not find a violation for that rule`() {
        val code =
            """
            @Suppress("ktlint:no-foo-identifier-standard", "ktlint:$NON_STANDARD_RULE_SET_ID:no-foo-identifier")
            val fooNotReported = "foo"

            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(4, 5, "standard:no-foo-identifier-standard"),
            lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at function level then do not find a violation for that rule in that function`() {
        val code =
            """
            @Suppress("ktlint:no-foo-identifier-standard", "ktlint:$NON_STANDARD_RULE_SET_ID:no-foo-identifier")
            fun foo() {
                val fooNotReported = "foo"
            }

            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(6, 5, "standard:no-foo-identifier-standard"),
            lintError(6, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at function level for all rules then do not find violations in that function`() {
        val code =
            """
            @Suppress("ktlint")
            fun foo() {
                val fooNotReported = "foo"
            }

            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(6, 5, "standard:no-foo-identifier-standard"),
            lintError(6, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at class level then do not find a violation for that rule in that class`() {
        val code =
            """
            @Suppress("ktlint:no-foo-identifier-standard", "ktlint:$NON_STANDARD_RULE_SET_ID:no-foo-identifier")
            class Foo {
                fun foo() {
                    val fooNotReported = "foo"
                }

                val foo = "foo"
            }
            """.trimIndent()
        assertThat(lint(code)).isEmpty()
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress for all rules at class level then do not find a violation for that rule in that class`() {
        val code =
            """
            @Suppress("ktlint")
            class Foo {
                fun foo() {
                    val fooNotReported = "foo"
                }

                val foo = "foo"
            }
            """.trimIndent()
        assertThat(lint(code)).isEmpty()
    }

    @Test
    fun `Given that the NoFooIdentifierRule is suppressed in the entire file with @file-colon-Suppress then do not find any NoFooIdentifierRule violation`() {
        val code =
            """
            @file:Suppress("ktlint:no-foo-identifier-standard", "ktlint:$NON_STANDARD_RULE_SET_ID:no-foo-identifier")

            class Foo {
                fun foo() {
                    val fooNotReported = "foo"
                }
            }

            val fooNotReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).isEmpty()
    }

    @Test
    fun `Given that all rules are suppressed in the entire file with @file-colon-Suppress then do not find any violation`() {
        val code =
            """
            @file:Suppress("ktlint")

            class Foo {
                fun foo() {
                    val fooNotReported = "foo"
                }
            }

            val fooNotReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).isEmpty()
    }

    @Nested
    inner class `Given that formatter tags are enabled` {
        @Test
        fun `Given that a NoFooIdentifierRule violation is suppressed with the default formatter tags in a block comment then do not find a violation in that block`() {
            val code =
                """
                /* @formatter:off */
                val fooNotReported = "foo"
                /* @formatter:on */
                val fooReported = "foo"
                """.trimIndent()

            val actual =
                lint(
                    code,
                    editorConfigOverride = EditorConfigOverride.from(FORMATTER_TAGS_ENABLED_PROPERTY to true),
                )
            assertThat(actual).containsExactly(
                lintError(4, 5, "standard:no-foo-identifier-standard"),
                lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
            )
        }

        @Test
        fun `Given that a NoFooIdentifierRule violation is suppressed with the default formatter tags in EOL comments then do not find a violation in that block`() {
            val code =
                """
                // @formatter:off
                val fooNotReported = "foo"
                // @formatter:on
                val fooReported = "foo"
                """.trimIndent()

            val actual =
                lint(
                    code,
                    editorConfigOverride = EditorConfigOverride.from(FORMATTER_TAGS_ENABLED_PROPERTY to true),
                )
            assertThat(actual).containsExactly(
                lintError(4, 5, "standard:no-foo-identifier-standard"),
                lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
            )
        }

        @Test
        fun `Given that a NoFooIdentifierRule violation is suppressed with custom formatter tags in block comments then do not find a violation in that block`() {
            val code =
                """
                /* custom-formatter-tag-off */
                val fooNotReported = "foo"
                /* custom-formatter-tag-on */
                val fooReported = "foo"
                """.trimIndent()

            val actual =
                lint(
                    code,
                    editorConfigOverride =
                        EditorConfigOverride.from(
                            FORMATTER_TAGS_ENABLED_PROPERTY to true,
                            FORMATTER_TAG_OFF_ENABLED_PROPERTY to "custom-formatter-tag-off",
                            FORMATTER_TAG_ON_ENABLED_PROPERTY to "custom-formatter-tag-on",
                        ),
                )
            assertThat(actual).containsExactly(
                lintError(4, 5, "standard:no-foo-identifier-standard"),
                lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
            )
        }

        @Test
        fun `Given that a NoFooIdentifierRule violation is suppressed with custom formatter tags in EOL comments then do not find a violation in that block`() {
            val code =
                """
                // custom-formatter-tag-off
                val fooNotReported = "foo"
                // custom-formatter-tag-on
                val fooReported = "foo"
                """.trimIndent()

            val actual =
                lint(
                    code,
                    editorConfigOverride =
                        EditorConfigOverride.from(
                            FORMATTER_TAGS_ENABLED_PROPERTY to true,
                            FORMATTER_TAG_OFF_ENABLED_PROPERTY to "custom-formatter-tag-off",
                            FORMATTER_TAG_ON_ENABLED_PROPERTY to "custom-formatter-tag-on",
                        ),
                )
            assertThat(actual).containsExactly(
                lintError(4, 5, "standard:no-foo-identifier-standard"),
                lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
            )
        }
    }

    @Nested
    inner class `Given code that tries to disable to ktlint-suppression rule itself` {
        @Test
        fun `Given a @file annotation`() {
            val code =
                """
                @file:Suppress("ktlint:internal:ktlint-suppression")
                """.trimIndent()
            val actual = lint(code = code, ignoreKtlintSuppressionRule = false)
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            assertThat(actual).containsExactly(
                LintError(1, 24, KTLINT_SUPPRESSION_RULE_ID, "Ktlint rule with id 'ktlint:internal:ktlint-suppression' is unknown or not loaded", false),
            )
        }

        @Test
        fun `Given a block comment with a ktlint-disable directive`() {
            val code =
                """
                /* ktlint-disable internal:ktlint-suppression */
                """.trimIndent()
            val actual = lint(code = code, ignoreKtlintSuppressionRule = false)
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            assertThat(actual).containsExactly(
                LintError(1, 4, KTLINT_SUPPRESSION_RULE_ID, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation", true),
                LintError(1, 19, KTLINT_SUPPRESSION_RULE_ID, "Ktlint rule with id 'internal:ktlint-suppression' is unknown or not loaded", false),
            )
        }

        @Test
        fun `Given an EOL comment with a ktlint-disable directive which is ignored then emit the violation`() {
            val code =
                """
                val foo = "foo" // ktlint-disable internal:ktlint-suppression
                """.trimIndent()
            val actual = lint(code = code, ignoreKtlintSuppressionRule = false)
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            assertThat(actual).containsExactly(
                lintError(1, 5, "standard:no-foo-identifier-standard"),
                lintError(1, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
                LintError(1, 20, KTLINT_SUPPRESSION_RULE_ID, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation", true),
                LintError(1, 35, KTLINT_SUPPRESSION_RULE_ID, "Ktlint rule with id 'internal:ktlint-suppression' is unknown or not loaded", false),
            )
        }
    }

    @Test
    fun `Given a suppression of a rule which alphabetically comes before rule id ktlint-suppression`() {
        val code =
            """
            fun bar() {
                /* ktlint-disable standard:indent */
                return mapOf(
                       1 to "   1 ms",
                      10 to "  10 ms",
                     999 to " 999 ms",
                    1000 to "   1 sec",
                )
                /* ktlint-enable standard:indent */
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:indent")
            fun bar() {
                return mapOf(
                       1 to "   1 ms",
                      10 to "  10 ms",
                     999 to " 999 ms",
                    1000 to "   1 sec",
                )
            }
            """.trimIndent()

        val actual =
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider { IndentationRule() },
                    ),
                editorConfigOverride =
                    EMPTY_EDITOR_CONFIG_OVERRIDE
                        .plus(
                            STANDARD_NO_FOO_IDENTIFIER_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                        ),
            ).format(Code.fromSnippet(code)) { _, _ -> }

        assertThat(actual).isEqualTo(formattedCode)
    }

    private class NoFooIdentifierRule(id: RuleId) : Rule(
        ruleId = id,
        about = About(),
    ) {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ) {
            if (node.elementType == ElementType.IDENTIFIER && node.text.startsWith("foo")) {
                emit(node.startOffset, "Line should not contain a foo identifier", false)
            }
        }
    }

    private fun lint(
        code: String,
        editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
        ruleProviders: Set<RuleProvider> = emptySet(),
        ignoreKtlintSuppressionRule: Boolean = true,
    ) = ArrayList<LintError>().apply {
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    // The same rule is supplied once a standard rule and once as non-standard rule. Note that the
                    // ruleIds are different.
                    RuleProvider { NoFooIdentifierRule(STANDARD_NO_FOO_IDENTIFIER_RULE_ID) },
                    RuleProvider { NoFooIdentifierRule(NON_STANDARD_NO_FOO_IDENTIFIER_RULE_ID) },
                ).plus(ruleProviders),
            editorConfigOverride =
                editorConfigOverride
                    .plus(
                        STANDARD_NO_FOO_IDENTIFIER_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                        NON_STANDARD_NO_FOO_IDENTIFIER_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                    ),
        ).lint(Code.fromSnippet(code)) { e ->
            if (ignoreKtlintSuppressionRule && e.ruleId == KTLINT_SUPPRESSION_RULE_ID) {
                // This class should be able to test code snippets containing the deprecated ktlint-directives
            } else {
                add(e)
            }
        }
    }

    private fun lintError(
        line: Int,
        column: Int,
        ruleId: String,
    ) = LintError(line, column, RuleId(ruleId), "Line should not contain a foo identifier", false)

    private companion object {
        val NON_STANDARD_RULE_SET_ID = "custom".also { require(it != RuleSetId.STANDARD.value) }

        val STANDARD_NO_FOO_IDENTIFIER_RULE_ID = RuleId("standard:no-foo-identifier-standard")
        val NON_STANDARD_NO_FOO_IDENTIFIER_RULE_ID = RuleId("$NON_STANDARD_RULE_SET_ID:no-foo-identifier")
    }
}
