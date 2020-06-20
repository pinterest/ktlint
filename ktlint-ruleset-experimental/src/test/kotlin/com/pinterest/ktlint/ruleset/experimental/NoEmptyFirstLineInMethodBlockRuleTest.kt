package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoEmptyFirstLineInMethodBlockRuleTest {

    @Test
    fun testFormatIsCorrect() {
        val formattedFunction =
            """
            fun bar() {
               val a = 2
            }
            """.trimIndent()

        assertThat(NoEmptyFirstLineInMethodBlockRule().lint(formattedFunction)).isEmpty()
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(formattedFunction)).isEqualTo(formattedFunction)
    }

    @Test
    fun testFormatWhenFirstLineIsEmptyInMethod() {
        val unformattedFunction =
            """
            fun bar() {

               val a = 2
            }
            """.trimIndent()
        val formattedFunction =
            """
            fun bar() {
               val a = 2
            }
            """.trimIndent()

        assertThat(NoEmptyFirstLineInMethodBlockRule().lint(unformattedFunction)).isEqualTo(
            listOf(
                LintError(2, 1, "no-empty-first-line-in-method-block", "First line in a method block should not be empty")
            )
        )
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(unformattedFunction)).isEqualTo(formattedFunction)
    }

    @Test
    fun testFormatWhenFirstLineIsEmptyInABlockWithinMethod() {
        val unformattedFunction =
            """
            fun funA() {

                if (conditionA()) {

                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        val formattedFunction =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()

        assertThat(NoEmptyFirstLineInMethodBlockRule().lint(unformattedFunction)).isEqualTo(
            listOf(
                LintError(2, 1, "no-empty-first-line-in-method-block", "First line in a method block should not be empty"),
                LintError(4, 1, "no-empty-first-line-in-method-block", "First line in a method block should not be empty")
            )
        )
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(unformattedFunction)).isEqualTo(formattedFunction)
    }

    @Test
    fun testFormatWhenFirstLineIsEmptyOnlyInABlockWithinMethod() {
        val unformattedFunction =
            """
            fun funA() {
                if (conditionA()) {

                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()
        val formattedFunction =
            """
            fun funA() {
                if (conditionA()) {
                    doSomething()
                } else if (conditionB()) {
                    doAnotherThing()
                }
            }
            """.trimIndent()

        assertThat(NoEmptyFirstLineInMethodBlockRule().lint(unformattedFunction)).isEqualTo(
            listOf(
                LintError(3, 1, "no-empty-first-line-in-method-block", "First line in a method block should not be empty")
            )
        )
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(unformattedFunction)).isEqualTo(formattedFunction)
    }

    @Test
    fun testFormatWhenFirstLineIsEmptyInFunctionButIgnoreAtClassLevel() {
        val unformattedFunction =
            """
            class A {

                fun bar() {

                   val a = 2
                }
            }
            """.trimIndent()
        val formattedFunction =
            """
            class A {

                fun bar() {
                   val a = 2
                }
            }
            """.trimIndent()

        assertThat(NoEmptyFirstLineInMethodBlockRule().lint(unformattedFunction)).isEqualTo(
            listOf(
                LintError(4, 1, "no-empty-first-line-in-method-block", "First line in a method block should not be empty")
            )
        )
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(unformattedFunction)).isEqualTo(formattedFunction)
    }

    @Test
    fun `lint empty first line may be placed in function inside anonymous object`() {
        val code =
            """
            fun fooBuilder() = object : Foo {
            
                override fun foo() {
                    TODO()
                }
            }
            """.trimIndent()
        assertThat(NoEmptyFirstLineInMethodBlockRule().lint(code)).isEmpty()
    }

    @Test
    fun `format empty first line may be placed in function inside anonymous object`() {
        val code =
            """
            fun fooBuilder() = object : Foo {
            
                override fun foo() {
                    TODO()
                }
            }
            """.trimIndent()
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(code)).isEqualTo(code)
    }
}
