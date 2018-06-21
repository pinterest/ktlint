package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class IndentationRuleTest {

    @Test
    fun testLint() {
        assertThat(IndentationRule().lint(
            """
            /**
             * _
             */
            fun main() {
                val a = 0
                // this is not detected because actual indent 8 % 4 = 0
                    val b = 0
                if (a == 0) {
                    println(a)
                }
                val b = builder().setX().setY()
                    .build()
               val c = builder("long_string" +
                     "")
            }

            class A {
                var x: String
                    get() = ""
                    set(v: String) { x = v }
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(13, 1, "indent", "Unexpected indentation (3) (it should be 4)"),
            // fixme: expected indent should not depend on the "previous" line value
            LintError(14, 1, "indent", "Unexpected indentation (9) (it should be 7)")
        ))
    }

    @Test
    fun testLintFormat() {
        assertThat(IndentationRule().format(
            """
            /**
             * _
             */
            fun main() {
                val a = 0
                    val b = 0
                if (a == 0) {
                    println(a)
                }
                val b = builder().setX().setY()
                    .build()
               val c = builder("long_string" +
                     "")
            }

            class A {
                var x: String
                    get() = ""
                    set(v: String) { x = v }
            }
            """.trimIndent()
        )).isEqualTo("""
            /**
             * _
             */
            fun main() {
                val a = 0
                    val b = 0
                if (a == 0) {
                    println(a)
                }
                val b = builder().setX().setY()
                    .build()
                val c = builder("long_string" +
                    "")
            }

            class A {
                var x: String
                    get() = ""
                    set(v: String) { x = v }
            }
        """.trimIndent())
    }

    @Test
    fun testLintCustomIndentSize() {
        assertThat(IndentationRule().lint(
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent(),
            mapOf("indent_size" to "3")
        )).isEqualTo(listOf(
            LintError(3, 1, "indent", "Unexpected indentation (4) (it should be 3)")
        ))
    }

    @Test
    fun testLintCustomIndentSizeFormat() {
        assertThat(IndentationRule().format(
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent(),
            mapOf("indent_size" to "3")
        )).isEqualTo("""
            fun main() {
               val v = ""
               println(v)
            }
        """.trimIndent()
        )
    }

    @Test
    fun testLintCustomIndentSizeValid() {
        assertThat(IndentationRule().lint(
            """
            /**
             * _
             */
            fun main() {
              val v = ""
              println(v)
            }

            class A {
              var x: String
                get() = ""
                set(v: String) { x = v }
            }
            """.trimIndent(),
            mapOf("indent_size" to "2")
        )).isEmpty()
    }

    @Test
    fun testLintIndentSizeUnset() {
        assertThat(IndentationRule().lint(
            """
            fun main() {
               val v = ""
                println(v)
            }
            """.trimIndent(),
            mapOf("indent_size" to "unset")
        )).isEmpty()
    }

    @Test
    fun testLintWithContinuationIndentSizeSet() {
        // gcd(indent_size, continuation_indent_size) == 2
        assertThat(IndentationRule().lint(
            """
            fun main() {
                val v = ""
                      .call()
                 call()
            }
            """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "6")
        )).isEqualTo(listOf(
            LintError(4, 1, "indent", "Unexpected indentation (5) (it should be 2)")
        ))
        assertThat(IndentationRule().lint(
            """
            fun main() {
                val v = ""
                      .call()
                 call()
            }
            """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "2")
        )).isEqualTo(listOf(
            LintError(4, 1, "indent", "Unexpected indentation (5) (it should be 2)")
        ))
        // gcd(indent_size, continuation_indent_size) == 1 equals no indent check
        assertThat(IndentationRule().lint(
            """
            fun main() {
                val v = ""
                    .call()
                     .call()
                      .call()
            }
            """.trimIndent(),
            mapOf("indent_size" to "4", "continuation_indent_size" to "3")
        )).isEmpty()
    }

    // https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting
    @Test
    fun testLintMultilineFunctionCall() {
        assertThat(IndentationRule().lint(
            """
            fun main() {
                fn(
                   a,
                   b,
                   c)
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(3, 1, "indent", "Unexpected indentation (7) (it should be 8)"),
            LintError(4, 1, "indent", "Unexpected indentation (7) (it should be 8)"),
            LintError(5, 1, "indent", "Unexpected indentation (7) (it should be 8)")
        ))
    }

    // https://kotlinlang.org/docs/reference/coding-conventions.html#method-call-formatting
    @Test
    fun testLintMultilineFunctionCallFormat() {
        assertThat(IndentationRule().format(
            """
            fun main() {
                fn(
                   a,
                   b,
                   c)
            }
            """.trimIndent()
        )).isEqualTo("""
            fun main() {
                fn(
                    a,
                    b,
                    c)
            }
        """.trimIndent())
    }

    @Test
    fun testLintCommentsAreIgnored() {
        assertThat(IndentationRule().lint(
            """
            fun funA(argA: String) =
                // comment
            // comment
                call(argA)
            fun main() {
                addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
             // comment
                    override fun onLayoutChange(
                    )
                })
            }
            """.trimIndent(),
            mapOf("indent_size" to "4")
        )).isEqualTo(listOf(
            LintError(7, 1, "indent", "Unexpected indentation (1) (it should be 8)")
        ))
    }

    @Test
    fun testLintCommentsAreIgnoredFormat() {
        assertThat(IndentationRule().format(
            """
            fun funA(argA: String) =
                // comment
            // comment
                call(argA)
            fun main() {
                addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
             // comment
                    override fun onLayoutChange(
                    )
                })
            }
            """.trimIndent(),
            mapOf("indent_size" to "4")
        )).isEqualTo("""
            fun funA(argA: String) =
                // comment
            // comment
                call(argA)
            fun main() {
                addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    // comment
                    override fun onLayoutChange(
                    )
                })
            }
        """.trimIndent())
    }

    @Test(description = "https://github.com/shyiko/ktlint/issues/180")
    fun testLintWhereClause() {
        assertThat(IndentationRule().lint(
            """
            class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
                val adapter1: A1,
                val adapter2: A2
            ) : RecyclerView.Adapter<C>()
                where A1 : RecyclerView.Adapter<V1>, A1 : ComposableAdapter.ViewTypeProvider,
                      A2 : RecyclerView.Adapter<V2>, A2 : ComposableAdapter.ViewTypeProvider {
            }
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testFormatCurlyBraceInWhereBlock() {
        assertThat(IndentationRule().format(
            """
            class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
                val adapter1: A1
            ) : RecyclerView.Adapter<C>()
                where A1 : Type1,
                      A2 : Type2 {
             }
            """.trimIndent()
        )).isEqualTo(
            """
            class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
                val adapter1: A1
            ) : RecyclerView.Adapter<C>()
                where A1 : Type1,
                      A2 : Type2 {
            }
                """.trimIndent()
        )
    }

    @Test
    fun testCurlyBrace() {
        assertThat(IndentationRule().lint(
            """
            class ReaderFacade {
                fun run() {
                 }
            }
              """.trimIndent()
        )).isEqualTo(
            listOf(
                LintError(3, 1, "indent", "Unexpected indentation (5) (it should be 4)")
            )
        )
    }

    @Test
    fun testCurlyBraceFormat() {
        assertThat(IndentationRule().format(
            """
                class ReaderFacade {
                    fun run() {
                     }
                }
              """.trimIndent()
        )).isEqualTo(
            """
                class ReaderFacade {
                    fun run() {
                    }
                }
                """.trimIndent()
        )
    }

    @Test
    fun testCurlyBraceInNestedBlockWithComment() {
        assertThat(IndentationRule().lint(
            """
            object KtLint {
                // comment
                class LoggerFactory {
                }
            }
              """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testCurlyBraceWithCompanionObjectFormat() {
        assertThat(IndentationRule().format(
            """
            class Foo {
              // asdf
                companion object {
                }
            }
              """.trimIndent()
        )).isEqualTo("""
            class Foo {
                // asdf
                companion object {
                }
            }
        """.trimIndent())
    }
}
