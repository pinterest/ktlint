package com.pinterest.ktlint

import java.nio.file.Path
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RuleSetsLoaderCLITest {
    @Test
    fun `Display warning when the provided custom ruleset does not contains a ruleset provider`(
        @TempDir
        tempDir: Path,
    ) {
        val jarWithoutRulesetProvider = "custom-ruleset/rule-set-provider-v2/ktlint-reporter-html.jar"
        CommandLineTestRunner(tempDir)
            .run(
                "custom-ruleset",
                listOf("-R", "$tempDir/$jarWithoutRulesetProvider"),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput).containsLineMatching("$jarWithoutRulesetProvider, provided as command line argument, does not contain a custom ruleset provider.")
                }.assertAll()
            }
    }

    @Test
    fun `Given a custom rule set with RulesetProviderV2 defined`(
        @TempDir
        tempDir: Path,
    ) {
        val jarWithRulesetProviderV2 = "custom-ruleset/rule-set-provider-v2/ktlint-ruleset-template.jar"
        CommandLineTestRunner(tempDir)
            .run(
                "custom-ruleset",
                listOf("-R", "$tempDir/$jarWithRulesetProviderV2"),
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(normalOutput)
                        .containsLineMatching(Regex(".* JAR ruleset provided with path .*$jarWithRulesetProviderV2.*"))
                        .containsLineMatching(Regex(".*custom-ruleset.rule-set-provider-v2.Main.kt:1:1: Unexpected var, use val instead.*custom-rule-set-id:no-var.*"))
                }.assertAll()
            }
    }
}
