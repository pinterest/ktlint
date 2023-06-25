package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.test.KtLintAssertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BlankLineBeforeDeclarationRuleTest {
    private val blankLineBeforeDeclarationRuleAssertThat =
        KtLintAssertThat.assertThatRule(
            provider = { BlankLineBeforeDeclarationRule() },
            editorConfigProperties = setOf(CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official),
        )

    @Test
    fun `Given some consecutive classes not separated by a blank line then insert a blank line in between`() {
        val code =
            """
            class Foo
            class Bar
            """.trimIndent()
        val formattedCode =
            """
            class Foo

            class Bar
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive classes separated with an annotation before the second class then insert a blank line before the annotation`() {
        val code =
            """
            class Foo
            @FooBar
            class Bar
            """.trimIndent()
        val formattedCode =
            """
            class Foo

            @FooBar
            class Bar
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive classes separated with a kdoc before the second class then insert a blank line before the kdoc`() {
        val code =
            """
            class Foo
            /**
             * Some KDOC
             */
            class Bar
            """.trimIndent()
        val formattedCode =
            """
            class Foo

            /**
             * Some KDOC
             */
            class Bar
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive classes separated with a block comment before the second class then insert a blank line before the kdoc`() {
        val code =
            """
            class Foo
            /*
             * Some comment
             */
            class Bar
            """.trimIndent()
        val formattedCode =
            """
            class Foo

            /*
             * Some comment
             */
            class Bar
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive classes separated with EOL comments before the second class then insert a blank line before the EOL-comments`() {
        val code =
            """
            class Foo
            // Some comment 1
            // Some comment 2
            class Bar
            """.trimIndent()
        val formattedCode =
            """
            class Foo

            // Some comment 1
            // Some comment 2
            class Bar
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive functions not separated by a blank line then insert a blank line in between`() {
        val code =
            """
            fun foo() {}
            fun bar() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {}

            fun bar() {}
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive functions separated with an annotation before the second function then insert a blank line before the annotation`() {
        val code =
            """
            fun foo() {}
            @FooBar
            fun bar() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {}

            @FooBar
            fun bar() {}
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive functions separated with a kdoc before the second function then insert a blank line before the annotation`() {
        val code =
            """
            fun foo() {}
            /**
             * Some KDOC
             */
            fun bar() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {}

            /**
             * Some KDOC
             */
            fun bar() {}
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive functions separated with a block comment before the second function then insert a blank line before the annotation`() {
        val code =
            """
            fun foo() {}
            /*
             * Some KDOC
             */
            fun bar() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {}

            /*
             * Some KDOC
             */
            fun bar() {}
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some consecutive functions separated with EOL comments before the second function then insert a blank line before the annotation`() {
        val code =
            """
            fun foo() {}
            // Some comment 1
            // Some comment 2
            fun bar() {}
            """.trimIndent()
        val formattedCode =
            """
            fun foo() {}

            // Some comment 1
            // Some comment 2
            fun bar() {}
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a kotlin script with some consecutive functions separated with EOL comments before the second function then insert a blank line before the annotation`() {
        val code =
            """
            tasks.withType<KotlinCompile>().configureEach {}

            // Some comment 1
            // Some comment 2
            tasks.withType<JavaCompile>().configureEach {}
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .asKotlinScript()
            .hasNoLintViolations()
    }

    @Nested
    inner class `Given some nested declaration` {
        @Test
        fun `Given a class as first code sibling inside a class body then do not insert a blank line between the class signature and this function`() {
            val code =
                """
                class Foo {
                    class Bar
                }
                """.trimIndent()
            blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a function as first code sibling inside a class body then do not insert a blank line between the class signature and this function`() {
            val code =
                """
                class Foo {
                    fun bar() {}
                }
                """.trimIndent()
            blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a class as first code sibling inside a function body then do not insert a blank line between the class signature and this function`() {
            val code =
                """
                fun foo() {
                    class Bar
                }
                """.trimIndent()
            blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a function as first code sibling inside a function body then do not insert a blank line between the class signature and this function`() {
            val code =
                """
                fun foo() {
                    fun bar() {}
                }
                """.trimIndent()
            blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a function as first code sibling inside a function literal without parameters then do not insert a blank line the declaration`() {
            val code =
                """
                val foo = {
                    fun bar() {}

                    bar()
                }
                """.trimIndent()
            blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given a function as first code sibling inside a function literal with parameters then do not insert a blank line the declaration`() {
            val code =
                """
                val foo = { _ ->
                    fun bar() {}

                    bar()
                }
                """.trimIndent()
            blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
        }
    }

    @Test
    fun `Given some consecutive properties not separated by a blank line then do not insert a blank line in between`() {
        val code =
            """
            val foo1 = "foo1"
            val foo2: String
                get() = "foo2"
            var foo3: String = "foo3"
                set(value) {
                    field = value.repeat(2)
                }
            var foo4 = "foo4"
            var foo5: String by Delegate()
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function containing a property declaration after a statement then do not insert a blank line before the declaration`() {
        val code =
            """
            fun foo() {
                bar()
                val bar = "bar"
            }
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given some consecutive declarations`() {
        val code =
            """
            const val foo1 = "foo1"
            class FooBar {
                val foo2 = "foo2"
                val foo3 = "foo3"
                fun bar1() {
                   val foo4 = "foo4"
                   val foo5 = "foo5"
                }
                fun bar2() = "bar"
                val foo6 = "foo3"
                val foo7 = "foo4"
                enum class Foo {}
            }
            """.trimIndent()
        val formattedCode =
            """
            const val foo1 = "foo1"

            class FooBar {
                val foo2 = "foo2"
                val foo3 = "foo3"

                fun bar1() {
                   val foo4 = "foo4"
                   val foo5 = "foo5"
                }

                fun bar2() = "bar"

                val foo6 = "foo3"
                val foo7 = "foo4"

                enum class Foo {}
            }
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "Expected a blank line for this declaration"),
                LintViolation(5, 5, "Expected a blank line for this declaration"),
                LintViolation(9, 5, "Expected a blank line for this declaration"),
                LintViolation(10, 5, "Expected a blank line for this declaration"),
                LintViolation(12, 5, "Expected a blank line for this declaration"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class followed by EOL comment followed by a class`() {
        val code =
            """
            class Foo
            // Some comment

            class Bar
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a when statement with a property declaration`() {
        val code =
            """
            val foo =
                when (val foobar = FooBar()) {
                    is Bar -> foobar.bar()
                    is Foo -> foobar.foo()
                    else -> foobar
                }
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class with a class initializer after another declaration`() {
        val code =
            """
            class Foo {
                val foo = "foo"
                init {
                    // do something
                }
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo {
                val foo = "foo"

                init {
                    // do something
                }
            }
            """.trimIndent()
        blankLineBeforeDeclarationRuleAssertThat(code)
            .hasLintViolation(3, 5, "Expected a blank line for this declaration")
            .isFormattedAs(formattedCode)
    }
}
