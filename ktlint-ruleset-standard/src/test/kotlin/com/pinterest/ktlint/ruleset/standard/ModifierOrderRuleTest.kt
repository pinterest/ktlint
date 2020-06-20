package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ModifierOrderRuleTest {

    @Test
    fun testLint() {
        // pretty much every line below should trip an error
        assertThat(
            ModifierOrderRule().lint(
                """
                abstract @Deprecated open class A { // open is here for test purposes only, otherwise it's redundant
                    open protected val v = ""
                    open suspend internal fun f(v: Any): Any = ""
                    lateinit public var lv: String
                    tailrec abstract fun findFixPoint(x: Double = 1.0): Double
                }

                class B : A() {
                    override public val v = ""
                    suspend override fun f(v: Any): Any = ""
                    tailrec override fun findFixPoint(x: Double): Double
                        = if (x == Math.cos(x)) x else findFixPoint(Math.cos(x))
                    override @Annotation fun getSomething() = ""
                    override @Annotation suspend public @Woohoo(data = "woohoo") fun doSomething() = ""
                    @A
                    @B(v = [
                        "foo",
                        "baz",
                        "bar"
                    ])
                    @C
                    suspend public fun returnsSomething() = ""

                    companion object {
                       const internal val V = ""
                    }
                }
                """.trimIndent()
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "modifier-order", "Incorrect modifier order (should be \"@Annotation... open abstract\")"),
                LintError(2, 5, "modifier-order", "Incorrect modifier order (should be \"protected open\")"),
                LintError(3, 5, "modifier-order", "Incorrect modifier order (should be \"internal open suspend\")"),
                LintError(4, 5, "modifier-order", "Incorrect modifier order (should be \"public lateinit\")"),
                LintError(5, 5, "modifier-order", "Incorrect modifier order (should be \"abstract tailrec\")"),
                LintError(9, 5, "modifier-order", "Incorrect modifier order (should be \"public override\")"),
                LintError(10, 5, "modifier-order", "Incorrect modifier order (should be \"override suspend\")"),
                LintError(11, 5, "modifier-order", "Incorrect modifier order (should be \"override tailrec\")"),
                LintError(13, 5, "modifier-order", "Incorrect modifier order (should be \"@Annotation... override\")"),
                LintError(14, 5, "modifier-order", "Incorrect modifier order (should be \"@Annotation... public override suspend\")"),
                LintError(15, 5, "modifier-order", "Incorrect modifier order (should be \"@Annotation... public suspend\")"),
                LintError(25, 8, "modifier-order", "Incorrect modifier order (should be \"internal const\")")
            )
        )
    }

    @Test
    fun testFormat() {
        assertThat(
            ModifierOrderRule().format(
                """
            abstract @Deprecated open class A { // open is here for test purposes only, otherwise it's redundant
                open protected val v = ""
                open suspend internal fun f(v: Any): Any = ""
                lateinit public var lv: String
                tailrec abstract fun findFixPoint(x: Double = 1.0): Double
            }

            class B : A() {
                override public val v = ""
                suspend override fun f(v: Any): Any = ""
                tailrec override fun findFixPoint(x: Double): Double
                    = if (x == Math.cos(x)) x else findFixPoint(Math.cos(x))
                override @Annotation fun getSomething() = ""
                suspend @Annotation override public @Woohoo(data = "woohoo") fun doSomething() = ""
                @A
                @B(v = [
                    "foo",
                    "baz",
                    "bar"
                ])
                @C
                suspend public fun returnsSomething() = ""

                companion object {
                   const internal val V = ""
                }
            }
            """
            )
        ).isEqualTo(
            """
            @Deprecated open abstract class A { // open is here for test purposes only, otherwise it's redundant
                protected open val v = ""
                internal open suspend fun f(v: Any): Any = ""
                public lateinit var lv: String
                abstract tailrec fun findFixPoint(x: Double = 1.0): Double
            }

            class B : A() {
                public override val v = ""
                override suspend fun f(v: Any): Any = ""
                override tailrec fun findFixPoint(x: Double): Double
                    = if (x == Math.cos(x)) x else findFixPoint(Math.cos(x))
                @Annotation override fun getSomething() = ""
                @Annotation @Woohoo(data = "woohoo") public override suspend fun doSomething() = ""
                @A
                @B(v = [
                    "foo",
                    "baz",
                    "bar"
                ])
                @C
                public suspend fun returnsSomething() = ""

                companion object {
                   internal const val V = ""
                }
            }
            """
        )
    }
}
