package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class NoItParamInMultilineLambdaRuleTest {

    @Test
    fun testLint() {
        assertThat(NoItParamInMultilineLambdaRule().diffFileLint(
            "spec/no-it-in-multiline-lambda/lint.kt.spec"
        )).isEmpty()

    }
}
