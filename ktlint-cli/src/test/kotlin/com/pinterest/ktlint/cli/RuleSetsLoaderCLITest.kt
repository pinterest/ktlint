package com.pinterest.ktlint.cli

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RuleSetsLoaderCLITest {
    @Test
    fun `Given a custom jar that does not contain a valid RuleSetProvider then display an error and exit`(
        @TempDir
        tempDir: Path,
    ) {
        val jarWithoutValidRulesetProvider = "custom-ruleset/ktlint-cli-reporter-html.jar"
        CommandLineTestRunner(tempDir)
            .run(
                "custom-ruleset",
                listOf("-R", "$tempDir/$jarWithoutValidRulesetProvider", "**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput + errorOutput)
                            .containsLineMatching(
                                Regex(
                                    ".*ERROR.* JAR file '.*$jarWithoutValidRulesetProvider' is missing a class implementing interface " +
                                        "'com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV2Provider'",
                                ),
                            )
                    }.assertAll()
            }
    }

    @Test
    fun `Given a custom ruleset jar that contains the deprecated RuleSetProviderV3 then display an error and exit`(
        @TempDir
        tempDir: Path,
    ) {
        val jarWithDeprecatedRulesetProvider = "custom-ruleset/ktlint-ruleset-with-deprecated-ruleset-provider.jar"
        CommandLineTestRunner(tempDir)
            .run(
                "custom-ruleset",
                listOf("-R", "$tempDir/$jarWithDeprecatedRulesetProvider", "**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching(
                                Regex(
                                    ".*WARN.* JAR file '.*$jarWithDeprecatedRulesetProvider' contains a class implementing a deprecated " +
                                        "interface 'com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3'",
                                ),
                            )
                    }.assertAll()
            }
    }
}
