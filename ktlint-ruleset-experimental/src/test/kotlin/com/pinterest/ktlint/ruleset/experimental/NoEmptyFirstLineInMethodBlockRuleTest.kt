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
                LintError(2, 1, "no-first-line-blank-in-method-block-rule", "First line in a method block should not be empty")
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
                LintError(2, 1, "no-first-line-blank-in-method-block-rule", "First line in a method block should not be empty"),
                LintError(4, 1, "no-first-line-blank-in-method-block-rule", "First line in a method block should not be empty")
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
                LintError(3, 1, "no-first-line-blank-in-method-block-rule", "First line in a method block should not be empty")
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
                LintError(4, 1, "no-first-line-blank-in-method-block-rule", "First line in a method block should not be empty")
            )
        )
        assertThat(NoEmptyFirstLineInMethodBlockRule().format(unformattedFunction)).isEqualTo(formattedFunction)
    }
}
