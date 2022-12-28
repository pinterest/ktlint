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
                    assertNormalExitCode()
                    // JAR ruleset provided with path "/var/folders/24/wtp_g21953x22nr8z86gvltc0000gp/T/junit920502858262478102/custom-ruleset/rule-set-provider-v2/ktlint-ruleset-template.jar
                    // Add editor config override to enable rule set(s) '[indent-string-template-ruleset]' from custom rule set JAR('s): '[/var/folders/24/wtp_g21953x22nr8z86gvltc0000gp/T/junit920502858262478102/custom-ruleset/rule-set-provider-v2/ktlint-ruleset-template.jar]'
                    assertThat(normalOutput)
                        .containsLineMatching(Regex(".* JAR ruleset provided with path .*$jarWithRulesetProviderV2.*"))
                        .containsLineMatching(Regex(".* Add editor config override to enable rule set\\(s\\) '\\[indent-string-template-ruleset]' from custom rule set JAR\\('s\\): .*$jarWithRulesetProviderV2.*"))
                }.assertAll()
            }
    }
}
