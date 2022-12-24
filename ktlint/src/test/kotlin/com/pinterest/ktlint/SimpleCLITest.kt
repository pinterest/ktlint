package com.pinterest.ktlint

import java.io.ByteArrayInputStream
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SimpleCLITest {
    @Test
    fun `Given CLI argument --help then return the help output`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "no-code-style-error",
                listOf("--help"),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput).containsLineMatching("An anti-bikeshedding Kotlin linter with built-in formatter.")
                    assertThat(normalOutput).containsLineMatching("Usage:")
                    assertThat(normalOutput).containsLineMatching("Examples:")
                }.assertAll()
            }
    }

    @Test
    fun `Given CLI argument --version then return the version information output`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "no-code-style-error",
                listOf("--version"),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput).contains(System.getProperty("ktlint-version"))
                }.assertAll()
            }
    }

    @Test
    fun `Given some code without errors then return from lint with normal exit code and no error output`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "no-code-style-error",
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertErrorOutputIsEmpty()
                }.assertAll()
            }
    }

    @Test
    fun `Given some code with an error then return from lint with the error exit code and error output`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(normalOutput).containsLineMatching("Needless blank line(s)")
                }.assertAll()
            }
    }

    @Test
    fun `Given some code with an error but a glob which does not select the file`(
        @TempDir
        tempDir: Path,
    ) {
        val somePattern = "some-pattern"
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf(somePattern),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput).containsLineMatching("No files matched [$somePattern]")
                }.assertAll()
            }
    }

    @Test
    fun `Given some code with an error which can be autocorrected then return from from with the normal exit code`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("-F"),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    // on JDK11+ contains warning about illegal reflective access operation
                    // assertErrorOutputIsEmpty()

                    assertSourceFileWasFormatted("Main.kt")
                }.assertAll()
            }
    }

    @Test
    fun `Given some code which only contains errors for rules which are disabled via CLI argument --disabled_rules then return from lint with the normal exit code and without error output`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("--disabled_rules=no-consecutive-blank-lines,no-empty-first-line-in-method-block"),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput).doesNotContain(
                        "no-consecutive-blank-lines",
                        "no-empty-first-line-in-method-block",
                    )
                }.assertAll()
            }
    }

    @Test
    fun `Given some code with an error and a pattern which is read in from stdin which does not select the file then return the no files matched warning`(
        @TempDir
        tempDir: Path,
    ) {
        val somePatternProvidedViaStdin = "some-pattern-provided-via-stdin"
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("--patterns-from-stdin"),
                stdin = ByteArrayInputStream(somePatternProvidedViaStdin.toByteArray()),
            ) {
                SoftAssertions().apply {
                    assertNormalExitCode()
                    assertThat(normalOutput).containsLineMatching("No files matched [$somePatternProvidedViaStdin]")
                }.assertAll()
            }
    }

    @Test
    fun `Issue 1608 - --relative and --reporter=sarif should play well together`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("--relative", "--reporter=sarif"),
            ) {
                SoftAssertions().apply {
                    assertErrorExitCode()
                    assertThat(errorOutput).doesNotContainLineMatching("Exception in thread \"main\" java.lang.IllegalArgumentException: this and base files have different roots:")
                }.assertAll()
            }
    }

    @Test
    fun `Issue 1742 - Disable the filename rule when --stdin is used`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                testProjectName = "too-many-empty-lines",
                arguments = listOf("--stdin"),
                stdin = ByteArrayInputStream("fun foo() = 42".toByteArray()),
            ) {
                assertThat(normalOutput).containsLineMatching(Regex(".*ktlint_standard_filename: disabled.*"))
            }
    }
}
