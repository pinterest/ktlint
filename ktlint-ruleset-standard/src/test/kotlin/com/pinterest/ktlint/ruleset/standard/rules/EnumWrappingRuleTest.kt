package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EnumWrappingRuleTest {
    private val enumWrappingRuleAssertThat = KtLintAssertThat.assertThatRule { EnumWrappingRule() }

    @Test
    fun `Given a single line enum class which does not need to be wrapped`() {
        val code =
            """
            enum class Foo { A, B, C, D }
            """.trimIndent()
        enumWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multi line enum class which does not need to be wrapped`() {
        val code =
            """
            enum class Foo {
                A,
                B,
                C,
                D
            }
            """.trimIndent()
        enumWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given an annotated enum entry` {
        @Test
        fun `Given an enum class with an enum entry with a single annotation`() {
            val code =
                """
                enum class Foo { @Bar BAR }
                """.trimIndent()
            val formattedCode =
                """
                enum class Foo {
                    @Bar BAR
                }
                """.trimIndent()
            enumWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { AnnotationRule() }
                .hasLintViolations(
                    LintViolation(1, 18, "Enum entry should start on a separate line"),
                    LintViolation(1, 27, "Expected newline before '}'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an enum class with multiple annotated enum entries`() {
            val code =
                """
                enum class Foo { A, @Bar1 B, @Bar1 @Bar2 C, @Bar3("bar3") @Bar1 D }
                """.trimIndent()
            val formattedCode =
                """
                enum class Foo {
                    A,
                    @Bar1 B,
                    @Bar1 @Bar2
                    C,
                    @Bar3("bar3")
                    @Bar1
                    D
                }
                """.trimIndent()
            enumWrappingRuleAssertThat(code)
                .addAdditionalRuleProvider { AnnotationRule() }
                .hasLintViolations(
                    LintViolation(1, 18, "Enum entry should start on a separate line"),
                    LintViolation(1, 21, "Enum entry should start on a separate line"),
                    LintViolation(1, 30, "Enum entry should start on a separate line"),
                    LintViolation(1, 45, "Enum entry should start on a separate line"),
                    LintViolation(1, 67, "Expected newline before '}'"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a multiline enum class then each entry should be on a separate line`() {
        val code =
            """
            enum class Foo {
                A, B
            }
            """.trimIndent()
        val formattedCode =
            """
            enum class Foo {
                A,
                B
            }
            """.trimIndent()
        enumWrappingRuleAssertThat(code)
            .hasLintViolation(2, 8, "Enum entry should start on a separate line")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a enum class without blank line between enum entries and other declarations`() {
        val code =
            """
            enum class Foo {
                A,
                B;
                fun foo() = "foo"
            }
            """.trimIndent()
        val formattedCode =
            """
            enum class Foo {
                A,
                B;

                fun foo() = "foo"
            }
            """.trimIndent()
        enumWrappingRuleAssertThat(code)
            .hasLintViolation(4, 1, "Expected blank line between enum entries and other declaration(s)")
            .isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given an enum with comments` {
        @Test
        fun `Given a single line enum class containing a block comment but not entries`() {
            val code =
                """
                enum class Foo { /* empty */ }
                """.trimIndent()
            enumWrappingRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a single line enum class with a block comment before the enum entries`() {
            val code =
                """
                enum class Foo { /* comment */ A, B }
                """.trimIndent()
            val formattedCode =
                """
                enum class Foo {
                    /* comment */ A,
                    B
                }
                """.trimIndent()
            enumWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 16, "Expected a (single) newline before comment"),
                    LintViolation(1, 32, "Enum entry should start on a separate line"),
                    LintViolation(1, 35, "Enum entry should start on a separate line"),
                    LintViolation(1, 37, "Expected newline before '}'"),
                ).isFormattedAs(formattedCode)
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single line enum class with a block comment after an enum entry`() {
            val code =
                """
                enum class Foo { A /* comment */, B }
                """.trimIndent()
            val formattedCode =
                """
                enum class Foo {
                    A /* comment */,
                    B
                }
                """.trimIndent()
            enumWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 18, "Enum entry should start on a separate line"),
                    LintViolation(1, 35, "Enum entry should start on a separate line"),
                    LintViolation(1, 37, "Expected newline before '}'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single line enum class with a block comments before an enum entry`() {
            val code =
                """
                enum class Foo { A, /* comment */ B }
                """.trimIndent()
            val formattedCode =
                """
                enum class Foo {
                    A,
                    /* comment */ B
                }
                """.trimIndent()
            enumWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 18, "Enum entry should start on a separate line"),
                    LintViolation(1, 35, "Enum entry should start on a separate line"),
                    LintViolation(1, 37, "Expected newline before '}'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a single line enum class with a block comment after the last enum entry`() {
            val code =
                """
                enum class Foo { A, B /* comment */ }
                """.trimIndent()
            val formattedCode =
                """
                enum class Foo {
                    A,
                    B /* comment */
                }
                """.trimIndent()
            enumWrappingRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 18, "Enum entry should start on a separate line"),
                    LintViolation(1, 21, "Enum entry should start on a separate line"),
                    LintViolation(1, 37, "Expected newline before '}'"),
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given an enum class without body`() {
        val code =
            """
            enum class Foo
            """.trimIndent()
        enumWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an enum class without enum entries in the body`() {
        val code =
            """
            enum class Foo {}
            """.trimIndent()
        enumWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2510 - Given an enum class with comment before the first entry`() {
        val code =
            """
            enum class FooBar {

                // Some comment about FooBar

                // Foo
                Foo,
            }
            """.trimIndent()
        val formattedCode =
            """
            enum class FooBar {
                // Some comment about FooBar

                // Foo
                Foo,
            }
            """.trimIndent()
        enumWrappingRuleAssertThat(code)
            .hasLintViolation(1, 19, "Expected a (single) newline before comment")
            .isFormattedAs(formattedCode)
    }
}
