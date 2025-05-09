package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import com.pinterest.ktlint.test.MULTILINE_STRING_QUOTE
import com.pinterest.ktlint.test.replaceStringTemplatePlaceholder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringTemplateRuleTest {
    private val stringTemplateRuleAssertThat = assertThatRule { StringTemplateRule() }

    @Test
    fun `Given some redundant toString calls`() {
        // Interpret "$." in code samples below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            val foo1 = "$.{String::class.toString()}"
            val foo2 = $MULTILINE_STRING_QUOTE$.{Int::class.toString()}$MULTILINE_STRING_QUOTE

            class Foo() {
                override fun toString(): String = "Override hashcode = $.{super.hashCode().toString()}"
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        val formattedCode =
            """
            val foo1 = "${'$'}.{String::class}"
            val foo2 = $MULTILINE_STRING_QUOTE${'$'}.{Int::class}$MULTILINE_STRING_QUOTE

            class Foo() {
                override fun toString(): String = "Override hashcode = ${'$'}.{super.hashCode()}"
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 28, "Redundant \".toString()\" call in string template"),
                LintViolation(2, 27, "Redundant \".toString()\" call in string template"),
                LintViolation(5, 78, "Redundant \".toString()\" call in string template"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a redundant toString call for which also the curly braces become redundant after removal of toString`() {
        // Interpret "$." in code samples below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            val foo = "$.{foo.toString()}"
            """.trimIndent().replaceStringTemplatePlaceholder()
        val formattedCode =
            """
            val foo = "${'$'}.foo"
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code)
            .hasLintViolation(1, 17, "Redundant \".toString()\" call in string template")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given 'super' as receiver, followed by a toString call then do not remove the toString call`() {
        // Interpret "$." in code samples below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            val foo = "$.{super.toString()}"
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some redundant curly braces`() {
        // Interpret "$." in code sample below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            val foo1 = "${'$'}.{foo}.hello"
            val foo2 = "${'$'}.{foo}"
            """.trimIndent().replaceStringTemplatePlaceholder()
        val formattedCode =
            """
            val foo1 = "${'$'}.foo.hello"
            val foo2 = "${'$'}.foo"
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 14, "Redundant curly braces"),
                LintViolation(2, 14, "Redundant curly braces"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some string template not wrapped in curly braces`() {
        // Interpret "$." in code sample below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            val foo1 = "$.bar"
            val foo2 = $MULTILINE_STRING_QUOTE$.bar$MULTILINE_STRING_QUOTE
            val foo3 = "$.bar.length"
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some string templates wrapped in curly braces which are not redundant`() {
        // Interpret "$." in code sample below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            val foo1 = "$.{bar}length"
            val foo2 = "$.{bar.length}.hello"
            val foo3 = "bar.length is $.{bar.length}"
            val foo4 = "${'$'}9.99"
            val foo5 = "Found: $.{if (bar > 0) "yes" else "no"}"
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given the Suppress annotation on RemoveCurlyBracesFromTemplate` {
        @Test
        fun `Given a function declaration annotated with Suppress RemoveCurlyBracesFromTemplate`() {
            val code =
                """
                fun foo1() = "Foo = $.{foo}"

                @Suppress("RemoveCurlyBracesFromTemplate")
                fun foo2() = "Foo = $.{foo}"

                @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                fun foo3() = "Foo = $.{foo}"
                """.trimIndent().replaceStringTemplatePlaceholder()
            val formattedCode =
                """
                fun foo1() = "Foo = ${'$'}.foo"

                @Suppress("RemoveCurlyBracesFromTemplate")
                fun foo2() = "Foo = ${'$'}.{foo}"

                @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                fun foo3() = "Foo = ${'$'}.{foo}"
                """.trimIndent().replaceStringTemplatePlaceholder()
            stringTemplateRuleAssertThat(code)
                .hasLintViolation(1, 17, "Redundant curly braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a variable declaration annotated with Suppress RemoveCurlyBracesFromTemplate`() {
            val code =
                """
                val foo1 = "Foo = $.{foo}"

                @Suppress("RemoveCurlyBracesFromTemplate")
                val foo2 = "Foo = $.{foo}"

                @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                val foo3 = "Foo = $.{foo}"
                """.trimIndent().replaceStringTemplatePlaceholder()
            val formattedCode =
                """
                val foo1 = "Foo = ${'$'}.foo"

                @Suppress("RemoveCurlyBracesFromTemplate")
                val foo2 = "Foo = ${'$'}.{foo}"

                @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                val foo3 = "Foo = ${'$'}.{foo}"
                """.trimIndent().replaceStringTemplatePlaceholder()
            stringTemplateRuleAssertThat(code)
                .hasLintViolation(1, 15, "Redundant curly braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a statement annotated with Suppress RemoveCurlyBracesFromTemplate`() {
            val code =
                """
                fun foo() {
                    println("Foo = ${'$'}.{foo}")
                    @Suppress("RemoveCurlyBracesFromTemplate")
                    println("Foo = $.{foo}")
                    @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                    println("Foo = ${'$'}.{foo}")
                }
                """.trimIndent().replaceStringTemplatePlaceholder()
            val formattedCode =
                """
                fun foo() {
                    println("Foo = ${'$'}.foo")
                    @Suppress("RemoveCurlyBracesFromTemplate")
                    println("Foo = ${'$'}.{foo}")
                    @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                    println("Foo = ${'$'}.{foo}")
                }
                """.trimIndent().replaceStringTemplatePlaceholder()
            stringTemplateRuleAssertThat(code)
                .hasLintViolation(2, 16, "Redundant curly braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a getter annotated with Suppress RemoveCurlyBracesFromTemplate`() {
            val code =
                """
                class Foo {
                    val foo1
                        get() = "Foo = $.{foo}"

                    @Suppress("RemoveCurlyBracesFromTemplate")
                    val foo2
                        get() = "Foo = $.{foo}"

                    @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                    val foo3
                        get() = "Foo = $.{foo}"
                }
                """.trimIndent().replaceStringTemplatePlaceholder()
            val formattedCode =
                """
                class Foo {
                    val foo1
                        get() = "Foo = ${'$'}.foo"

                    @Suppress("RemoveCurlyBracesFromTemplate")
                    val foo2
                        get() = "Foo = ${'$'}.{foo}"

                    @Suppress("RemoveCurlyBracesFromTemplate", "OtherSuppression")
                    val foo3
                        get() = "Foo = ${'$'}.{foo}"
                }
                """.trimIndent().replaceStringTemplatePlaceholder()
            stringTemplateRuleAssertThat(code)
                .hasLintViolation(3, 20, "Redundant curly braces")
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a class annotated with Suppress RemoveCurlyBracesFromTemplate`() {
            val code =
                """
                class Foo1 {
                    val foo
                        get() = "Foo = $.{foo}"
                }
                @Suppress("RemoveCurlyBracesFromTemplate")
                class Foo2 {
                    val foo
                        get() = "Foo = $.{foo}"
                }
                @Suppress("RemoveCurlyBracesFromTemplate")
                class Foo3 {
                    val foo
                        get() = "Foo = ${'$'}.{foo}"
                }
                """.trimIndent().replaceStringTemplatePlaceholder()
            val formattedCode =
                """
                class Foo1 {
                    val foo
                        get() = "Foo = ${'$'}.foo"
                }
                @Suppress("RemoveCurlyBracesFromTemplate")
                class Foo2 {
                    val foo
                        get() = "Foo = ${'$'}.{foo}"
                }
                @Suppress("RemoveCurlyBracesFromTemplate")
                class Foo3 {
                    val foo
                        get() = "Foo = ${'$'}.{foo}"
                }
                """.trimIndent().replaceStringTemplatePlaceholder()
            stringTemplateRuleAssertThat(code)
                .hasLintViolation(3, 20, "Redundant curly braces")
                .isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given 'this' keyword in string-template wrapped in curly braces`() {
        val code =
            """
            class Foo {
                fun foo() {
                    println("$.{this}")
                    println("$.{this@F}")
                }
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        val formattedCode =
            """
            class Foo {
                fun foo() {
                    println("${'$'}.this")
                    println("${'$'}.{this@F}")
                }
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code)
            .hasLintViolation(3, 19, "Redundant curly braces")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some keywords in string-template wrapped in curly braces`() {
        val code =
            """
            class F {
                fun keyword() {
                    println("$.{null}")
                    println("$.{true}")
                    println("$.{false}")
                }
            }
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 996, 1044 - Given a string-template with a redundant toString call should not lead to a failure in the UnusedImportsRule`() {
        val code =
            """
            fun foo() = 1
            fun test() {
                println("${'$'}{foo().toString()}")
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() = 1
            fun test() {
                println("${'$'}{foo()}")
            }
            """.trimIndent()
        stringTemplateRuleAssertThat(code)
            .addAdditionalRuleProvider { NoUnusedImportsRule() }
            .hasLintViolation(3, 21, "Redundant \".toString()\" call in string template")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a string template immediately followed by non-whitespace character`() {
        val code =
            """
            fun test(foo: String, bar: String) {
                println("${'$'}{foo}text:${'$'}bar")
            }
            """.trimIndent()
        stringTemplateRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2615 - Given a string template with redundant curly braces then do not remove the corresponding import`() {
        // Interpret "$." in code samples below as "$". It is used whenever the code which has to be inspected should
        // actually contain a string template. Using "$" instead of "$." would result in a String in which the string
        // templates would have been evaluated before the code would actually be processed by the rule.
        val code =
            """
            import java.io.File.separator

            val s = "$.{separator} is a file separator"
            """.trimIndent().replaceStringTemplatePlaceholder()
        val formattedCode =
            """
            import java.io.File.separator

            val s = "$.separator is a file separator"
            """.trimIndent().replaceStringTemplatePlaceholder()
        stringTemplateRuleAssertThat(code)
            .addAdditionalRuleProvider { NoUnusedImportsRule() }
            .hasNoLintViolationsForRuleId(NO_UNUSED_IMPORTS_RULE_ID)
            .isFormattedAs(formattedCode)
    }
}
