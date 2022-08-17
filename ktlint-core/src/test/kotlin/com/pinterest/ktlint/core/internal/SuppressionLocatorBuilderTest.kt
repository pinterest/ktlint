package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.ast.ElementType
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
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
            LintError(1, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(1, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
            LintError(2, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(2, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
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
            val foo = "foo" // ktlint-disable no-foo-identifier-standard custom:no-foo-identifier
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
            LintError(4, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(4, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with a block comment for a specific rule then do not find a violation for that rule in that block`() {
        val code =
            """
            /* ktlint-disable no-foo-identifier-standard custom:no-foo-identifier */
            val fooNotReported = "foo"
            /* ktlint-enable no-foo-identifier-standard custom:no-foo-identifier */
            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            LintError(4, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(4, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
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
            LintError(4, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(4, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at statement level for a specific rule then do not find a violation for that rule`() {
        val code =
            """
            @Suppress("ktlint:no-foo-identifier-standard", "ktlint:custom:no-foo-identifier")
            val fooNotReported = "foo"

            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            LintError(4, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(4, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at function level then do not find a violation for that rule in that function`() {
        val code =
            """
            @Suppress("ktlint:no-foo-identifier-standard", "ktlint:custom:no-foo-identifier")
            fun foo() {
                val fooNotReported = "foo"
            }

            val fooReported = "foo"
            """.trimIndent()
        assertThat(lint(code)).containsExactly(
            LintError(6, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(6, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
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
            LintError(6, 5, "no-foo-identifier-standard", "Line should not contain a foo identifier"),
            LintError(6, 5, "custom:no-foo-identifier", "Line should not contain a foo identifier"),
        )
    }

    @Test
    fun `Given that a NoFooIdentifierRule violation is suppressed with @Suppress at class level then do not find a violation for that rule in that class`() {
        val code =
            """
            @Suppress("ktlint:no-foo-identifier-standard", "ktlint:custom:no-foo-identifier")
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
            @file:Suppress("ktlint:no-foo-identifier-standard", "ktlint:custom:no-foo-identifier")

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

    private class NoFooIdentifierRule(id: String) : Rule(id) {
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

    private fun lint(code: String) =
        ArrayList<LintError>().apply {
            KtLint.lint(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleProviders = setOf(
                        // The same rule is supplied once a standard rule and once as non-standard rule. Note that the
                        // ruleIds are different.
                        RuleProvider { NoFooIdentifierRule("no-foo-identifier-standard") },
                        RuleProvider { NoFooIdentifierRule("$NON_STANDARD_RULE_SET_ID:no-foo-identifier") },
                    ),
                    cb = { e, _ -> add(e) },
                ),
            )
        }

    private companion object {
        const val STANDARD_RULE_SET_ID = "standard" // Value may not be changed
        const val NON_STANDARD_RULE_SET_ID = "custom" // Can be any value other than "standard"
    }
}
