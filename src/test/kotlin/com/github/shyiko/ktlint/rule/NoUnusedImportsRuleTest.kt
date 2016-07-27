package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.LintError
import com.github.shyiko.ktlint.format
import com.github.shyiko.ktlint.lint
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

            fun main() {
                println(a())
                C.call(B())
            }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 1, "rule-id", "Unused import"),
            LintError(3, 1, "rule-id", "Unused import"),
            LintError(4, 1, "rule-id", "Unused import")
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

            fun main() {
                println(a())
                C.call()
                fn(B2.NAME)
            }
            """.trimIndent()
        )).isEqualTo(
            """
            import p.a
            import p2.B as B2
            import p.C

            fun main() {
                println(a())
                C.call()
                fn(B2.NAME)
            }
            """.trimIndent()
        )
    }

}
