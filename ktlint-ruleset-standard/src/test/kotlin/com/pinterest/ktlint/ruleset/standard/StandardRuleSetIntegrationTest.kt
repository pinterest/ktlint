package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StandardRuleSetIntegrationTest {

    @Test
    fun `test string-template wit unused-imports integration`() {
        val actual = KtLint.format(
            KtLint.Params(
                text = """
                fun foo() = 1
                fun test() {
                    println("${'$'}{foo().toString()}")
                }
                """.trimIndent(),
                ruleSets = listOf(StandardRuleSetProvider().get()),
                cb = { _, _ -> }
            )
        )
        assertThat(actual).isEqualTo(
            """
            fun foo() = 1
            fun test() {
                println("${'$'}{foo()}")
            }

            """.trimIndent()
        )
    }
}
