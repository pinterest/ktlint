package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class ChainMethodContinuationTest {
    private val chainMethodContinuationAssertThat = assertThatRule { ChainMethodContinuation() }

    @Test
    fun `Given method chain which is correctly wrapped in single line`() {
        val code =
            """
            val foo = listOf(1, 2, 3).filter { it > 2 }!!.takeIf { it.count() > 100 }.map { it * it }?.sum()!!
            """.trimIndent()
        chainMethodContinuationAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given method chain which is incorrectly wrapped`() {
        val code =
            """
            val foo = listOf(1, 2, 3).
                filter { it > 2 }!!.
                takeIf { it.count() > 100 }.
                map {
                    it * it
                }?.
                map {
                    it * it
                }!!.
                sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)
                .filter { it > 2 }!!
                .takeIf { it.count() > 100 }
                .map {
                    it * it
                }?.map {
                    it * it
                }!!.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, ". must merge at the start of next call"),
                LintViolation(2, 25, ". must merge at the start of next call"),
                LintViolation(3, 33, ". must merge at the start of next call"),
                LintViolation(6, 8, "}?. must merge at the start of next call"),
                LintViolation(9, 9, "}!!. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method with RBRACE is not wrapped with next chain operator`() {
        val code =
            """
            val foo = listOf(1, 2, 3)
                .filter {
                    it > 2
                }
                .map {
                    2 * it
                }
                ?.map {
                    2 * it
                }!!
                .sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)
                .filter {
                    it > 2
                }.map {
                    2 * it
                }?.map {
                    2 * it
                }!!.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 6, ". must must merge at the end of previous call"),
                LintViolation(8, 7, "?. must must merge at the end of previous call"),
                LintViolation(11, 6, ". must must merge at the end of previous call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method chain which is correctly wrapped in next line with previous RBrace`() {
        val code =
            """
            val foo1 = listOf(1, 2, 3).filter { it > 2 }
                .sum()
            val foo2 = listOf(1, 2, 3).filter { it > 2 }!!
                .sum()
            val foo3 = listOf(1, 2, 3).filter { it > 2 }!!
                ?.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given object declaration in chain which is incorrectly wrapped`() {
        val code =
            """
            val foo = object : Runnable {
                override fun run() {
                    /* no-op */
                }
            }.
                run()!!.
                hashCode()?.
                hashCode()?.
                hashCode()
            """.trimIndent()
        val formattedCode =
            """
            val foo = object : Runnable {
                override fun run() {
                    /* no-op */
                }
            }.run()!!
                .hashCode()
                ?.hashCode()
                ?.hashCode()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 3, "}. must merge at the start of next call"),
                LintViolation(6, 13, ". must merge at the start of next call"),
                LintViolation(7, 17, "?. must merge at the start of next call"),
                LintViolation(8, 17, "?. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given nested chains which is incorrectly wrapped`() {
        val code =
            """
            val foo = listOf(1, 2, 3).
                filter {
                    listOf(1, 2, 3).
                        map {
                            it * it
                        }.size > 1
                }!!.
                map {
                    it * it
                }
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)
                .filter {
                    listOf(1, 2, 3)
                        .map {
                            it * it
                        }.size > 1
                }!!.map {
                    it * it
                }
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, ". must merge at the start of next call"),
                LintViolation(3, 25, ". must merge at the start of next call"),
                LintViolation(7, 9, "}!!. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }
}
