package com.pinterest.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@DisabledOnOs(OS.WINDOWS)
@DisplayName("CLI ruleset loader checks")
class RuleSetsLoaderCLITest : BaseCLITest() {
    @DisplayName("Custom rule set based on RuleSetProvider of KtLint 0.46 or before")
    @Nested
    inner class RuleSetProviderV1 {
        @Test
        fun `Display warning when the provided custom ruleset will not compatible with KtLint 0_48`() {
            val jarWithRulesetProviderV1 = "custom-ruleset/rule-set-provider-v1/ktlint-ruleset-template.jar"
            runKtLintCliProcess(
                "custom-ruleset",
                listOf("-R", "$BASE_DIR_PLACEHOLDER/$jarWithRulesetProviderV1")
            ) {
                assertNormalExitCode()

                // "15:36:29.259 [main] WARN com.pinterest.ktlint.internal.LoadRuleProviders - JAR /var/folders/24/wtp_g21953x22nr8z86gvltc0000gp/T/junit5220922714985703035/custom-ruleset/rule-set-provider-v1/ktlint-reporter-html.jar, provided as command line argument, contains a custom ruleset provider which will *NOT* be compatible with the next KtLint version (0.48). Contact the maintainer of this ruleset. This JAR is not maintained by the KtLint project.".matches(Regex(".* WARN .* JAR .*$jarWithoutRulesetProvider, provided as command line argument, contains a custom ruleset provider which is \\*NOT\\* compatible with the next KtLint version \\(0.48\\). Contact the maintainer of this ruleset. This JAR is not maintained by the KtLint project."))
                assertThat(normalOutput)
                    .anyMatch {
                        it.matches(
                            Regex(".* WARN .* JAR .*$jarWithRulesetProviderV1, provided as command line argument, contains a custom ruleset provider which will \\*NOT\\*.* be compatible with the next KtLint version \\(0.48\\). Contact the maintainer of this ruleset. This JAR is not maintained by the KtLint project.*")
                        )
                    }
            }
        }

        @Test
        fun `Display warning when the provided custom ruleset does not contains a ruleset provider`() {
            val jarWithoutRulesetProvider = "custom-ruleset/rule-set-provider-v1/ktlint-reporter-html.jar"
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

    @DisplayName("Custom rule set based on RuleSetProviderV2 of KtLint 0.47 and after")
    @Nested
    inner class RuleSetProviderV2 {
        @Test
        fun `Display no warning about not being compatible with KtLint 0_48`() {
            val jarWithRulesetProviderV2 = "custom-ruleset/rule-set-provider-v2/ktlint-ruleset-template.jar"
            runKtLintCliProcess(
                "custom-ruleset",
                listOf("-R", "$BASE_DIR_PLACEHOLDER/$jarWithRulesetProviderV2")
            ) {
                assertNormalExitCode()

                // "15:36:29.259 [main] WARN com.pinterest.ktlint.internal.LoadRuleProviders - JAR /var/folders/24/wtp_g21953x22nr8z86gvltc0000gp/T/junit5220922714985703035/custom-ruleset/rule-set-provider-v1/ktlint-reporter-html.jar, provided as command line argument, contains a custom ruleset provider which will *NOT* be compatible with the next KtLint version (0.48). Contact the maintainer of this ruleset. This JAR is not maintained by the KtLint project.".matches(Regex(".* WARN .* JAR .*$jarWithoutRulesetProvider, provided as command line argument, contains a custom ruleset provider which is \\*NOT\\* compatible with the next KtLint version \\(0.48\\). Contact the maintainer of this ruleset. This JAR is not maintained by the KtLint project."))
                assertThat(normalOutput)
                    .noneMatch {
                        it.matches(
                            Regex(".* WARN .* JAR .*$jarWithRulesetProviderV2, provided as command line argument, contains a custom ruleset provider which will \\*NOT\\*.* be compatible with the next KtLint version \\(0.48\\). Contact the maintainer of this ruleset. This JAR is not maintained by the KtLint project.*")
                        )
                    }
            }
        }

        @Test
        fun `Display warning when the provided custom ruleset does not contains a ruleset provider`() {
            val jarWithoutRulesetProvider = "custom-ruleset/rule-set-provider-v2/ktlint-reporter-html.jar"
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
}
