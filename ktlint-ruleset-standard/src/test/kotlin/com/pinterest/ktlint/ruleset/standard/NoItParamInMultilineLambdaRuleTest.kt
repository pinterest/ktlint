package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileLint
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
}
