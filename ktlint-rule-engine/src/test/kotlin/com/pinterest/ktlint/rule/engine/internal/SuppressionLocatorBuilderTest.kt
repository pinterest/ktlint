package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
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
    fun `Given that a NoFooIdentifierRule violation is suppressed with an EOL-comment to disable all rules then do not find a violation`() {
        val code =
            """
            val foo = "foo" // ktlint-disable
            """.trimIndent()
        assertThat(lint(code)).isEmpty()
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with an EOL-comment for the specific rule then do not find a violation`() {
        val code =
            """
            val foo = "foo" // ktlint-disable no-foo-identifier-standard $NON_STANDARD_RULE_SET_ID:no-foo-identifier
            """.trimIndent()
        assertThat(lint(code)).isEmpty()
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with a block comment for all rules then do not find a violation in that block`() {
        val code =
            """
            /* ktlint-disable */
            val fooNotReported = "foo"
            /* ktlint-enable */
            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(4, 5, "standard:no-foo-identifier-standard"),
            lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with a block comment for a specific rule then do not find a violation for that rule in that block`() {
        val code =
            """
            /* ktlint-disable no-foo-identifier-standard $NON_STANDARD_RULE_SET_ID:no-foo-identifier */
            val fooNotReported = "foo"
            /* ktlint-enable no-foo-identifier-standard $NON_STANDARD_RULE_SET_ID:no-foo-identifier */
            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            lintError(4, 5, "standard:no-foo-identifier-standard"),
            lintError(4, 5, "$NON_STANDARD_RULE_SET_ID:no-foo-identifier"),
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
    ) = ArrayList<LintError>().apply {
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    // The same rule is supplied once a standard rule and once as non-standard rule. Note that the
                    // ruleIds are different.
                    RuleProvider { NoFooIdentifierRule(STANDARD_NO_FOO_IDENTIFIER_RULE_ID) },
                    RuleProvider { NoFooIdentifierRule(NON_STANDARD_NO_FOO_IDENTIFIER_RULE_ID) },
                ),
            editorConfigOverride =
                editorConfigOverride
                    .plus(
                        STANDARD_NO_FOO_IDENTIFIER_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                        NON_STANDARD_NO_FOO_IDENTIFIER_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                    ),
        ).lint(Code.fromSnippet(code)) { e -> add(e) }
    }

    private fun lintError(
        line: Int,
        col: Int,
        ruleId: String,
    ) = LintError(line, col, RuleId(ruleId), "Line should not contain a foo identifier", false)

    private companion object {
        val NON_STANDARD_RULE_SET_ID = "custom".also { require(it != RuleSetId.STANDARD.value) }

        val STANDARD_NO_FOO_IDENTIFIER_RULE_ID = RuleId("standard:no-foo-identifier-standard")
        val NON_STANDARD_NO_FOO_IDENTIFIER_RULE_ID = RuleId("$NON_STANDARD_RULE_SET_ID:no-foo-identifier")
    }
}
