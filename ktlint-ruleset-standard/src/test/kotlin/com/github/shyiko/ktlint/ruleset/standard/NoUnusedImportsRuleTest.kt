package com.github.shyiko.ktlint.ruleset.standard

import com.gihub.shyiko.ktlint.ruleset.standard.NoUnusedImportsRule
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

            fun main() {
                println(a())
                C.call(B())
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
