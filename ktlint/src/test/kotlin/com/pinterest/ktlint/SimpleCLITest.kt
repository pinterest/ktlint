package com.pinterest.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@DisabledOnOs(OS.WINDOWS)
@DisplayName("CLI basic checks")
class SimpleCLITest : BaseCLITest() {
    @Test
    fun `Given CLI argument --help then return the help output`() {
        runKtLintCliProcess(
            "no-code-style-error",
            listOf("--help"),
        ) {
            assertNormalExitCode()
            assertErrorOutputIsEmpty()

            assert(normalOutput.contains("Usage:") && normalOutput.contains("Examples:")) {
                "Did not produced help output!\n ${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @Test
    fun `Given CLI argument --version then return the version information output`() {
        runKtLintCliProcess(
            "no-code-style-error",
            listOf("--version"),
        ) {
            assertNormalExitCode()
            assertErrorOutputIsEmpty()

            val expectedVersion = System.getProperty("ktlint-version")
            assert(normalOutput.contains(expectedVersion)) {
                "Output did not contain expected $expectedVersion version:\n ${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @Test
    fun `Given some code without errors then return from lint with normal exit code and no error output`() {
        runKtLintCliProcess(
            "no-code-style-error",
        ) {
            assertNormalExitCode()
            assertErrorOutputIsEmpty()
        }
    }

    @Test
    fun `Given some code with an error then return from lint with the error exit code and error output`() {
        runKtLintCliProcess(
            "too-many-empty-lines",
        ) {
            assertErrorExitCode()

            assert(normalOutput.find { it.contains("Needless blank line(s)") } != null) {
                "Unexpected output:\n${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @Test
    fun `Given some code with an error but a glob which does not select the file`() {
        runKtLintCliProcess(
            "too-many-empty-lines",
            listOf("SomeOtherFile.kt"),
        ) {
            assertErrorExitCode()

            assert(normalOutput.find { it.contains("No files matched [SomeOtherFile.kt]") } != null) {
                "Unexpected output:\n${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @Test
    fun `Given some code with an error which can be autocorrected then return from from with the normal exit code`() {
        runKtLintCliProcess(
            "too-many-empty-lines",
            listOf("-F"),
        ) {
            assertNormalExitCode()
            // on JDK11+ contains warning about illegal reflective access operation
            // assertErrorOutputIsEmpty()

            assertSourceFileWasFormatted("Main.kt")
        }
    }

    @Test
    fun `Given some code which only contains errors for rules which are disabled via CLI argument --disabled_rules then return from lint with the normal exit code and without error output`() {
        runKtLintCliProcess(
            "too-many-empty-lines",
            listOf("--disabled_rules=no-consecutive-blank-lines,no-empty-first-line-in-method-block"),
        ) {
            assertNormalExitCode()

            assertThat(normalOutput).doesNotContain(
                "no-consecutive-blank-lines",
                "no-empty-first-line-in-method-block",
            )
        }
    }
}
