package yourpkgname

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoVarRuleTest {
    private val wrappingRuleAssertThat = assertThatRule { NoVarRule() }

    @Test
    fun `No var rule`() {
        // whenever KTLINT_DEBUG env variable is set to "ast" or -DktlintDebug=ast is used
        // com.pinterest.ktlint.test.(lint|format) will print AST (along with other debug info) to the stderr.
        // this can be extremely helpful while writing and testing rules.
        // uncomment the line below to take a quick look at it
        // System.setProperty("ktlintDebug", "ast")
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
