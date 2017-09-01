package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.format
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoUnitReturnRuleTest {

    @Test
    fun testLint() {
        assertThat(NoUnitReturnRule().lint(
            """
            fun f1() {}
            fun f2(): Unit {}
            fun f2(): Unit = start()
            fun f2_(): Unit /**/
                = start()
            fun f3(): String = ""
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 11, "no-unit-return", "Unnecessary \"Unit\" return type")
        ))
    }

    @Test
    fun testFormat() {
        assertThat(NoUnitReturnRule().format(
            """
            fun f1() {}
            fun f2(): Unit {}
            fun f3(): String = ""
            fun f4(a: Unit): Unit {}
            """
        )).isEqualTo(
            """
            fun f1() {}
            fun f2() {}
            fun f3(): String = ""
            fun f4(a: Unit) {}
            """
        )
    }

}
