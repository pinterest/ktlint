package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.EOL_CHAR
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChainMethodContinuationTest {
    private val chainMethodContinuationAssertThat =
        assertThatRule(
            provider = { ChainMethodContinuation() },
            additionalRuleProviders = setOf(RuleProvider { DiscouragedCommentLocationRule() })
        )

    @Test
    fun `Given that no maximum line length is set, and a single line method chain then do not wrap`() {
        val code =
            """
            val foo = listOf(1, 2, 3).filter { it > 2 }!!.takeIf { it.count() > 100 }.map { it * it }?.sum()!!
            """.trimIndent()
        chainMethodContinuationAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given that maximum line length is set, and a single line method chain does not exceed the maximum line length then do not wrap`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                                                       $EOL_CHAR
            val foo = listOf(1, 2, 3).filter { it > 2 }!!.takeIf { it.count() > 100 }.map { it * it }?.sum()!!
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given that maximum line length is set, and a single line method chain does exceed the maximum line length then wrap`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                                                                      $EOL_CHAR
            val foo = listOf(1, 2, 3).filter { it > 2 }!!.takeIf { it.count() > 100 }.map { it * it }?.sum()!!
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                                                                      $EOL_CHAR
            val foo = listOf(1, 2, 3)
                .filter { it > 2 }!!
                .takeIf { it.count() > 100 }
                .map { it * it }
                ?.sum()!!
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 26, "Expected newline before '.'"),
                LintViolation(2, 46, "Expected newline before '.'"),
                // During formatting this violation will not occur due to wrapping of previous chain operators
                LintViolation(2, 58, "Expected newline before '.'"),
                LintViolation(2, 74, "Expected newline before '.'"),
                // During formatting this violation will not occur due to wrapping of previous chain operators
                LintViolation(2, 90, "Expected newline before '?.'"),
            ).isFormattedAs(formattedCode)
    }

    @Disabled(
        "Given an outer chained method which is written a single line. Given that it exceeds the maximum line length, then the " +
            "individual chains are to be wrapped. This works ok. But if an inner chain (by definition a single line) still " +
            "exceeds the maximum line length the wrapping of this inner chain conflicts with the function-literal rule."
    )
    @Test
    fun `Given that maximum line length is set, and a single line method chain does exceed the maximum line length then wrap then also wrap inner method chains if needed`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foo = listOf(1, 2, 3).filter { it > 2 }!!.takeIf { it.count() > 100 }.map { it * it }?.sum()!!
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER    $EOL_CHAR
            val foo = listOf(1, 2, 3)
                .filter { it > 2 }!!
                .takeIf {
                    it.count() > 100
                }.map { it * it }
                ?.sum()!!
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .setMaxLineLength()
            // TODO: Fix misbehavior in combination with FunctionLiteralRule due to prioritisation of wrapping function literal versus
            //  chained methods
            .addAdditionalRuleProvider { FunctionLiteralRule() }
            //.addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 26, "Expected newline before '.'"),
                LintViolation(2, 46, "Expected newline before '.'"),
                // During formatting this violation will not occur due to wrapping of previous chain operators
                LintViolation(2, 58, "Expected newline before '.'"),
                LintViolation(2, 74, "Expected newline before '.'"),
                // During formatting this violation will not occur due to wrapping of previous chain operators
                LintViolation(2, 90, "Expected newline before '?.'"),
            ).isFormattedAs(formattedCode)
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
    fun `Given a method chain which wrapped after the dot operator instead of before the dot operator`() {
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
                LintViolation(1, 26, "Expected newline before '.'"),
                LintViolation(1, 26, "Unexpected newline after '.'"),
                LintViolation(2, 22, "Expected newline before '.'"),
                LintViolation(2, 22, "Unexpected newline after '.'"),
                LintViolation(5, 6, "Unexpected newline after '.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a method chain on the same line as the previous method chain`() {
        val code =
            """
            val foo = listOf(1, 2, 3, 4).
                filter { it > 2 }.filter { it > 3 }.
                filter {
                    it > 4
                }.
                sum().dec()
            """.trimIndent()
        val formattedCode =
            """
            val foo = listOf(1, 2, 3, 4)
                .filter { it > 2 }
                .filter { it > 3 }
                .filter {
                    it > 4
                }.sum()
                .dec()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 29, "Expected newline before '.'"),
                LintViolation(1, 29, "Unexpected newline after '.'"),
                LintViolation(2, 22, "Expected newline before '.'"),
                LintViolation(2, 40, "Expected newline before '.'"),
                LintViolation(2, 40, "Unexpected newline after '.'"),
                LintViolation(5, 6, "Unexpected newline after '.'"),
                LintViolation(6, 10, "Expected newline before '.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given next method chain which is incorrectly wrapped with !! operator`() {
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
                }!!
                .sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 28, "Expected newline before '.'"),
                LintViolation(1, 28, "Unexpected newline after '.'"),
                LintViolation(2, 24, "Expected newline before '.'"),
                LintViolation(2, 24, "Unexpected newline after '.'"),
                LintViolation(5, 8, "Expected newline before '.'"),
                LintViolation(5, 8, "Unexpected newline after '.'"),
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
                LintViolation(1, 26, "Expected newline before '?.'"),
                LintViolation(1, 27, "Unexpected newline after '?.'"),
                LintViolation(2, 22, "Expected newline before '?.'"),
                LintViolation(2, 23, "Unexpected newline after '?.'"),
                LintViolation(5, 7, "Unexpected newline after '?.'"),
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
            val foo1 = listOf(1, 2, 3)
                .filter {
                    it > 2
                }.sum()

            val foo2 = listOf(1, 2, 3)
                .filter {
                    it > 2
                }.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(1, 27, "Expected newline before '.'"),
                LintViolation(4, 5, "Unexpected newline before '.'"),
                LintViolation(6, 27, "Expected newline before '.'"),
                LintViolation(9, 1, "Unexpected newline before '.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline method chain which is correctly wrapped`() {
        val code =
            """
            val foo1 = listOf(1, 2, 3)
                .filter { it > 2 }
                .sum()
            val foo2 = listOf(1, 2, 3)
                .filter { it > 2 }!!
                .sum()
            val foo3 = listOf(1, 2, 3)
            .filter { it > 2 }!!
                ?.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a multiline method chain which is not correctly wrapped`() {
        val code =
            """
            val foo1 = listOf(1, 2, 3).filter { it > 2 }
                .sum()
            val foo2 = listOf(1, 2, 3).filter { it > 2 }!!
                .sum()
            val foo3 = listOf(1, 2, 3).filter { it > 2 }!!
                ?.sum()
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = listOf(1, 2, 3)
                .filter { it > 2 }
                .sum()
            val foo2 = listOf(1, 2, 3)
                .filter { it > 2 }!!
                .sum()
            val foo3 = listOf(1, 2, 3)
                .filter { it > 2 }!!
                ?.sum()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, "Expected newline before '.'"),
                LintViolation(3, 27, "Expected newline before '.'"),
                LintViolation(5, 27, "Expected newline before '.'"),
            ).isFormattedAs(formattedCode)
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
            .hasLintViolation(5, 2, "Unexpected newline after '.'")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given nested chains which are correctly wrapped`() {
        val code =
            """
            val foo = listOf(1, 2, 3)
                .filter {
                    listOf(1, 2, 3).map { it * it }.size > 1
                }!!
                .map {
                    it * it
                }
            """.trimIndent()
        chainMethodContinuationAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given nested chains which are incorrectly wrapped`() {
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
                }!!
                .map {
                    it * it
                }
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 26, "Expected newline before '.'"),
                LintViolation(1, 26, "Unexpected newline after '.'"),
                LintViolation(3, 24, "Expected newline before '.'"),
                LintViolation(3, 24, "Unexpected newline after '.'"),
                LintViolation(7, 8, "Expected newline before '.'"),
                LintViolation(7, 8, "Unexpected newline after '.'"),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline method chain then wrap each method chain to a separate line`() {
        val code =
            """
            val foo1 = listOf(1, 2, 3)
                .bar().foo()?.fooBar()
            val foo2 = listOf(1, 2, 3).bar()
                .foo()?.fooBar()
            val foo3 = listOf(1, 2, 3).bar().foo()
                ?.fooBar()
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = listOf(1, 2, 3)
                .bar()
                .foo()
                ?.fooBar()
            val foo2 = listOf(1, 2, 3)
                .bar()
                .foo()
                ?.fooBar()
            val foo3 = listOf(1, 2, 3)
                .bar()
                .foo()
                ?.fooBar()
            """.trimIndent()
        chainMethodContinuationAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 11, "Expected newline before '.'"),
                LintViolation(2, 17, "Expected newline before '?.'"),
                LintViolation(3, 27, "Expected newline before '.'"),
                LintViolation(4, 11, "Expected newline before '?.'"),
                LintViolation(5, 27, "Expected newline before '.'"),
                LintViolation(5, 33, "Expected newline before '.'"),
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class `Given chain with comments` {
        @Test
        fun `Comments between a chain operator and the next chained method are disallowed`() {
            val code =
                """
                val foo1 = listOf(1, 2, 3)
                    ./** some comment */size
                val foo2 = listOf(1, 2, 3)
                    ./** some comment */single()
                val foo3 = listOf(1, 2, 3)
                    ./** some comment */filter { it > 2 }
                val foo4 = listOf(1, 2, 3)
                    ./* some comment */size
                val foo5 = listOf(1, 2, 3)
                    ./* some comment */single()
                val foo6 = listOf(1, 2, 3)
                    ./* some comment */filter { it > 2 }
                val foo7 = listOf(1, 2, 3)
                    .// some comment
                    size
                val foo8 = listOf(1, 2, 3)
                    .// some comment
                    single()
                val foo9 = listOf(1, 2, 3)
                    .// some comment
                    filter { it > 2 }
                """.trimIndent()
            assertThatRule { DiscouragedCommentLocationRule() }(code)
                .hasLintViolations(
                    LintViolation(2, 6, "No comment expected at this location", false),
                    LintViolation(4, 6, "No comment expected at this location", false),
                    LintViolation(6, 6, "No comment expected at this location", false),
                    LintViolation(8, 6, "No comment expected at this location", false),
                    LintViolation(10, 6, "No comment expected at this location", false),
                    LintViolation(12, 6, "No comment expected at this location", false),
                    LintViolation(14, 6, "No comment expected at this location", false),
                    LintViolation(17, 6, "No comment expected at this location", false),
                    LintViolation(20, 6, "No comment expected at this location", false),
                ).hasNoLintViolationsForRuleId(CHAIN_WRAPPING_RULE_ID)
        }

        @Test
        fun `Given a single line method chain with comments before the chain operator`() {
            val code =
                """
                val foo = listOf(1, 2, 3) /* 1 */ .filter { it > 2 }!! /* 2 */ .takeIf { it.count() > 100 } /* 3 */?.sum()!!
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3) /* 1 */
                    .filter { it > 2 }!! /* 2 */
                    .takeIf { it.count() > 100 } /* 3 */
                    ?.sum()!!
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 35, "Expected newline before '.'"),
                    LintViolation(1, 64, "Expected newline before '.'"),
                    LintViolation(1, 100, "Expected newline before '?.'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a multiline method chain with comments at end of previous line and chain operator on the next line`() {
            val code =
                """
                val foo1 = listOf(1, 2, 3) /* 1 */
                    .filter { it > 2 }!! /* 2 */
                    .takeIf { it.count() > 100 } /* 3 */
                    ?.sum()!!
                val foo2 = listOf(1, 2, 3) // 1
                    .filter { it > 2 }!! // 2
                    .takeIf { it.count() > 100 } // 3
                    ?.sum()!!
                """.trimIndent()
            chainMethodContinuationAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `Given method chain which is incorrectly wrapped with dot operator with block comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3)/* 1 */.filter {
                        it > 2
                    }/* 2 */.sum()
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3)/* 1 */
                    .filter {
                        it > 2
                    }/* 2 */
                    .sum()
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .hasLintViolations(
                    LintViolation(1, 33, "Expected newline before '.'"),
                    LintViolation(3, 13, "Expected newline before '.'"),
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given method chain which is incorrectly wrapped in next line with previous RBrace with block comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3).filter {
                    it > 2
                } // 1
                  /* 2 */.sum()
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3)
                    .filter {
                        it > 2
                    } // 1
                    /* 2 */
                    .sum()
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .hasLintViolations(
                    LintViolation(1, 26, "Expected newline before '.'"),
                    LintViolation(4, 10, "Expected newline before '.'"),
                ).isFormattedAs(formattedCode)
        }


        @Test
        fun `Given method chain which is incorrectly wrapped in next line with previous RBrace with single line comments`() {
            val code =
                """
                val foo = listOf(1, 2, 3).filter {
                    it > 2
                }// 1
                    .sum()
                """.trimIndent()
            val formattedCode =
                """
                val foo = listOf(1, 2, 3)
                    .filter {
                        it > 2
                    }// 1
                    .sum()
                """.trimIndent()
            chainMethodContinuationAssertThat(code)
                .addAdditionalRuleProvider { IndentationRule() }
                .hasLintViolation(1, 26, "Expected newline before '.'")
                .isFormattedAs(formattedCode)
        }
    }
}
