package yourpkgname

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetaInfServicesTest {
    @Test
    fun `Check if RuleSetProviderV3 is registered in META-INF services`() {
        // TODO: Replace RuleSetProviderV3 with RuleSetV2Provider after release 2.0
        val classNameRuleSetProviderBaseClass = RuleSetProviderV3::class.java.name
        val classNameRuleSetProviderImplementationClass = CustomRuleSetProvider::class.java.name

        val actual = getResourceWithPath("META-INF/services/$classNameRuleSetProviderBaseClass")

        assertThat(actual).isEqualTo(classNameRuleSetProviderImplementationClass)
    }

    private fun getResourceWithPath(path: String) =
        (
            ClassLoader
                .getSystemClassLoader()
                .getResourceAsStream(path)
                ?: throw RuntimeException("Expected a resource to exist with path '$path'. Was the class renamed?")
        ).bufferedReader()
            .readText()
            .trim()
}
