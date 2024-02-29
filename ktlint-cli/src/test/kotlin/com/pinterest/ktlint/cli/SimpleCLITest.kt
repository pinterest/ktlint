package com.pinterest.ktlint.cli

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayInputStream
import java.nio.file.Path

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
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        assertThat(normalOutput).containsLineMatching("An anti-bikeshedding Kotlin linter with built-in formatter.")
                        assertThat(normalOutput).containsLineMatching("Usage:")
                        assertThat(normalOutput).containsLineMatching("EXAMPLES")
                    }.assertAll()
            }
    }

    @ParameterizedTest(name = "Options: {0}")
    @ValueSource(
        strings = [
            "-v",
            "--version",
        ],
    )
    fun `Given CLI argument --version then return the version information output`(
        version: String,
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "no-code-style-error",
                listOf(version),
            ) {
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        assertThat(normalOutput).contains("ktlint version ${System.getProperty("ktlint-version")}")
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
                SoftAssertions()
                    .apply {
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
                listOf("**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput).containsLineMatching("Needless blank line(s)")
                    }.assertAll()
            }
    }

    @Test
    fun `Given some code with an error then return from lint with the error exit code and warning to use --format`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching(
                                Regex(".* WARN .* Lint has found errors than can be autocorrected using 'ktlint --format'"),
                            )
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
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        assertThat(normalOutput).containsLineMatching("No files matched [$somePattern]")
                    }.assertAll()
            }
    }

    @Test
    fun `Given some code in a file ending with a default kotlin extension then it will be picked up if no patterns are specified at the command line`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "no-code-style-error",
                // No patterns specified at the command line. As of that it has to pick up all files having a default kotlin extension
                emptyList(),
            ) {
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching("Enable default patterns")
                            .containsLineMatching("1 file(s) scanned / 0 error(s)")
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
                listOf("-F", "**/*.test"),
            ) {
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        // on JDK11+ contains warning about illegal reflective access operation
                        // assertErrorOutputIsEmpty()

                        assertSourceFileWasFormatted("Main.kt.test")
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
                SoftAssertions()
                    .apply {
                        assertNormalExitCode()
                        assertThat(normalOutput).containsLineMatching("No files matched [$somePatternProvidedViaStdin]")
                    }.assertAll()
            }
    }

    @Test
    fun `Issue 1793 - Given some code with an error and no patterns read in from stdin then return nothing `(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("--patterns-from-stdin"),
                stdin = ByteArrayInputStream(ByteArray(0)),
            ) {
                assertNormalExitCode()
                assertThat(normalOutput)
                    .doesNotContainLineMatching("Enable default patterns")
                    .containsLineMatching("No files matched []")
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
                listOf("**/*.test", "--relative", "--reporter=sarif"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(errorOutput).doesNotContainLineMatching(
                            "Exception in thread \"main\" java.lang.IllegalArgumentException: this and base files have different roots:",
                        )
                    }.assertAll()
            }
    }

    @Test
    fun `Issue 2576 - single reporter`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("**/*.test", "--reporter=plain"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching("Main.kt.test:2:1: First line in a method block should not be empty")
                    }.assertAll()
            }
    }

    @Test
    fun `Issue 2576 - multiple reporters`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("**/*.test", "--reporter=plain", "--reporter=json,output=ktlint-violations.json"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching("Initializing \"plain\" reporter")
                            .containsLineMatching(Regex(".*Initializing \"json\" reporter with .*, output=ktlint-violations.json"))
                            .containsLineMatching("Main.kt.test:2:1: First line in a method block should not be empty")
                            .containsLineMatching(Regex(".*ReporterAggregator -- \"json\" report written to .*ktlint-violations.json"))
                    }.assertAll()
            }
    }

    @Test
    fun `Given a custom reporter which does not exist`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("**/*.test", "--reporter=custom,artifact=custom-reporter.jar"),
            ) {
                SoftAssertions()
                    .apply {
                        assertErrorExitCode()
                        assertThat(normalOutput)
                            .containsLineMatching("File 'custom-reporter.jar' does not exist")
                    }.assertAll()
            }
    }

    @Test
    fun `Generate git pre commit hook`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("installGitPreCommitHook"),
            ) {
                SoftAssertions()
                    .apply {
                        // The command will throw an error because the testProjectName directory does not contain a
                        // '.git' directory. This is sufficient to know that the ktlint command was recognized.
                        assertErrorExitCode()
                        assertThat(errorOutput).containsLineMatching(
                            "git directory not found. Are you sure you are inside project directory?",
                        )
                    }.assertAll()
            }
    }

    @Test
    fun `Generate git pre push hook`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("installGitPrePushHook"),
            ) {
                SoftAssertions()
                    .apply {
                        // The command will throw an error because the testProjectName directory does not contain a
                        // '.git' directory. This is sufficient to know that the ktlint command was recognized.
                        assertErrorExitCode()
                        assertThat(errorOutput).containsLineMatching(
                            "git directory not found. Are you sure you are inside project directory?",
                        )
                    }.assertAll()
            }
    }

    @Nested
    inner class `Generate 'editorconfig' file` {
        @Test
        fun `Given that the code-style is specified then exit with error`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("generateEditorConfig", "--code-style ktlint_official"),
                ) {
                    SoftAssertions()
                        .apply {
                            assertNormalExitCode()
                            assertThat(normalOutput).containsLineMatching("ktlint_code_style = ktlint_official")
                        }.assertAll()
                }
        }

        @Test
        fun `Given that the code-style is not specified then the command should fail`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("generateEditorConfig"),
                ) {
                    SoftAssertions()
                        .apply {
                            assertErrorExitCode()
                            assertThat(errorOutput).containsLineMatching("Error: missing option --code-style")
                        }.assertAll()
                }
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

    @Test
    fun `Issue 1832 - Given stdin input containing Kotlin Script resulting in a KtLintParseException when linted as Kotlin code then process the input as Kotlin Script`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                testProjectName = "too-many-empty-lines",
                arguments = listOf("--stdin"),
                stdin =
                    ByteArrayInputStream(
                        """
                        pluginManagement {
                            repositories {
                                mavenCentral()
                                gradlePluginPortal()
                            }


                            includeBuild("build-logic")
                        }
                        """.trimIndent().toByteArray(),
                    ),
            ) {
                assertThat(normalOutput)
                    .containsLineMatching(Regex(".*Not a valid Kotlin file.*"))
                    .containsLineMatching(Regex(".*Now, trying to read the input as Kotlin Script.*"))
                assertThat(errorOutput)
                    .containsLineMatching(Regex(".*Needless blank line.*"))
            }
    }

    @Test
    fun `Issue 1832 - Given stdin input containing Kotlin Script resulting in a KtLintParseException when formatted as Kotlin code then process the input as Kotlin Script`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                testProjectName = "too-many-empty-lines",
                arguments = listOf("--stdin", "--format"),
                stdin =
                    ByteArrayInputStream(
                        """
                        pluginManagement {
                            repositories {
                                mavenCentral()
                                gradlePluginPortal()
                            }


                            includeBuild("build-logic")
                        }
                        """.trimIndent().toByteArray(),
                    ),
            ) {
                assertThat(normalOutput)
                    .containsLineMatching(Regex(".*Not a valid Kotlin file.*"))
                    .containsLineMatching(Regex(".*Now, trying to read the input as Kotlin Script.*"))
                assertThat(errorOutput)
                    .doesNotContainLineMatching(Regex(".*Needless blank line.*"))
            }
    }

    @Test
    fun `Issue 2379 - Given stdin input resulting in a KtLintParseException when formatted as Kotlin Script code`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                testProjectName = "too-many-empty-lines",
                arguments = listOf("--stdin", "--format"),
                stdin =
                    ByteArrayInputStream(
                        """
                        fun foo() =
                        """.trimIndent().toByteArray(),
                    ),
            ) {
                assertThat(errorOutput)
                    .containsLineMatching(Regex(".*Not a valid Kotlin file.*"))
                    .doesNotContainLineMatching(Regex(".*Now, trying to read the input as Kotlin Script.*"))
            }
    }

    @Test
    fun `Given that the deprecated parameter --code-style is specified, then return an error`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                testProjectName = "too-many-empty-lines",
                arguments = listOf("--code-style=android_studio"),
            ) {
                assertThat(errorOutput).containsLineMatching(
                    "Parameter '--code-style' is no longer valid. The code style should be defined as '.editorconfig' property " +
                        "'ktlint_code_style='",
                )
            }
    }
}
