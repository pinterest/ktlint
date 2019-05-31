package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.diffFileLint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class MethodExpressionBodyRuleTest {

    @Test
    fun testLint() {
        assertThat(
            MethodExpressionBodyRule().diffFileLint(
            "spec/method-expression-body/lint.kt.spec"
            )
        ).isEmpty()
    }
}
