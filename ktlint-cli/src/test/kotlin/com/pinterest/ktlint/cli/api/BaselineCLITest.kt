package com.pinterest.ktlint.cli.api

import com.pinterest.ktlint.cli.CommandLineTestRunner
import com.pinterest.ktlint.cli.containsLineMatching
import com.pinterest.ktlint.cli.doesNotContainLineMatching
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BaselineCLITest {
    @Test
    fun `Given files containing lint errors then find those errors when the baseline is ignored`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "baseline",
                listOf(
                    "TestBaselineFile.kt.test",
                    "some/path/to/TestBaselineFile2.kt.test"
                ),
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(normalOutput)
                        .containsLineMatching(Regex("^TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                        .containsLineMatching(Regex("^TestBaselineFile.kt.test:2:1: Unexpected blank line.*"))
                        .containsLineMatching(Regex("^some/path/to/TestBaselineFile2.kt.test:1:25: Unnecessary block.*"))
                        .containsLineMatching(Regex("^some/path/to/TestBaselineFile2.kt.test:2:1: Unexpected blank line.*"))
                }.assertAll()
            }
    }

    @Test
    fun `Given files containing lint errors which are all registered in the baseline file and as of that are all ignored`(
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
                    "some/path/to/TestBaselineFile2.kt.test",
                ),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput)
                        .doesNotContainLineMatching(Regex("^TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                        .doesNotContainLineMatching(Regex("^TestBaselineFile.kt.test:2:1: Unexpected blank line.*"))
                        .doesNotContainLineMatching(Regex("^some/path/to/TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                        .doesNotContainLineMatching(Regex("^some/path/to/TestBaselineFile.kt.test:2:1: Unexpected blank line.*"))
                        .containsLineMatching(
                            Regex(
                                ".*Baseline file '$baselinePath' contains 6 reference\\(s\\) to rule ids without a rule set id. For " +
                                    "those references the rule set id 'standard' is assumed. It is advised to regenerate this baseline " +
                                    "file.*",
                            ),
                        )
                }.assertAll()
            }
    }

    @Test
    fun `Given files containing lint errors which are not registered in the baseline file and as of that are all reported`(
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
                    "some/path/to/TestBaselineExtraErrorFile2.kt.test",
                ),
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(normalOutput)
                        .containsLineMatching(Regex("^TestBaselineExtraErrorFile.kt.test:2:1: Unexpected blank line.*"))
                        .containsLineMatching(Regex("^some/path/to/TestBaselineExtraErrorFile2.kt.test:2:1: Unexpected blank line.*"))
                        .containsLineMatching(
                            Regex(
                                ".*Baseline file '$baselinePath' contains 6 reference\\(s\\) to rule ids without a rule set id. For " +
                                    "those references the rule set id 'standard' is assumed. It is advised to regenerate this baseline " +
                                    "file.*",
                            ),
                        )
                }.assertAll()
            }
    }
}
