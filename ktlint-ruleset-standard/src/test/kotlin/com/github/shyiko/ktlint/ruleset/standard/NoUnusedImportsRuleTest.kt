package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.ruleset.standard.NoUnusedImportsRule
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import com.github.shyiko.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoUnusedImportsRuleTest {

    @Test
    fun testLint() {
        assertThat(NoUnusedImportsRule().lint(
            """
            import p.a
            import p.B6
            import java.nio.file.Paths
            import p.B as B12
            import p2.B
            import p.C
            import p.a.*
            import escaped.`when`
            import escaped.`foo`
            import p.infixfunc

            fun main() {
                println(a())
                C.call(B())
                1 infixfunc 2
                `when`()
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 1, "no-unused-imports", "Unused import"),
            LintError(3, 1, "no-unused-imports", "Unused import"),
            LintError(4, 1, "no-unused-imports", "Unused import"),
            LintError(9, 1, "no-unused-imports", "Unused import")
        ))
        assertThat(NoUnusedImportsRule().lint(
            """
            import rx.lang.kotlin.plusAssign

            fun main() {
                v += 1
            }
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testSamePackageImport() {
        assertThat(NoUnusedImportsRule().lint(
            """


            import C1
            import C1 as C1X
            import `C2`
            import `C2` as C2X
            import C3.method

            fun main() {
                println(C1, C1X, C2, C2X, method)
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(3, 1, "no-unused-imports", "Unnecessary import"),
            LintError(5, 1, "no-unused-imports", "Unnecessary import")
        ))
        assertThat(NoUnusedImportsRule().lint(
            """
            package p

            import p.C1
            import p.C1 as C1X
            import p.`C2`
            import p.`C2` as C2X
            import p.C3.method

            fun main() {
                println(C1, C1X, C2, C2X, method)
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(3, 1, "no-unused-imports", "Unnecessary import"),
            LintError(5, 1, "no-unused-imports", "Unnecessary import")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(NoUnusedImportsRule().format(
            """
            import p.a
            import p.B6
            import p.B as B12
            import p2.B as B2
            import p.C
            import escaped.`when`
            import escaped.`foo`

            fun main() {
                println(a())
                C.call()
                fn(B2.NAME)
                `when`()
            }
            """.trimIndent()
        )).isEqualTo(
            """
            import p.a
            import p2.B as B2
            import p.C
            import escaped.`when`

            fun main() {
                println(a())
                C.call()
                fn(B2.NAME)
                `when`()
            }
            """.trimIndent()
        )
    }

}
