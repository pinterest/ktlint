package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
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
    fun `Given method chain which is incorrectly wrapped with dot operator`() {
        val code =
            """
            val foo = listOf(1, 2, 3).
                filter { it > 2 }.
                filter {
                    it > 2
                }.
                sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)
                .filter { it > 2 }
                .filter {
                    it > 2
                }.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, ". must merge at the start of next call"),
                LintViolation(2, 23, ". must merge at the start of next call"),
                LintViolation(5, 7, "}. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method chain which is incorrectly wrapped with dot operator ending with same line chain`() {
        val code =
            """
            val foo = listOf(1, 2, 3).
                filter { it > 2 }.filter { it > 2 }.
                filter {
                    it > 2
                }.
                sum().dec()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)
                .filter { it > 2 }.filter { it > 2 }
                .filter {
                    it > 2
                }.sum().dec()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, ". must merge at the start of next call"),
                LintViolation(2, 41, ". must merge at the start of next call"),
                LintViolation(5, 7, "}. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method chain which is incorrectly wrapped with unsafe dot operator`() {
        val code =
            """
            val foo = listOf(1, 2, 3)!!.
                filter { it > 2 }!!.
                filter {
                    it > 2
                }!!.
                sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)!!
                .filter { it > 2 }!!
                .filter {
                    it > 2
                }!!.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 29, ". must merge at the start of next call"),
                LintViolation(2, 25, ". must merge at the start of next call"),
                LintViolation(5, 9, "}!!. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method chain which is incorrectly wrapped with safe dot operator`() {
        val code =
            """
            val foo = listOf(1, 2, 3)?.
                filter { it > 2 }?.
                filter {
                    it > 2
                }?.
                sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3)
                ?.filter { it > 2 }
                ?.filter {
                    it > 2
                }?.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 28, "?. must merge at the start of next call"),
                LintViolation(2, 24, "?. must merge at the start of next call"),
                LintViolation(5, 8, "}?. must merge at the start of next call"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method chain which is incorrectly wrapped in next line with previous RBrace`() {
        val code =
            """
            val foo1 = listOf(1, 2, 3).filter {
                it > 2
            }
                .sum()

            val foo2 = listOf(1, 2, 3).filter {
                it > 2
            }
            .sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = listOf(1, 2, 3).filter {
                it > 2
            }.sum()

            val foo2 = listOf(1, 2, 3).filter {
                it > 2
            }.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(4, 6, ". must must merge at the end of previous call"),
                LintViolation(9, 2, ". must must merge at the end of previous call"),
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
                run()
            """.trimIndent()
        val formattedCode =
            """
            val foo = object : Runnable {
                override fun run() {
                    /* no-op */
                }
            }.run()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(5, 3, "}. must merge at the start of next call"),
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

    @Nested
    inner class `Given chain with comments` {
        @Test
        fun `Given method chain which is correctly wrapped in single line with comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3)/* 1 */./* 2 */filter { it > 2 }!!/* 1 */.takeIf { it.count() > 100 }/* 1 */?./* 1 */sum()!!
                """.trimIndent()
            chainMethodContinuationAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given method chain which is incorrectly wrapped with dot operator with block comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3)/* 1 */./* 2 */
                    filter {
                        it > 2
                    }/* 3 */./* 4 */
                    sum()/* 5 */
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3)/* 1 *//* 2 */
                    .filter {
                        it > 2
                    }/* 3 */.sum()/* 4 */
                    /* 5 */
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 34, ". must merge at the start of next call"),
                    LintViolation(4, 14, "}/* 3 */. must merge at the start of next call"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped with dot operator with single line comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3).// 1
                    filter {
                        it > 2
                    }.// 2
                    sum()// 3
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3)// 1
                    .filter {
                        it > 2
                    }.sum()// 2
                    // 3
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 27, ". must merge at the start of next call"),
                    LintViolation(4, 7, "}. must merge at the start of next call"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped in next line with previous RBrace with block comments`() {
            val code =
                """
                val foo1 = listOf(1, 2, 3).filter {
                    it > 2
                }/* 1 */
                    /* 2 */./* 3 */sum()/* 4 */
                """.trimIndent()
            val formattedCode =
                """
                val foo1 = listOf(1, 2, 3).filter {
                    it > 2
                }./* 3 */sum()/* 1 */
                    /* 2 *//* 4 */
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 13, ". must must merge at the end of previous call"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped in next line with previous RBrace with block comments and ends with chain`() {
            val code =
                """
                val foo1 = listOf(1, 2, 3).filter {
                    it > 2
                }/* 1 */
                    /* 2 */./* 3 */sum().dec()/* 4 */
                """.trimIndent()
            val formattedCode =
                """
                val foo1 = listOf(1, 2, 3).filter {
                    it > 2
                }./* 3 */sum().dec()/* 1 */
                    /* 2 *//* 4 */
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 13, ". must must merge at the end of previous call"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped in next line with previous RBrace with single line comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3).filter {
                    it > 2
                }// 1
                    .sum()// 2
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3).filter {
                    it > 2
                }.sum()// 1// 2
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 6, ". must must merge at the end of previous call"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped in next line with previous RBrace with single line comments and ends with chain`() {
            val code =
                """
                val foo = listOf(1, 2, 3).filter {
                    it > 2
                }// 1
                    .sum().dec()// 2
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3).filter {
                    it > 2
                }.sum().dec()// 1// 2
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 6, ". must must merge at the end of previous call"),
                ).isFormattedAs(formattedCode)
        }
    }
}
