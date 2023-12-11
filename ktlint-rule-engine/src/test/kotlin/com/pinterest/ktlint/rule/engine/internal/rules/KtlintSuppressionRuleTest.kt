package com.pinterest.ktlint.rule.engine.internal.rules

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.ruleset.standard.rules.ArgumentListWrappingRule
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import com.pinterest.ktlint.test.KtlintDocumentationTest
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class KtlintSuppressionRuleTest {
    private val ktlintSuppressionRuleAssertThat =
        assertThatRuleBuilder { KtlintSuppressionRule(emptyList()) }
            // Create a dummy rule for each rule id that is used in a ktlint directive or suppression in the tests in this
            // class. If no rule provider is added for the rule id, a lint violation is thrown which will bloat the tests too
            // much.
            //
            // Ids of real rules used but for which the real implementation is unwanted as it would modify the formatted code
            .addAdditionalRuleProvider { DummyRule("standard:no-wildcard-imports") }
            .addAdditionalRuleProvider { DummyRule("standard:no-multi-spaces") }
            .addAdditionalRuleProvider { DummyRule("standard:max-line-length") }
            .addAdditionalRuleProvider { DummyRule("standard:package-name") }
            // Ids of fake rules in a custom and the standard rule set
            .addAdditionalRuleProvider { DummyRule("custom:foo") }
            .addAdditionalRuleProvider { DummyRule("standard:bar") }
            .addAdditionalRuleProvider { DummyRule("standard:foo") }
            .assertThat()

    @Nested
    inner class `Given a suppression annotation missing the rule set id prefix` {
        @Test
        fun `Given a @file Suppress annotation`() {
            val code =
                """
                @file:Suppress("ktlint:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 17, "Identifier to suppress ktlint rule must be fully qualified with the rule set id")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a @file SuppressWarnings annotation`() {
            val code =
                """
                @file:SuppressWarnings("ktlint:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                """.trimIndent()
            val formattedCode =
                """
                @file:SuppressWarnings("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 25, "Identifier to suppress ktlint rule must be fully qualified with the rule set id")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a @file array annotation with Suppress and SuppressWarnings annotations`() {
            val code =
                """
                @file:[Suppress("ktlint:bar", "ktlint:custom:foo") SuppressWarnings("ktlint:foo")]
                """.trimIndent()
            val formattedCode =
                """
                @file:[Suppress("ktlint:standard:bar", "ktlint:custom:foo") SuppressWarnings("ktlint:standard:foo")]
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 18, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 70, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an array annotation with Suppress and SuppressWarnings annotations`() {
            val code =
                """
                @[Suppress("ktlint:bar", "ktlint:custom:foo") SuppressWarnings("ktlint:foo")]
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @[Suppress("ktlint:standard:bar", "ktlint:custom:foo") SuppressWarnings("ktlint:standard:foo")]
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 13, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 65, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a Suppress annotation with a named argument with an arrayOf initialization`() {
            val code =
                """
                @Suppress(names = arrayOf("ktlint:bar", "ktlint:standard:foo", "ktlint:custom:foo"))
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @Suppress(names = arrayOf("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:custom:foo"))
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 28, "Identifier to suppress ktlint rule must be fully qualified with the rule set id")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a Suppress annotation with a named argument with an array (squared brackets) initialization`() {
            val code =
                """
                @Suppress(names = ["ktlint:bar", "ktlint:standard:foo", "ktlint:custom:foo"])
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @Suppress(names = ["ktlint:standard:bar", "ktlint:standard:foo", "ktlint:custom:foo"])
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 21, "Identifier to suppress ktlint rule must be fully qualified with the rule set id")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a Suppress annotation on a declaration`() {
            val code =
                """
                @Suppress("ktlint:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 12, "Identifier to suppress ktlint rule must be fully qualified with the rule set id")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a SuppressWarnings annotation on a declaration`() {
            val code =
                """
                @SuppressWarnings("ktlint:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @SuppressWarnings("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:custom:foo")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 20, "Identifier to suppress ktlint rule must be fully qualified with the rule set id")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given an EOL comment with a ktlint-disable directive not preceded by code leaf on same line`() {
        val code =
            """
            val foo = "foo"
            // ktlint-disable
            val bar = "bar"
            """.trimIndent()
        val formattedCode =
            """
            val foo = "foo"
            val bar = "bar"
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolation(2, 4, "Directive 'ktlint-disable' in EOL comment is ignored as it is not preceded by a code element")
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given an import statement` {
        @Test
        fun `Given an EOL comment with a ktlint-disable directive on an import`() {
            val code =
                """
                import foo.bar
                import foobar.* // ktlint-disable no-wildcard-imports
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:no-wildcard-imports")

                import foo.bar
                import foobar.*
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(2, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an EOL comment with a ktlint-disable directive on an import and an existing @file Suppress annotation`() {
            val code =
                """
                @file:Suppress("aaa", "zzz")

                import foo.bar
                import foobar.* // ktlint-disable no-wildcard-imports
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("aaa", "ktlint:standard:no-wildcard-imports", "zzz")

                import foo.bar
                import foobar.*
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(4, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an EOL comment with a ktlint-disable directive on an import and an existing @file Suppress annotation without parameters`() {
            val code =
                """
                @file:Suppress

                import foobar.* // ktlint-disable no-wildcard-imports
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:no-wildcard-imports")

                import foobar.*
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(3, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an EOL comment with a ktlint-disable directive on an import on a file starting with a (copyright) comment before the package statement`() {
            val code =
                """
                /* Some copyright notice before package statement */
                package foo

                import foo.bar
                import foobar.* // ktlint-disable no-wildcard-imports
                """.trimIndent()
            val formattedCode =
                """
                /* Some copyright notice before package statement */
                @file:Suppress("ktlint:standard:no-wildcard-imports")

                package foo

                import foo.bar
                import foobar.*
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(5, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an EOL comment with a ktlint-disable directive on an import and an existing @file Suppress annotation  on a file starting with a (copyright) comment`() {
            val code =
                """
                /* Some copyright notice before package statement */
                @file:Suppress("aaa", "zzz")
                package foo

                import foo.bar
                import foobar.* // ktlint-disable no-wildcard-imports
                """.trimIndent()
            val formattedCode =
                """
                /* Some copyright notice before package statement */
                @file:Suppress("aaa", "ktlint:standard:no-wildcard-imports", "zzz")
                package foo

                import foo.bar
                import foobar.*
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(6, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given an EOL comment with a ktlint-disable directive` {
        @Test
        fun `Given a ktlint-disable directive without rule-id`() {
            val code =
                """
                val foo = "foo" // ktlint-disable
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive with rule-id not prefixed with a rule set id`() {
            val code =
                """
                val foo = "foo" // ktlint-disable foo
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:foo")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive with rule-id prefixed with a rule set id`() {
            val code =
                """
                val foo = "foo" // ktlint-disable standard:foo
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:foo")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @ParameterizedTest(name = "Rules: {0}")
        @ValueSource(
            strings = [
                // Rule ids are already sorted
                "custom:foo standard:bar standard:foo",
                // Redundant spaces between rule ids should not lead to suppressing all rules by adding "ktlint" as suppression id
                "custom:foo   standard:bar   standard:foo",
                // Duplicate rule ids are ignored
                "standard:bar standard:foo standard:bar custom:foo",
                // Unsorted rule ids are sorted
                "standard:foo custom:foo standard:bar",
            ],
        )
        fun `Given a ktlint-disable directive with multiple rule-ids`(ruleIds: String) {
            val code =
                """
                val foo = "foo" // ktlint-disable $ruleIds
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:custom:foo", "ktlint:standard:bar", "ktlint:standard:foo")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with @Suppress then add the ktlint suppression and sort all suppressions alphabetically`() {
            val code =
                """
                @Suppress("zzz", "aaa")
                val foo = "foo" // ktlint-disable standard:foo
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:standard:foo", "zzz")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(2, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with @SuppressWarnings then add the ktlint suppression and sort all suppressions alphabetically`() {
            val code =
                """
                @SuppressWarnings("aaa", "zzz")
                val foo = "foo" // ktlint-disable standard:foo
                """.trimIndent()
            val formattedCode =
                """
                @SuppressWarnings("aaa", "ktlint:standard:foo", "zzz")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(2, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with both @Suppress and @SuppressWarnings then add the ktlint suppression to the @Suppress`() {
            val code =
                """
                @Suppress("aaa", "zzz")
                @SuppressWarnings("bbb", "yyy")
                val foo = "foo" // ktlint-disable standard:foo
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:standard:foo", "zzz")
                @SuppressWarnings("bbb", "yyy")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(3, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }
    }

    // Note that the embedded kotlin compiler treats the last comment preceding a declaration or expression as integral part of the ASTNode
    // representing that declaration or expression. To match the working SuppressionBuilderLocator of Ktlint it should be treated as a top
    // level comment.
    // To keep test cases below concise, they only contain a ktlint-disable directive in a block comment and no declaration or expression.
    @Nested
    inner class `Given a top level block comment with a ktlint-disable directive` {
        @Test
        fun `Given a ktlint-disable directive without rule-id`() {
            val code =
                """
                /* ktlint-disable */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @ParameterizedTest(name = "Ktlint-disable: {0}")
        @CsvSource(
            quoteCharacter = '"',
            delimiter = '|',
            value = [
                "foo                                | ktlint:standard:foo",
                "standard:foo                       | ktlint:standard:foo",
                "custom:foo                         | ktlint:custom:foo",
                "custom:foo standard:bar            | ktlint:custom:foo,ktlint:standard:bar",
                // Redundant spaces between rule ids should not lead to suppressing all rules by adding "ktlint" as suppression id
                "custom:foo   standard:bar          | ktlint:custom:foo,ktlint:standard:bar",
                // Duplicate rule ids are ignored
                "custom:foo standard:bar custom:foo | ktlint:custom:foo,ktlint:standard:bar",
                // Unsorted rule ids are sorted
                "standard:bar custom:foo            | ktlint:custom:foo,ktlint:standard:bar",
            ],
        )
        fun `Given a top-level ktlint-disable directive`(
            ruleIds: String,
            expectedSuppressionIdString: String,
        ) {
            val code =
                """
                /* ktlint-disable $ruleIds */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress(${
                    expectedSuppressionIdString
                        .split(',')
                        .joinToString { "\"$it\"" }
                })
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with @Suppress then add the ktlint suppression and sort all suppressions alphabetically`() {
            val code =
                """
                @file:Suppress("zzz", "aaa")
                /* ktlint-disable standard:foo */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("aaa", "ktlint:standard:foo", "zzz")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(2, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with @SuppressWarnings then add the ktlint suppression and sort all suppressions alphabetically`() {
            val code =
                """
                @file:SuppressWarnings("aaa", "zzz")
                /* ktlint-disable standard:foo */
                """.trimIndent()
            val formattedCode =
                """
                @file:SuppressWarnings("aaa", "ktlint:standard:foo", "zzz")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(2, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with both @Suppress and @SuppressWarnings then add the ktlint suppression to the @Suppress`() {
            val code =
                """
                @file:Suppress("aaa", "zzz")
                @file:SuppressWarnings("bbb", "yyy")

                /* ktlint-disable standard:foo */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("aaa", "ktlint:standard:foo", "zzz")
                @file:SuppressWarnings("bbb", "yyy")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(4, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with both @SuppressWarnings and @Suppress then add the ktlint suppression to the @Suppress`() {
            val code =
                """
                @file:SuppressWarnings("bbb", "yyy")
                @file:Suppress("aaa", "zzz")

                /* ktlint-disable standard:foo */
                """.trimIndent()
            val formattedCode =
                """
                @file:SuppressWarnings("bbb", "yyy")
                @file:Suppress("aaa", "ktlint:standard:foo", "zzz")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(4, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a pair of matching ktlint directives in block comments within the same parent node` {
        @Test
        fun `Given a ktlint-disable directive without rule-id`() {
            val code =
                """
                fun foo() {
                    /* ktlint-disable */
                    doSomething()
                    /* ktlint-enable */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive with rule-id not prefixed with a rule set id`() {
            val code =
                """
                fun foo() {
                    /* ktlint-disable foo */
                    doSomething()
                    /* ktlint-enable foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:foo")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive with rule-id prefixed with a rule set id`() {
            val code =
                """
                fun foo() {
                    /* ktlint-disable standard:foo */
                    doSomething()
                    /* ktlint-enable standard:foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:foo")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @ParameterizedTest(name = "Rules: {0}")
        @ValueSource(
            strings = [
                "standard:bar standard:foo",
                // Redundant spaces between rule ids should not lead to suppressing all rules by adding "ktlint" as suppression id
                "standard:bar   standard:foo",
                // Duplicate rule ids are ignored
                "standard:bar standard:foo standard:bar",
                // Unsorted rule ids are sorted
                "standard:foo standard:bar",
            ],
        )
        fun `Given a ktlint-disable directive with multiple rule-ids`(ruleIds: String) {
            val code =
                """
                fun foo() {
                    /* ktlint-disable $ruleIds */
                    doSomething()
                    /* ktlint-enable $ruleIds */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with @Suppress then add the ktlint suppression and sort all suppressions alphabetically`() {
            val code =
                """
                @Suppress("zzz", "aaa")
                fun foo() {
                    /* ktlint-disable standard:foo */
                    doSomething()
                    /* ktlint-enable standard:foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:standard:foo", "zzz")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(5, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with @SuppressWarnings then add the ktlint suppression and sort all suppressions alphabetically`() {
            val code =
                """
                @SuppressWarnings("aaa", "zzz")
                fun foo() {
                    /* ktlint-disable standard:foo */
                    doSomething()
                    /* ktlint-enable standard:foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                @SuppressWarnings("aaa", "ktlint:standard:foo", "zzz")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(5, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-disable directive for which the target element is already annotated with both @Suppress and @SuppressWarnings then add the ktlint suppression to the @Suppress`() {
            val code =
                """
                @Suppress("aaa", "zzz")
                @SuppressWarnings("bbb", "yyy")
                fun foo() {
                    /* ktlint-disable standard:foo */
                    doSomething()
                    /* ktlint-enable standard:foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:standard:foo", "zzz")
                @SuppressWarnings("bbb", "yyy")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(6, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Nested
    inner class `Given a ktlint-enable directive` {
        @Test
        fun `Given a ktlint-enable directive matching with a ktlint-disable directive`() {
            val code =
                """
                fun foo() {
                    /* ktlint-disable standard:foo */
                    doSomething()
                    /* ktlint-enable standard:foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:foo")
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                    LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a ktlint-enable directive not matching with a ktlint-disable directive`() {
            val code =
                """
                fun foo() {
                    doSomething()
                    /* ktlint-enable standard:foo */
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foo() {
                    doSomething()
                }
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(3, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a ktlint-disable directive for a specific rule on a declaration which already has suppression annotation for all ktlint rules`() {
        val code =
            """
            @Suppress("ktlint")
            fun foo() {
                bar() // ktlint-disable standard:bar

                /* ktlint-disable standard:foo */
                /* ktlint-disable custom:foo */
                bar()
                /* ktlint-enable custom:foo */
                /* ktlint-enable standard:foo */
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint")
            fun foo() {
                bar()

                bar()
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 14, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(5, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(6, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(8, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                LintViolation(9, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @KtlintDocumentationTest
    fun `Documentation example`() {
        val code =
            """
            /* ktlint-disable standard:no-wildcard-imports */

            class FooBar {
                val foo = "some longggggggggggggggggggg text" // ktlint-disable standard:max-line-length

                fun bar() =
                    listOf(
                        /* ktlint-disable standard:no-multi-spaces */
                        "1   One",
                        "10  Ten",
                        "100 Hundred",
                        /* ktlint-enable standard:no-multi-spaces */
                    )
            }
            """.trimIndent()
        val formattedCode =
            """
            @file:Suppress("ktlint:standard:no-wildcard-imports")

            class FooBar {
                @Suppress("ktlint:standard:max-line-length")
                val foo = "some longggggggggggggggggggg text"

                fun bar() =
                    @Suppress("ktlint:standard:no-multi-spaces")
                    listOf(
                        "1   One",
                        "10  Ten",
                        "100 Hundred",
                    )
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .hasLintViolations(
                LintViolation(1, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(4, 54, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(8, 16, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(12, 16, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @ParameterizedTest(name = "Suppression type: {0}")
    @ValueSource(
        strings = [
            "Suppress",
            "SuppressWarnings",
        ],
    )
    fun `Given multiple ktlint-disable directives which have to merged into an existing @file Suppress annotation`(annotationName: String) {
        val code =
            """
            @file:$annotationName("ktlint:standard:bar")

            import bar // ktlint-disable standard:no-wildcard-imports

            /* ktlint-disable standard:foo */

            val someFoo = foo.TEST
            val someBar = bar.TEST

            /* ktlint-disable custom:foo */
            """.trimIndent()
        val formattedCode =
            """
            @file:$annotationName("ktlint:custom:foo", "ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:no-wildcard-imports")

            import bar

            val someFoo = foo.TEST
            val someBar = bar.TEST
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 15, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(5, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(10, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a block comment containing a ktlint-disable directive inside an init block`() {
        val code =
            """
            class Foo() {
                var foo: String
                var bar: String

                init {
                    /* ktlint-disable standard:foo */
                    foo = "foo"
                    /* ktlint-enable standard:foo */
                }

                init { // ktlint-disable standard:bar
                    bar = "bar"
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
            class Foo() {
                var foo: String
                var bar: String

                init {
                    foo = "foo"
                }

                init {
                    bar = "bar"
                }
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(6, 12, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(8, 12, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                LintViolation(11, 15, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a pair of matching ktlint directives in block comments as siblings in same parent node`() {
        val code =
            """
            fun foobar(
                /* ktlint-disable standard:foo */
                foo: Int,
                bar: Int,
                /* ktlint-enable standard:foo */
            ) {}
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:foo")
            fun foobar(
                foo: Int,
                bar: Int,
            ) {}
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(5, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given ktlint-disable directive in block comment not having a ktlint-enable directive in a sibling in the same parent node` {
        @Test
        fun `Given a ktlint-disable directive root level not related to an declaration or expression then move to @file annotation`() {
            val code =
                """
                /* ktlint-disable standard:foo */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:foo")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given ktlint-disable directive in last block comment before class but not having a ktlint-enable directive`() {
            val code =
                """
                /* ktlint-disable standard:foo */
                class Foo
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:foo")

                class Foo
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given ktlint-disable directive in last block comment before property but not having a ktlint-enable directive`() {
            val code =
                """
                /* ktlint-disable standard:foo */
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:foo")

                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a pair of matching ktlint directives in block comments but not as siblings in same parent node`() {
            val code =
                """
                fun foobar(
                    /* ktlint-disable standard:foo */
                    foo: Int,
                    bar: Int,
                ) {
                    /* ktlint-enable standard:foo */
                    doSomething()
                }
                """.trimIndent()
            val formattedCode =
                """
                fun foobar(
                    /* ktlint-disable standard:foo */
                    foo: Int,
                    bar: Int,
                ) {
                    doSomething()
                }
                """.trimIndent()
            @Suppress("ktlint:standard:argument-list-wrapping", "ktlint:standard:max-line-length")
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. The matching 'ktlint-enable' directive is not found in same scope. Replace with @Suppress annotation", false),
                    LintViolation(6, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given ktlint-disable directive on a package statement`() {
        val code =
            """
            package foo.foo_bar // ktlint-disable standard:package-name
            """.trimIndent()
        val formattedCode =
            """
            @file:Suppress("ktlint:standard:package-name")

            package foo.foo_bar
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolation(1, 24, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an invalid rule id then ignore it without throwing an exception`() {
        val code =
            """
            @file:Suppress("ktlint:standard:SOME-INVALID-RULE-ID-1")

            @Suppress("ktlint:standard:SOME-INVALID-RULE-ID-2")
            class Foo {
                /* ktlint-disable standard:SOME-INVALID-RULE-ID-3 */
                fun bar() {
                    val bar = "bar" // ktlint-disable standard:SOME-INVALID-RULE-ID-4
                }
                /* ktlint-enable standard:SOME-INVALID-RULE-ID-3 */
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 17, "Ktlint rule with id 'ktlint:standard:SOME-INVALID-RULE-ID-1' is unknown or not loaded", false),
                LintViolation(3, 12, "Ktlint rule with id 'ktlint:standard:SOME-INVALID-RULE-ID-2' is unknown or not loaded", false),
                LintViolation(5, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(5, 23, "Ktlint rule with id 'standard:SOME-INVALID-RULE-ID-3' is unknown or not loaded", false),
                LintViolation(7, 28, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(7, 43, "Ktlint rule with id 'standard:SOME-INVALID-RULE-ID-4' is unknown or not loaded", false),
                LintViolation(9, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            )
    }

    @Test
    fun `Given an unknown ktlint rule id then do not create an empty @Suppress annotation`() {
        val code =
            """
            val foo = "foo" // ktlint-disable standard:unknown-rule-id
            """.trimIndent()
        val formattedCode =
            """
            val foo = "foo"
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(1, 35, "Ktlint rule with id 'standard:unknown-rule-id' is unknown or not loaded", false),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a setter with multiple ktlint directives`() {
        val code =
            """
            class Foo {
                var foo: Int = 1
                    set(value) { // ktlint-disable standard:foo
                        field = value // ktlint-disable standard:bar
                        field = value
                    }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                var foo: Int = 1
                    @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
                    set(value) {
                        field = value
                        field = value
                    }
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 25, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(4, 30, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a primary constructor with multiple ktlint directives`() {
        val code =
            """
            class Foo constructor(bar: Bar) {
                /* ktlint-disable standard:bar standard:foo */

                /* ktlint-enable standard:bar standard:foo */
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
            class Foo constructor(bar: Bar) {
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class with a single parameter wrapped between ktlint disable and ktlint enable directives`() {
        val code =
            """
            class Foo(
                /* ktlint-disable standard:bar standard:foo */
                val bar: Bar
                /* ktlint-enable standard:bar standard:foo */
            )
            """.trimIndent()
        val formattedCode =
            """
            class Foo(
                @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
                val bar: Bar
            )
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(4, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class with multiple parameters wrapped between ktlint disable and ktlint enable directives`() {
        val code =
            """
            class Foo(
                /* ktlint-disable standard:bar standard:foo */
                val bar1: Bar,
                val bar2: Bar,
                /* ktlint-enable standard:bar standard:foo */
            )
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
            class Foo(
                val bar1: Bar,
                val bar2: Bar,
            )
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(5, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a ktlint-disable block directive around a single declaration then place the @Suppress on the declaration`() {
        val code =
            """
            class Foobar {
                val bar = "bar"

                /* ktlint-disable standard:bar standard:foo */
                fun foo() {}
                /* ktlint-enable standard:bar standard:foo */
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foobar {
                val bar = "bar"

                @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
                fun foo() {}
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(6, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a ktlint-disable block directive around multiple declarations then place the @Suppress on the declaration`() {
        val code =
            """
            class Foobar {
                /* ktlint-disable standard:bar standard:foo */
                val bar = "bar"

                fun foo() {}
                /* ktlint-enable standard:bar standard:foo */
            }
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:bar", "ktlint:standard:foo")
            class Foobar {
                val bar = "bar"

                fun foo() {}
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 8, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(6, 8, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a declaration with a @Suppress annotation using a named argument and a ktlint-disable directive`() {
        val code =
            """
            @Suppress(names = ["unused"])
            val foo = "foo" // ktlint-disable standard:foo
            """.trimIndent()
        val formattedCode =
            """
            @Suppress("ktlint:standard:foo", "unused")
            val foo = "foo"
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolation(2, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a property delegate with a ktlint-disable directive`() {
        val code =
            """
            val foo by lazy(LazyThreadSafetyMode.PUBLICATION) { // ktlint-disable standard:foo
                // do something
            }
            """.trimIndent()
        val formattedCode =
            """
            val foo by @Suppress("ktlint:standard:foo")
            lazy(LazyThreadSafetyMode.PUBLICATION) {
                // do something
            }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolation(1, 56, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a nested expression with a ktlint-disable directive`() {
        val code =
            """
            val foo =
                setOf("a")
                    .map {
                        bar(it) // ktlint-disable standard:foo
                    }
            """.trimIndent()
        val formattedCode =
            """
            val foo =
                @Suppress("ktlint:standard:foo")
                setOf("a")
                    .map {
                        bar(it)
                    }
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolation(4, 24, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an expression containing a ktlint block directive then change the expression to an annotated expression with an @Suppress annotation`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                          $EOL_CHAR
            fun optionalInputTestArguments(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(
                        "foo",
                        "bar"
                    ),
                    /* ktlint-disable */
                    Arguments.of("fooooooooooooooooooooooo","bar"),
                    /* ktlint-enable */
                )
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                          $EOL_CHAR
            fun optionalInputTestArguments(): Stream<Arguments> =
                Stream.of(
                    Arguments.of(
                        "foo",
                        "bar"
                    ),
                    @Suppress("ktlint")
                    Arguments.of("fooooooooooooooooooooooo","bar"),
                )
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalRuleProvider { ArgumentListWrappingRule() }
            .addRequiredRuleProviderDependenciesFrom(StandardRuleSetProvider())
            .hasLintViolations(
                LintViolation(8, 12, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(10, 12, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations"),
            ).isFormattedAs(formattedCode)
    }
}

private class DummyRule(
    id: String,
) : Rule(RuleId(id), About())
