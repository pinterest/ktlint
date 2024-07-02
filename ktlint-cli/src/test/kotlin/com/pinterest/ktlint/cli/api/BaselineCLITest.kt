package com.pinterest.ktlint.cli.api

import com.pinterest.ktlint.cli.CommandLineTestRunner
import com.pinterest.ktlint.cli.containsLineMatching
import com.pinterest.ktlint.cli.doesNotContainLineMatching
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BaselineCLITest {
    @Test
    fun `Given files containing lint errors then find those errors when the baseline is ignored`(
        @TempDir
        tempDir: Path,
    ) {
        val projectName = "baseline"
        CommandLineTestRunner(tempDir)
            .run(
                projectName,
                listOf(
                    "TestBaselineFile.kt.test",
                    "some/path/to/TestBaselineFile.kt.test",
                ),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching(Regex(".*/$projectName/TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                            .containsLineMatching(Regex(".*/$projectName/some/path/to/TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                    }.assertAll()
            }
    }

    @Nested
    inner class `Given files containing lint errors which are all registered in the baseline file and as of that are all ignored` {
        @Test
        fun `Given a baseline file in the root of the working directory`(
            @TempDir
            tempDir: Path,
        ) {
            val projectName = "baseline"
            val baselinePath = "test-baseline.xml"

            CommandLineTestRunner(tempDir)
                .run(
                    projectName,
                    listOf(
                        "--baseline=$baselinePath",
                        "--format",
                        "TestBaselineFile.kt.test",
                        "some/path/to/TestBaselineFile.kt.test",
                    ),
                ) {
                    SoftAssertions()
                        .apply {
                            assertNormalExitCode()
                            assertThat(normalOutput)
                                .doesNotContainLineMatching(Regex(".*/$projectName/TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                                .doesNotContainLineMatching(
                                    Regex(".*/$projectName/some/path/to/TestBaselineFile.kt.test:1:24: Unnecessary block.*"),
                                ).containsLineMatching(
                                    Regex(
                                        ".*Baseline file '$baselinePath' contains 4 reference\\(s\\) to rule ids without a rule set id. " +
                                            "For those references the rule set id 'standard' is assumed. It is advised to regenerate " +
                                            "this baseline file.*",
                                    ),
                                ).doesNotContainLineMatching(
                                    Regex(
                                        ".*Format was not able to resolve all violations which \\(theoretically\\) can be autocorrected in file.*",
                                    ),
                                )
                        }.assertAll()
                }
        }

        @Test
        fun `Given a baseline file in a subdirectory of the working directory`(
            @TempDir
            tempDir: Path,
        ) {
            val projectName = "baseline"
            val baselinePath = "config/test-baseline.xml"

            CommandLineTestRunner(tempDir)
                .run(
                    projectName,
                    listOf(
                        "--baseline=$baselinePath",
                        "TestBaselineFile.kt.test",
                        "some/path/to/TestBaselineFile.kt.test",
                    ),
                ) {
                    SoftAssertions()
                        .apply {
                            assertNormalExitCode()
                            assertThat(normalOutput)
                                .doesNotContainLineMatching(Regex(".*/$projectName/TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                                .doesNotContainLineMatching(
                                    Regex(".*/$projectName/some/path/to/TestBaselineFile.kt.test:1:24: Unnecessary block.*"),
                                ).containsLineMatching(
                                    Regex(
                                        ".*Baseline file '$baselinePath' contains 4 reference\\(s\\) to rule ids without a rule set id. " +
                                            "For those references the rule set id 'standard' is assumed. It is advised to regenerate " +
                                            "this baseline file.*",
                                    ),
                                )
                        }.assertAll()
                }
        }

        @Test
        fun `Given a baseline file with an absolute path, not necessarily in the working directory or one of its subdirectories`(
            @TempDir
            tempDir: Path,
        ) {
            val baselinePath = "$tempDir/baseline/config/test-baseline.xml"

            CommandLineTestRunner(tempDir)
                .run(
                    "baseline",
                    listOf(
                        "--baseline=$baselinePath",
                        "TestBaselineFile.kt.test",
                        "some/path/to/TestBaselineFile.kt.test",
                    ),
                ) {
                    SoftAssertions()
                        .apply {
                            assertNormalExitCode()
                            assertThat(normalOutput)
                                .doesNotContainLineMatching(Regex("^TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                                .doesNotContainLineMatching(Regex("^some/path/to/TestBaselineFile.kt.test:1:24: Unnecessary block.*"))
                                .containsLineMatching(
                                    Regex(
                                        // Escape "\" in baseline path for Windows
                                        ".*Baseline file '${baselinePath.replace("\\", "\\\\")}' contains 4 " +
                                            "reference\\(s\\) to rule ids without a rule set id. For those references the rule set id " +
                                            "'standard' is assumed. It is advised to regenerate this baseline file.*",
                                    ),
                                )
                        }.assertAll()
                }
        }
    }

    @Test
    fun `Given files containing lint errors which are not registered in the baseline file and as of that are all reported`(
        @TempDir
        tempDir: Path,
    ) {
        val projectName = "baseline"
        val baselinePath = "test-baseline.xml"

        CommandLineTestRunner(tempDir)
            .run(
                projectName,
                listOf(
                    "--baseline=$baselinePath",
                    "TestBaselineExtraErrorFile.kt.test",
                    "some/path/to/TestBaselineExtraErrorFile.kt.test",
                ),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching(
                                Regex(
                                    ".*/$projectName/TestBaselineExtraErrorFile.kt.test:1:1: Replace the block comment with an " +
                                        "EOL comment.*",
                                ),
                            ).containsLineMatching(
                                Regex(
                                    ".*/$projectName/some/path/to/TestBaselineExtraErrorFile.kt.test:1:1: Replace the block comment with " +
                                        "an EOL comment.*",
                                ),
                            ).containsLineMatching(
                                Regex(
                                    ".*Baseline file '$baselinePath' contains 4 reference\\(s\\) to rule ids without a rule set id. For " +
                                        "those references the rule set id 'standard' is assumed. It is advised to regenerate this " +
                                        "baseline file.*",
                                ),
                            )
                    }.assertAll()
            }
    }
}
