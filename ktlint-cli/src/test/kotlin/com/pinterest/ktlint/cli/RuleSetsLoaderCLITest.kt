package com.pinterest.ktlint.cli

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RuleSetsLoaderCLITest {
    @Test
    fun `Given a custom jar that does not contain the required RuleSetProviderV3 then display an error and exit`(
        @TempDir
        tempDir: Path,
    ) {
        val jarWithoutRulesetProviderV3 = "custom-ruleset/rule-set-provider-v2/ktlint-cli-reporter-html.jar"
        CommandLineTestRunner(tempDir)
            .run(
                "custom-ruleset",
                listOf("-R", "$tempDir/$jarWithoutRulesetProviderV3", "**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput + errorOutput)
                            .containsLineMatching(
                                Regex(
                                    ".*ERROR.* JAR file '.*$jarWithoutRulesetProviderV3' is missing a class implementing interface " +
                                        "'com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3'",
                                ),
                            )
                    }.assertAll()
            }
    }

    @Disabled("Keep test around as example if the current ruleset provider is deprecated in the future")
    @Test
    fun `Given a custom ruleset jar that contains the deprecated RuleSetProviderV2 then display an error and exit`(
        @TempDir
        tempDir: Path,
    ) {
        val jarWithRulesetProviderV2 = "custom-ruleset/rule-set-provider-v2/ktlint-test-ruleset-provider-v2-deprecated.jar"
        CommandLineTestRunner(tempDir)
            .run(
                "custom-ruleset",
                listOf("-R", "$tempDir/$jarWithRulesetProviderV2", "**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput + errorOutput)
                            .containsLineMatching(
                                Regex(
                                    ".*ERROR.* JAR file '.*$jarWithRulesetProviderV2' contains a class implementing an unsupported " +
                                        "interface 'com.pinterest.ktlint.core.RuleSetProviderV2'",
                                ),
                            )
                    }.assertAll()
            }
    }
}
