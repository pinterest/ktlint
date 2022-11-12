package com.pinterest.ktlint

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CLI ruleset loader checks")
class RuleSetsLoaderCLITest : BaseCLITest() {
    @Test
    fun `Display warning when the provided custom ruleset does not contains a ruleset provider`() {
        val jarWithoutRulesetProvider = "custom-ruleset/rule-set-provider-v2/ktlint-reporter-html.jar"
        runKtLintCliProcess(
            "custom-ruleset",
            listOf("-R", "$BASE_DIR_PLACEHOLDER/$jarWithoutRulesetProvider"),
        ) {
            SoftAssertions().apply {
                assertNormalExitCode()
                assertThat(normalOutput).containsLineMatching("$jarWithoutRulesetProvider, provided as command line argument, does not contain a custom ruleset provider.")
            }.assertAll()
        }
    }
}
