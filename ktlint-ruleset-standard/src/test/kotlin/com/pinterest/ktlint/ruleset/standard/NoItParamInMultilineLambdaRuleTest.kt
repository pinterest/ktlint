package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoItParamInMultilineLambdaRuleTest {

    @Test
    fun testLint() {
        assertThat(
            NoItParamInMultilineLambdaRule().diffFileLint(
                "spec/no-it-in-multiline-lambda/lint.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testShouldNotReportWhenItAsAVariableIsUsed() {
        val itParamText =
            """
            fun main() {
                appendCommaSeparated(properties) { prop ->
                    val it = prop.get(obj)
                    it.get()
                }
            }
            """.trimIndent()
        assertThat(NoItParamInMultilineLambdaRule().lint(itParamText)).isEmpty()
    }

    @Test
    fun testShouldNotReportWhenItIsFunctionName() {
        val itParamText =
            """
            fun main() {
                describe("if this") {
                    it("does that") { 
                       //     
                    }
                }
            }
            """.trimIndent()
        assertThat(NoItParamInMultilineLambdaRule().lint(itParamText)).isEmpty()
    }

    @Test
    fun testShouldNotReportWhenItIsBeingAssignedToAVariableAndPassedToFunction() {
        val itParamText =
            """
            fun main() {
                appendCommaSeparated(properties) { 
                    val it = 4
                    bar(it)
                }
            }
            """.trimIndent()
        assertThat(NoItParamInMultilineLambdaRule().lint(itParamText)).isEmpty()
    }

    @Test
    fun testShouldReportWhenNonIsBeingAssignedToAVariableAndItIsPassedToFunction() {
        val itParamText =
            """
            fun main() {
                appendCommaSeparated(properties) { 
                    val iit = 4
                    bar(it)
                }
            }
            """.trimIndent()
        assertThat(NoItParamInMultilineLambdaRule().lint(itParamText))
            .isEqualTo(
                listOf(
                    LintError(
                        line = 4,
                        col = 13,
                        ruleId = "no-it-in-multiline-lambda",
                        detail = "Multiline lambda must explicitly name \"it\" parameter"
                    )
                )
            )
    }

    @Test
    fun testShouldReportWhenItIsBeingAssignedToAVariable() {
        val itParamText =
            """
            fun main() {
                appendCommaSeparated(properties) { 
                    val foo = 4
                    val bar = it
                }
            }
            """.trimIndent()
        assertThat(NoItParamInMultilineLambdaRule().lint(itParamText))
            .isEqualTo(
                listOf(
                    LintError(
                        line = 4,
                        col = 19,
                        ruleId = "no-it-in-multiline-lambda",
                        detail = "Multiline lambda must explicitly name \"it\" parameter"
                    )
                )
            )
    }

    @Test
    fun testShouldReportWhenItIsBeingSendInAFunction() {
        val itParamText =
            """
            fun main() {
                appendCommaSeparated(properties) { 
                    val foo = 4
                    bar(foo, it)
                }
            }
            """.trimIndent()
        assertThat(NoItParamInMultilineLambdaRule().lint(itParamText))
            .isEqualTo(
                listOf(
                    LintError(
                        line = 4,
                        col = 18,
                        ruleId = "no-it-in-multiline-lambda",
                        detail = "Multiline lambda must explicitly name \"it\" parameter"
                    )
                )
            )
    }
}
