package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.diffFileLint
import com.pinterest.ktlint.test.format
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpacingAroundAngleBracketRuleTest {

    @Test
    fun testLint() {
        assertThat(SpacingAroundAngleBracketsRule().diffFileLint("spec/spacing-around-angle-brackets/lint.kt.spec")).isEmpty()
    }

    @Test
    fun testFormat() {
        assertThat(
            SpacingAroundAngleBracketsRule().diffFileFormat(
                "spec/spacing-around-angle-brackets/format.kt.spec",
                "spec/spacing-around-angle-brackets/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun `lint keywords which allow preceding whitespace`() {
        assertThat(
            SpacingAroundAngleBracketsRule().lint(
                """
                public class AngleTest<B : String> {
                    val     <T> T.exhaustive get() = this;
                    fun     <T> compare(other: T) {}
                    var     <T> T.exhaustive: T get() = this;
                }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun `format reified keyword within angle brackets`() {
        assertThat(
            SpacingAroundAngleBracketsRule().format(
                """
                interface Consumer< reified T    > {
                    fun add(item: T)
                }
                """.trimIndent()
            )
        ).isEqualTo(
            """
            interface Consumer<reified T> {
                fun add(item: T)
            }
            """.trimIndent()
        )
    }
}
