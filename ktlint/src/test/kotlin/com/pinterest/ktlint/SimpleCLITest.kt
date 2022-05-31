package com.pinterest.ktlint

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@DisabledOnOs(OS.WINDOWS)
@DisplayName("CLI basic checks")
class SimpleCLITest : BaseCLITest() {

    @DisplayName("Should print help")
    @Test
    fun shouldOutputHelp() {
        runKtLintCliProcess(
            "no-code-style-error",
            listOf("--help")
        ) {
            assertNormalExitCode()
            assertErrorOutputIsEmpty()

            assert(normalOutput.contains("Usage:") && normalOutput.contains("Examples:")) {
                "Did not produced help output!\n ${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @DisplayName("Should print correct version")
    @Test
    fun shouldCorrectlyPrintVersion() {
        runKtLintCliProcess(
            "no-code-style-error",
            listOf("--version")
        ) {
            assertNormalExitCode()
            assertErrorOutputIsEmpty()

            val expectedVersion = System.getProperty("ktlint-version")
            assert(normalOutput.contains(expectedVersion)) {
                "Output did not contain expected $expectedVersion version:\n ${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @DisplayName("Should complete lint without errors")
    @Test
    internal fun lintWithoutErrors() {
        runKtLintCliProcess(
            "no-code-style-error"
        ) {
            assertNormalExitCode()
            assertErrorOutputIsEmpty()
        }
    }

    @DisplayName("Should complete lint with error")
    @Test
    internal fun lintWithError() {
        runKtLintCliProcess(
            "too-many-empty-lines"
        ) {
            assertErrorExitCode()

            assert(normalOutput.find { it.contains("Needless blank line(s)") } != null) {
                "Unexpected output:\n${normalOutput.joinToString(separator = "\n")}"
            }
        }
    }

    @DisplayName("Should format without errors")
    @Test
    fun formatWorks() {
        runKtLintCliProcess(
            "too-many-empty-lines",
            listOf("-F")
        ) {
            assertNormalExitCode()
            // on JDK11+ contains warning about illegal reflective access operation
            // assertErrorOutputIsEmpty()

            assertSourceFileWasFormatted("Main.kt")
        }
    }
}
