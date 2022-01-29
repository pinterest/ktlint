package com.pinterest.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@DisabledOnOs(OS.WINDOWS)
@DisplayName("CLI ruleset loader checks")
class RuleSetsLoaderCLITest : BaseCLITest() {
    @Test
    fun `Display no warning when the provided custom ruleset contains a ruleset provider`() {
        runKtLintCliProcess(
            "custom-ruleset",
            listOf("-R $BASE_DIR_PLACEHOLDER/custom-ruleset/ktlint-ruleset-template.jar")
        ) {
            assertNormalExitCode()

            assertThat(normalOutput)
                .noneMatch {
                    it.matches(
                        Regex(".* WARN .* JAR .* provided as command line argument, does not contain a custom ruleset provider.")
                    )
                }
        }
    }

    @Test
    fun `Display warning when the provided custom ruleset does not contains a ruleset provider`() {
        val jarWithoutRulesetProvider = "custom-ruleset/ktlint-reporter-html.jar"
        runKtLintCliProcess(
            "custom-ruleset",
            listOf("-R", "$BASE_DIR_PLACEHOLDER/$jarWithoutRulesetProvider")
        ) {
            assertNormalExitCode()

            assertThat(normalOutput)
                .anyMatch {
                    it.matches(
                        Regex(".* WARN .* JAR .*$jarWithoutRulesetProvider, provided as command line argument, does not contain a custom ruleset provider.")
                    )
                }
        }
    }
}
