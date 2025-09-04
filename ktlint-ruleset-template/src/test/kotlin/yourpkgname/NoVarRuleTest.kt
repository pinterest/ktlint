package yourpkgname

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoVarRuleTest {
    private val wrappingRuleAssertThat = assertThatRule { NoVarRule() }

    @Test
    fun `No var rule`() {
        val code =
            """
            fun fn() {
                var v = "var"
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(2, 5, "Unexpected var, use val instead")
    }
}
