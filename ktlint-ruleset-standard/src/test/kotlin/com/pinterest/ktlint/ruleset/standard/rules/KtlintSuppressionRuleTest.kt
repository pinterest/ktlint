package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class KtlintSuppressionRuleTest {
    private val ktlintSuppressionRuleAssertThat =
        KtLintAssertThat.assertThatRule { KtlintSuppressionRule() }

    @Nested
    inner class `Given a suppression annotation missing the rule set id prefix` {
        @Test
        fun `Given a @file Suppress annotation`() {
            val code =
                """
                @file:Suppress("ktlint:bar", "ktlint:standard:foo", "ktlint:foo-bar")
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:foo-bar")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 24, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 61, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a @file SuppressWarnings annotation`() {
            val code =
                """
                @file:SuppressWarnings("ktlint:bar", "ktlint:standard:foo", "ktlint:foo-bar")
                """.trimIndent()
            val formattedCode =
                """
                @file:SuppressWarnings("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:foo-bar")
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 32, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 69, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a @file array annotation with Suppress and SuppressWarnings annotations`() {
            val code =
                """
                @file:[Suppress("ktlint:bar", "ktlint:standard:foo") SuppressWarnings("ktlint:foo-bar")]
                """.trimIndent()
            val formattedCode =
                """
                @file:[Suppress("ktlint:standard:bar", "ktlint:standard:foo") SuppressWarnings("ktlint:standard:foo-bar")]
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 25, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 79, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an array annotation with Suppress and SuppressWarnings annotations`() {
            val code =
                """
                @[Suppress("ktlint:bar", "ktlint:standard:foo") SuppressWarnings("ktlint:foo-bar")]
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @[Suppress("ktlint:standard:bar", "ktlint:standard:foo") SuppressWarnings("ktlint:standard:foo-bar")]
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 20, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 74, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a Suppress annotation with a named argument with an arrayOf initialization`() {
            val code =
                """
                @Suppress(names = arrayOf("ktlint:bar", "ktlint:standard:foo", "ktlint:foo-bar"))
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @Suppress(names = arrayOf("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:foo-bar"))
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 35, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 72, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a Suppress annotation with a named argument with an array (squared brackets) initialization`() {
            val code =
                """
                @Suppress(names = ["ktlint:bar", "ktlint:standard:foo", "ktlint:foo-bar"])
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @Suppress(names = ["ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:foo-bar"])
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 28, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 65, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a Suppress annotation on a declaration`() {
            val code =
                """
                @Suppress("ktlint:bar", "ktlint:standard:foo", "ktlint:foo-bar")
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:foo-bar")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 19, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 56, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a SuppressWarnings annotation on a declaration`() {
            val code =
                """
                @SuppressWarnings("ktlint:bar", "ktlint:standard:foo", "ktlint:foo-bar")
                val foo = "foo"
                """.trimIndent()
            val formattedCode =
                """
                @SuppressWarnings("ktlint:standard:bar", "ktlint:standard:foo", "ktlint:standard:foo-bar")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 27, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                    LintViolation(1, 64, "Identifier to suppress ktlint rule must be fully qualified with the rule set id"),
                ).isFormattedAs(formattedCode)
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
                val foo = "foo" // ktlint-disable some-rule-id
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:some-rule-id")
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
                val foo = "foo" // ktlint-disable some-rule-set:some-rule-id
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:some-rule-set:some-rule-id")
                val foo = "foo"
                """.trimIndent()
            ktlintSuppressionRuleAssertThat(code)
                .hasLintViolation(1, 20, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation")
                .isFormattedAs(formattedCode)
        }

        @ParameterizedTest(name = "Rules: {0}")
        @ValueSource(
            strings = [
                "some-rule-set-1:some-rule-id-1 some-rule-set-2:some-rule-id-2",
                // Redundant spaces between rule ids should not lead to suppressing all rules by adding "ktlint" as suppression id
                "some-rule-set-1:some-rule-id-1   some-rule-set-2:some-rule-id-2",
                // Duplicate rule ids are ignored
                "some-rule-set-1:some-rule-id-1 some-rule-set-2:some-rule-id-2 some-rule-set-1:some-rule-id-1",
                // Unsorted rule ids are sorted
                "some-rule-set-2:some-rule-id-2 some-rule-set-1:some-rule-id-1",
            ],
        )
        fun `Given a ktlint-disable directive with multiple rule-ids`(ruleIds: String) {
            val code =
                """
                val foo = "foo" // ktlint-disable $ruleIds
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:some-rule-set-1:some-rule-id-1", "ktlint:some-rule-set-2:some-rule-id-2")
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
                val foo = "foo" // ktlint-disable some-rule-set:some-rule-id
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                val foo = "foo" // ktlint-disable some-rule-set:some-rule-id
                """.trimIndent()
            val formattedCode =
                """
                @SuppressWarnings("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                val foo = "foo" // ktlint-disable some-rule-set:some-rule-id
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                "foo                                   | ktlint:standard:foo",
                "standard:foo                          | ktlint:standard:foo",
                "custom-rule-set:foo                   | ktlint:custom-rule-set:foo",
                "set-a:rule-a set-b:rule-b             | ktlint:set-a:rule-a,ktlint:set-b:rule-b",
                // Redundant spaces between rule ids should not lead to suppressing all rules by adding "ktlint" as suppression id
                "set-a:rule-a   set-b:rule-b           | ktlint:set-a:rule-a,ktlint:set-b:rule-b",
                // Duplicate rule ids are ignored
                "set-a:rule-a set-b:rule-b set-a:rule-a| ktlint:set-a:rule-a,ktlint:set-b:rule-b",
                // Unsorted rule ids are sorted
                "set-b:rule-b set-a:rule-a             | ktlint:set-a:rule-a,ktlint:set-b:rule-b",
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
                @file:Suppress(${expectedSuppressionIdString
                    .split(',')
                    .joinToString { "\"$it\"" }})
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
                /* ktlint-disable some-rule-set:some-rule-id */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                /* ktlint-disable some-rule-set:some-rule-id */
                """.trimIndent()
            val formattedCode =
                """
                @file:SuppressWarnings("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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

                /* ktlint-disable some-rule-set:some-rule-id */
                """.trimIndent()
            val formattedCode =
                """
                @file:Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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

                /* ktlint-disable some-rule-set:some-rule-id */
                """.trimIndent()
            val formattedCode =
                """
                @file:SuppressWarnings("bbb", "yyy")
                @file:Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                    /* ktlint-disable some-rule-id */
                    doSomething()
                    /* ktlint-enable some-rule-id */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:standard:some-rule-id")
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
                    /* ktlint-disable some-rule-set:some-rule-id */
                    doSomething()
                    /* ktlint-enable some-rule-set:some-rule-id */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:some-rule-set:some-rule-id")
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
                "some-rule-set-1:some-rule-id-1 some-rule-set-2:some-rule-id-2",
                // Redundant spaces between rule ids should not lead to suppressing all rules by adding "ktlint" as suppression id
                "some-rule-set-1:some-rule-id-1   some-rule-set-2:some-rule-id-2",
                // Duplicate rule ids are ignored
                "some-rule-set-1:some-rule-id-1 some-rule-set-2:some-rule-id-2 some-rule-set-1:some-rule-id-1",
                // Unsorted rule ids are sorted
                "some-rule-set-2:some-rule-id-2 some-rule-set-1:some-rule-id-1",
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
                @Suppress("ktlint:some-rule-set-1:some-rule-id-1", "ktlint:some-rule-set-2:some-rule-id-2")
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
                    /* ktlint-disable some-rule-set:some-rule-id */
                    doSomething()
                    /* ktlint-enable some-rule-set:some-rule-id */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                    /* ktlint-disable some-rule-set:some-rule-id */
                    doSomething()
                    /* ktlint-enable some-rule-set:some-rule-id */
                }
                """.trimIndent()
            val formattedCode =
                """
                @SuppressWarnings("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                    /* ktlint-disable some-rule-set:some-rule-id */
                    doSomething()
                    /* ktlint-enable some-rule-set:some-rule-id */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("aaa", "ktlint:some-rule-set:some-rule-id", "zzz")
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
                    /* ktlint-disable some-rule-set:some-rule-id */
                    doSomething()
                    /* ktlint-enable some-rule-set:some-rule-id */
                }
                """.trimIndent()
            val formattedCode =
                """
                @Suppress("ktlint:some-rule-set:some-rule-id")
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
                    /* ktlint-enable some-rule-set:some-rule-id */
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
                bar() // ktlint-disable some-rule-set:some-rule-id-1

                /* ktlint-disable some-rule-set:some-rule-id-2 */
                /* ktlint-disable some-rule-set:some-rule-id-3 */
                bar()
                /* ktlint-enable some-rule-set:some-rule-id-3 */
                /* ktlint-enable some-rule-set:some-rule-id-2 */
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
            @file:$annotationName("ktlint:standard:ccc", "ktlint:standard:ddd")

            import foo // ktlint-disable foo
            import bar // ktlint-disable bar

            /* ktlint-disable fff */

            val someFoo = foo.TEST
            val someBar = bar.TEST

            /* ktlint-disable aaa */
            """.trimIndent()
        val formattedCode =
            """
            @file:$annotationName("ktlint:standard:aaa", "ktlint:standard:bar", "ktlint:standard:ccc", "ktlint:standard:ddd", "ktlint:standard:fff", "ktlint:standard:foo")

            import foo
            import bar

            val someFoo = foo.TEST
            val someBar = bar.TEST
            """.trimIndent()
        ktlintSuppressionRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(3, 15, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(4, 15, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(6, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
                LintViolation(11, 4, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a BLOCK comment containing a ktlint-disable directive inside an init block`() {
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
        fun `G1iven a ktlint-disable directive root level not related to an declaration or expression then move to @file annotation`() {
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
}
