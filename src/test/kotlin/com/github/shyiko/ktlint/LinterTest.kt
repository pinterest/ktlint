package com.github.shyiko.ktlint

import com.github.shyiko.ktlint.rule.NoWildcardImportsRule
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class LinterTest {

    @Test
    fun testErrorSuppression() {
        assertThat(NoWildcardImportsRule().lint(
            """
            import a.* // ktlint-disable
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 10, "rule-id", "Wildcard import")
        ))
        assertThat(NoWildcardImportsRule().lint(
            """
            import a.* // ktlint-disable rule-id
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(2, 10, "rule-id", "Wildcard import")
        ))
        assertThat(NoWildcardImportsRule().lint(
            """
            /* ktlint-disable */
            import a.*
            import a.*
            /* ktlint-enable */
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(5, 10, "rule-id", "Wildcard import")
        ))
        assertThat(NoWildcardImportsRule().lint(
            """
            /* ktlint-disable rule-id */
            import a.*
            import a.*
            /* ktlint-enable rule-id */
            import a.* // will trigger an error
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(5, 10, "rule-id", "Wildcard import")
        ))
    }

}
