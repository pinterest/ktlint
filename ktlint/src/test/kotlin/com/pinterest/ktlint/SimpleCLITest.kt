package com.pinterest.ktlint

import java.io.ByteArrayInputStream
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@DisplayName("CLI basic checks")
class SimpleCLITest : BaseCLITest() {
    /**
     * For some reason, the external `ktlint --help` process hangs on Windows.
     */
    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Given CLI argument --help then return the help output`() {
        runKtLintCliProcess(
            "no-code-style-error",
            listOf("--help"),
        ) {
            SoftAssertions().apply {
                assertNormalExitCode()
                assertErrorOutputIsEmpty()
                assertThat(normalOutput).containsLineMatching("An anti-bikeshedding Kotlin linter with built-in formatter.")
                assertThat(normalOutput).containsLineMatching("Usage:")
                assertThat(normalOutput).containsLineMatching("Examples:")
            }.assertAll()
        }
    }

    @Test
    fun `Given CLI argument --version then return the version information output`() {
        runKtLintCliProcess(
            "no-code-style-error",
            listOf("--version"),
        ) {
            SoftAssertions().apply {
                assertNormalExitCode()
                assertErrorOutputIsEmpty()
                assertThat(normalOutput).containsExactly(System.getProperty("ktlint-version"))
            }.assertAll()
        }
    }

    @Test
    fun `Given some code without errors then return from lint with normal exit code and no error output`() {
        runKtLintCliProcess(
            "no-code-style-error",
        ) {
            SoftAssertions().apply {
                assertNormalExitCode()
                assertErrorOutputIsEmpty()
            }.assertAll()
        }
    }

    @Test
    fun `Given some code with an error then return from lint with the error exit code and error output`() {
        runKtLintCliProcess(
            "too-many-empty-lines",
        ) {
            SoftAssertions().apply {
                assertErrorExitCode()
                assertThat(normalOutput).containsLineMatching("Needless blank line(s)")
            }.assertAll()
        }
    }

    @Test
    fun `Given some code with an error but a glob which does not select the file`() {
        val somePattern = "some-pattern"
        runKtLintCliProcess(
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
    fun `Given some code with an error which can be autocorrected then return from from with the normal exit code`() {
        runKtLintCliProcess(
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
    fun `Given some code which only contains errors for rules which are disabled via CLI argument --disabled_rules then return from lint with the normal exit code and without error output`() {
        runKtLintCliProcess(
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
    fun `Given some code with an error and a pattern which is read in from stdin which does not select the file then return the no files matched warning`() {
        val somePatternProvidedViaStdin = "some-pattern-provided-via-stdin"
        runKtLintCliProcess(
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
}
