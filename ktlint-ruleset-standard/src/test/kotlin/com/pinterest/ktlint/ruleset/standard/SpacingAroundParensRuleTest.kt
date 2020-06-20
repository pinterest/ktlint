package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundParensRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundParensRule().diffFileLint("spec/paren-spacing/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundParensRule().diffFileFormat(
                "spec/paren-spacing/format.kt.spec",
                "spec/paren-spacing/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `lint spacing after lpar followed by a comment is allowed`() {
        assertThat(
            SpacingAroundParensRule().lint(
                """
                fun main() {
                    System.out.println( /** 123 */
                        "test kdoc"
                    )
                    System.out.println( /* 123 */
                        "test comment block"
                    )
                    System.out.println( // 123
                        "test single comment"
                    )
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format spacing after lpar followed by a comment is allowed`() {
        val code =
            """
            fun main() {
                System.out.println( /** 123 */
                    "test kdoc"
                )
                System.out.println( /* 123 */
                    "test comment block"
                )
                System.out.println( // 123
                    "test single comment"
                )
            }
            """.trimIndent()
        assertThat(SpacingAroundParensRule().format(code)).isEqualTo(code)
    }

    @Test
    fun `lint spacing before annotated function type is allowed`() {
        assertThat(
            SpacingAroundParensRule().lint(
                """
                val myFun1: @Foo () -> Unit = {}
                val myFun2: @Foo() () -> Unit = {}
                val typeParameter1: (@Foo () -> Unit) -> Unit = { {} }
                val typeParameter2: (@Foo() () -> Unit) -> Unit = { { } }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format spacing before annotated function type is allowed`() {
        val code =
            """
            val myFun1: @Foo () -> Unit = {}
            val myFun2: @Foo() () -> Unit = {}
            val typeParameter1: (@Foo () -> Unit) -> Unit = { {} }
            val typeParameter2: (@Foo() () -> Unit) -> Unit = { { } }
            """.trimIndent()
        assertThat(SpacingAroundParensRule().format(code)).isEqualTo(code)
    }
}
