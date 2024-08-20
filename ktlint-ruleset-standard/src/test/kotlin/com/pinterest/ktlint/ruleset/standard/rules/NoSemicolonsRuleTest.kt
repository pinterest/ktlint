package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoSemicolonsRuleTest {
    private val noSemicolonsRuleAssertThat = assertThatRule { NoSemicolonsRule() }

    @Test
    fun `Given a package statement followed by a comma then return a lint error`() {
        val code =
            """
            package a.b.c;
            """.trimIndent()
        val formattedCode =
            """
            package a.b.c
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(1, 14, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple statements on a single line then do no return a lint error`() {
        val code =
            """
            val foo = "foo"; val bar = "bar"
            fun name() { a(); return b }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a semi colon at the start of a line and not followed by code or comment on the same line then do report a lint error`() {
        val code =
            """
            fun foo() {
                ;
                bar()
                ;

                bar()

                ;
            }
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {
                bar()

                bar()
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 5, "Unnecessary semicolon"),
                LintViolation(4, 5, "Unnecessary semicolon"),
                LintViolation(8, 5, "Unnecessary semicolon"),
            ).isFormattedAs(formattedCode)
    }

    @Disabled("To be implemented")
    @Test
    fun `Given a semi colon at the start of a line and followed by code or comment on the same line then do report a lint error`() {
        val code =
            """
            ;val fooBar = "fooBar"
            ;// some comment
            """.trimIndent()
        val formattedCode =
            """
            val fooBar = "fooBar"
            // some comment
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(1, 1, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Disabled("To be implemented")
    @Test
    fun `Given a semi colon at the start of a block then do report a lint error`() {
        val code =
            """
            fun name() { ; a() }
            """.trimIndent()
        val formattedCode =
            """
            fun name() { a() }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(1, 13, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Disabled("To be implemented")
    @Test
    fun `Given a semi colon at end of a block then do report a lint error`() {
        val code =
            """
            fun name() { a(); }
            """.trimIndent()
        val formattedCode =
            """
            fun name() { a() }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(1, 16, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a call to function with an optional lambda as last parameter which is omitted, followed by function call on a lambda receiver requires a semicolon in between`() {
        val code =
            """
            fun fooBar1() {
                // Removing this semicolon on next line results in compile errors!
                foo("some-foo");
                {
                    // Do something
                }.bar()
            }

            fun fooBar2() {
                // Removing this semicolon on next line results in compile errors!
                foo("some-foo"); { /* Do something */ }.bar()
            }

            // This code is here just to demonstrate why the semicolon after the call to foo is important when the
            // optional lambda is omitted
            fun foo(input: String, block: ((String) -> Unit)? = null) {
                if (block != null) {
                    block(input)
                } else {
                    input
                }
            }

            // This code is here just to demonstrate why the semicolon after the call to foo is important when the
            // optional lambda is omitted
            fun <R> (() -> R).bar() {
                // Do something
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given multiple statements on a single line and semicolon not followed by space then do return a lint error`() {
        val code =
            """
            fun name() { a();return b }
            """.trimIndent()
        val formattedCode =
            """
            fun name() { a(); return b }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(1, 18, "Missing spacing after \";\"")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function declaration followed by a comma then return a lint error`() {
        val code =
            """
            fun main() {};
            """.trimIndent()
        val formattedCode =
            """
            fun main() {}
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(1, 14, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 281 - Given a companion object without block and followed by semicolon then do not return a lint error`() {
        val code =
            """
            class A1 {
                companion object;
            }
            class A2 {
                companion object ;
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 281 - Given a companion object followed by a block and semicolon then do return a lint error`() {
        val code =
            """
            class A {
                companion object {
                    const val emptyString = ""
                };
            }
            """.trimIndent()
        val formattedCode =
            """
            class A {
                companion object {
                    const val emptyString = ""
                }
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(4, 6, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a KDoc with an identifier followed by a semicolon then do not return a lint error`() {
        val code =
            """
            /**
             * [x];
             */
            fun foo() {
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a lambda call followed by an annotated block then do not return a lint error`() {
        val code =
            """
            annotation class Ann
            val f: () -> String = run {
                listOf(1).map {
                    it + it
                };
                // Removing the semicolon on the line above results in a compile error!
                @Ann
                { "" }
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a lambda call followed by a block then do not return a lint error`() {
        val code =
            """
            val f: () -> String = run {
                listOf(1).map {
                    it + it
                };
                // Removing the semicolon on the line above results in a compile error!
                { "" }
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a lambda call followed by a block preceded with a KDoc then do not return a lint error`() {
        val code =
            """
            val f: () -> String = run {
                listOf(1).map {
                    it + it
                };
                /**
                 * kdoc
                 */
                { "" }
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Nested
    inner class `Given an enumeration` {
        @Test
        fun `Given an enumeration and the list of values is followed by a declaration then do not return a lint error`() {
            val code =
                """
                enum class E1 {
                    ONE, TWO;
                    fun fn() {}
                }
                enum class E2 {
                    ONE, TWO;
                    val foo = "foo"
                }
                enum class E3 {
                    ONE, TWO;
                    private companion object {
                       val foo = "foo"
                   }
                }
                """.trimIndent()
            noSemicolonsRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Issue 1733 - Given an enumeration with primary constructor and the list of values is followed by a code element then do not return a lint error`() {
            val code =
                """
                enum class E1(bar: String) {
                    ONE("one"),
                    TWO("two");
                    fun fn() {}
                }
                enum class E2(bar: String) {
                    ONE("one"),
                    TWO("two");
                    val foo = "foo"
                }
                enum class E3(bar: String) {
                    ONE("one"),
                    TWO("two");
                    private companion object {
                       val foo = "foo"
                   }
                }
                """.trimIndent()
            noSemicolonsRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given an enumeration and the list of values is closed with an EOL comment followed by a semicolon on the next line then do not return a lint error`() {
            val code =
                """
                enum class Test {
                    ONE
                    // comment
                    ;
                }
                enum class Foo(bar: String) {
                    ONE("one")
                    // comment
                    ;
                }
                """.trimIndent()
            val formattedCode =
                """
                enum class Test {
                    ONE
                    // comment
                }
                enum class Foo(bar: String) {
                    ONE("one")
                    // comment
                }
                """.trimIndent()
            noSemicolonsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 5, "Unnecessary semicolon"),
                    LintViolation(9, 5, "Unnecessary semicolon"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given an enumeration and the list of values is closed with a semicolon not followed by statements then do return a lint error`() {
            val code =
                """
                enum class E1 {
                    A,
                    B;
                }
                enum class E2 {
                    A,
                    B // comment
                    ;
                }
                enum class E3 {
                    ;
                }
                enum class E4 {
                    // comment
                    ;
                }
                """.trimIndent()
            noSemicolonsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 6, "Unnecessary semicolon"),
                    LintViolation(8, 5, "Unnecessary semicolon"),
                    LintViolation(11, 5, "Unnecessary semicolon"),
                    LintViolation(15, 5, "Unnecessary semicolon"),
                )
        }

        @Test
        fun `Given an enumeration with a primary constructor and the list of values is closed with a semicolon not followed by statements then do return a lint error`() {
            val code =
                """
                enum class Foo1(bar: String) {
                    A(1),
                    B(2);
                }
                enum class Foo2(bar: String) {
                    A(1),
                    B(2) // comment
                    ;
                }
                enum class Foo3(bar: String) {
                    ;
                }
                enum class Foo4(bar: String) {
                    // comment
                    ;
                }
                """.trimIndent()
            noSemicolonsRuleAssertThat(code)
                .hasLintViolations(
                    LintViolation(3, 9, "Unnecessary semicolon"),
                    LintViolation(8, 5, "Unnecessary semicolon"),
                    LintViolation(11, 5, "Unnecessary semicolon"),
                    LintViolation(15, 5, "Unnecessary semicolon"),
                )
        }
    }

    @Test
    fun `Given a for loop with an empty body then do not report a lint error`() {
        val code =
            """
            fun test(list: List<Int>) {
                for (i in list);
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a while loop with an empty body then do not report a lint error`() {
        val code =
            """
            fun test() {
                while (true);
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a do-while loop with an empty body then do report a lint error`() {
        val code =
            """
            fun test() {
                do while (true);
            }
            """.trimIndent()
        val formattedCode =
            """
            fun test() {
                do while (true)
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code)
            .hasLintViolation(2, 20, "Unnecessary semicolon")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an if-statement with an empty body then do not report a lint error`() {
        val code =
            """
            fun testIf() {
                if (true);
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2771 - Given an enum class without enum entry, and containing some code after the semi then do not report a lint error`() {
        val code =
            """
            enum class Foo {
                ;

                fun foo() {
                    // do something
                }
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 2771 - Given an enum class without enum entry, and some comment before and some code after the semi then do not report a lint error`() {
        val code =
            """
            enum class Foo {
                // NO-OP
                ;

                fun foo() {
                    // do something
                }
            }
            """.trimIndent()
        noSemicolonsRuleAssertThat(code).hasNoLintViolations()
    }
}
