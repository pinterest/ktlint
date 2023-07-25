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
    fun `Given method chain which is correctly wrapped in multi lines`() {
        val code =
            """
            val foo = listOf(1, 2, 3)
                .filter { it > 2 }!!
                .takeIf { it.count() > 100 }
                .map {
                    it * it
                }?.sum()!!
            """.trimIndent()
        chainMethodContinuationAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given next method chain which is incorrectly wrapped with dot operator`() {
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
                LintViolation(1, 27, "Expected newline before '.'"),
                LintViolation(2, 23, "Expected newline before '.'"),
                LintViolation(5, 7, "Unexpected newline after '}.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given next same line method chains which is incorrectly wrapped with dot operator`() {
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
                LintViolation(1, 27, "Expected newline before '.'"),
                LintViolation(2, 41, "Expected newline before '.'"),
                LintViolation(5, 7, "Unexpected newline after '}.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given next method chain which is incorrectly wrapped with unsafe dot operator`() {
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
                LintViolation(1, 29, "Expected newline before '.'"),
                LintViolation(2, 25, "Expected newline before '.'"),
                LintViolation(5, 9, "Unexpected newline after '}!!.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given next method chain which is incorrectly wrapped with safe dot operator`() {
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
                LintViolation(1, 28, "Expected newline before '?.'"),
                LintViolation(2, 24, "Expected newline before '?.'"),
                LintViolation(5, 8, "Unexpected newline after '}?.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given method chain which is incorrectly wrapped with preceding brace`() {
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
                LintViolation(4, 6, "Unexpected newline before '.'"),
                LintViolation(9, 2, "Unexpected newline before '.'"),
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
    fun `Given next chain which is incorrectly wrapped chain operator in object declaration`() {
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
                LintViolation(5, 3, "Unexpected newline after '}.'"),
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
                LintViolation(1, 27, "Expected newline before '.'"),
                LintViolation(3, 25, "Expected newline before '.'"),
                LintViolation(7, 9, "Unexpected newline after '}!!.'"),
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
        fun `Given multi line method chain with proper in between comments `() {
            val code =
                """
                val foo1 =
                   listOf(1, 2, 3).filter { it > 2 }
                       /*
                        * some comment
                        */
                       .sum().dec()

                       // some comment
                       .dec()
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasNoLintViolations()
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
                val foo = listOf(1, 2, 3)/* 1 */
                    ./* 2 */filter {
                        it > 2
                    }/* 3 */./* 4 */sum()/* 5 */
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 34, "Expected newline before '.'"),
                    LintViolation(4, 14, "Unexpected newline after '}/* 3 */./* 4 */'"),
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
                }/* 1 *//* 2 */./* 3 */sum()/* 4 */
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 13, "Unexpected newline before '/* 2 */.'"),
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

                val foo2 = listOf(1, 2, 3).filter {
                    it > 2
                }
                    /* 1 *//* 2 */.sum()
                """.trimIndent()
            val formattedCode =
                """
                val foo1 = listOf(1, 2, 3).filter {
                    it > 2
                }/* 1 *//* 2 */./* 3 */sum().dec()/* 4 */

                val foo2 = listOf(1, 2, 3).filter {
                    it > 2
                }/* 1 *//* 2 */.sum()
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(4, 13, "Unexpected newline before '/* 2 */.'"),
                    LintViolation(9, 20, "Unexpected newline before '/* 1 *//* 2 */.'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped with single line comments after chain`() {
            val code =
                """
                val foo1 = listOf(1, 2, 3).
                    sum()// 1

                val foo2 = listOf(1, 2, 3).
                    sum().dec()// 1

                val foo3 = listOf(1, 2, 3)
                    .filter {
                        it > 2
                    }
                    .sum()// 1
                """.trimIndent()
            val formattedCode =
                """
                val foo1 = listOf(1, 2, 3)
                    .sum()// 1

                val foo2 = listOf(1, 2, 3)
                    .sum().dec()// 1

                val foo3 = listOf(1, 2, 3)
                    .filter {
                        it > 2
                    }.sum()// 1
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 28, "Expected newline before '.'"),
                    LintViolation(4, 28, "Expected newline before '.'"),
                    LintViolation(11, 6, "Unexpected newline before '.'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped with dot operator with single line comments after chain operator`() {
            val code =
                """
                val foo = listOf(1, 2, 3).// 1
                    filter {
                        it > 2
                    }.// 2
                    sum()
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(1, 27, "Expected newline before '.'"),
                    LintViolation(4, 7, "Unexpected newline after '}.// 2'"),
                )
        }

        @Test
        fun `Given method chain which is incorrectly wrapped with dot operator with single line comments and last line end with chain`() {
            val code =
                """
                val foo = listOf(1, 2, 3)
                    .filter {
                        it > 2
                    }.// 2
                    sum().dec()// 3
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(4, 7, "Unexpected newline after '}.// 2'"),
                )
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
            chainMethodContinuationAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(4, 6, "Unexpected newline before '.'"),
                )
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
            chainMethodContinuationAssertThat(code)
                .hasLintViolationsWithoutAutoCorrect(
                    LintViolation(4, 6, "Unexpected newline before '.'"),
                )
        }
    }
}
