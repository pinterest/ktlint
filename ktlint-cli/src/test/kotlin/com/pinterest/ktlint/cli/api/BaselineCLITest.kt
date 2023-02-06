package com.pinterest.ktlint.cli.api

import com.pinterest.ktlint.cli.CommandLineTestRunner
import com.pinterest.ktlint.cli.containsLineMatching
import com.pinterest.ktlint.cli.doesNotContainLineMatching
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BaselineCLITest {
    @Test
    fun `Given a file containing lint errors then find those errors when the baseline is ignored`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "baseline",
                listOf("TestBaselineFile.kt.test"),
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(normalOutput)
                        .containsLineMatching(Regex(".*:1:24: Unnecessary block.*"))
                        .containsLineMatching(Regex(".*:2:1: Unexpected blank line.*"))
                }.assertAll()
            }
    }

    @Test
    fun `Given a file containing lint errors which are all registered in the baseline file and as of that are all ignored`(
        @TempDir
        tempDir: Path,
    ) {
        val baselinePath = "test-baseline.xml"

        CommandLineTestRunner(tempDir)
            .run(
                "baseline",
                listOf(
                    "--baseline=$baselinePath",
                    "TestBaselineFile.kt.test",
                ),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput)
                        .doesNotContainLineMatching(Regex(".*:1:24: Unnecessary block.*"))
                        .doesNotContainLineMatching(Regex(".*:2:1: Unexpected blank line.*"))
                        .containsLineMatching(
                            Regex(
                                ".*Baseline file '$baselinePath' contains 3 reference\\(s\\) to rule ids without a rule set id. For " +
                                    "those references the rule set id 'standard' is assumed. It is advised to regenerate this baseline " +
                                    "file.*",
                            ),
                        )
                }.assertAll()
            }
    }

    @Test
    fun `Given a file containing lint errors which are not registered in the baseline file and as of that are all reported`(
        @TempDir
        tempDir: Path,
    ) {
        val baselinePath = "test-baseline.xml"

        CommandLineTestRunner(tempDir)
            .run(
                "baseline",
                listOf(
                    "--baseline=$baselinePath",
                    "TestBaselineExtraErrorFile.kt.test",
                ),
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(normalOutput)
                        .containsLineMatching(Regex(".*:2:1: Unexpected blank line.*"))
                        .containsLineMatching(
                            Regex(
                                ".*Baseline file '$baselinePath' contains 3 reference\\(s\\) to rule ids without a rule set id. For " +
                                    "those references the rule set id 'standard' is assumed. It is advised to regenerate this baseline " +
                                    "file.*",
                            ),
                        )
                }.assertAll()
            }
    }
}
