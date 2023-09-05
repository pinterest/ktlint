package com.pinterest.ktlint.cli

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
                SoftAssertions()
                    .apply {
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
    fun `Given some code which only contains errors for rules which are disabled via CLI argument --disabled_rules then return from lint with the normal exit code and without error output`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                "too-many-empty-lines",
                listOf("--disabled_rules=no-consecutive-blank-lines,no-empty-first-line-in-method-block"),
            ) {
                SoftAssertions()
                    .apply {
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

    @Nested
    inner class `Generate git pre commit hook` {
        @Test
        fun `Given that the code-style option is specified before the command`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("--code-style=ktlint_official", "installGitPreCommitHook"),
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
        fun `Given that the code-style option is specified after the command`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("installGitPreCommitHook", "--code-style=ktlint_official"),
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
        fun `Given that no code-style option is specified then the command should fail`(
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
                            assertErrorExitCode()
                            assertThat(errorOutput).containsLineMatching(
                                "Option --code-style must be set as to generate the git pre commit hook correctly",
                            )
                        }.assertAll()
                }
        }
    }

    @Nested
    inner class `Generate git pre push hook` {
        @Test
        fun `Given that the code-style option is specified before the command`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("--code-style=ktlint_official", "installGitPrePushHook"),
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
        fun `Given that the code-style option is specified after the command`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("installGitPrePushHook", "--code-style=ktlint_official"),
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
        fun `Given that no code-style option is specified then the command should fail`(
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
                            assertErrorExitCode()
                            assertThat(errorOutput).containsLineMatching(
                                "Option --code-style must be set as to generate the git pre push hook correctly",
                            )
                        }.assertAll()
                }
        }
    }

    @Nested
    inner class `Generate 'editorconfig' file` {
        @Test
        fun `Given that the code-style option is specified before the command`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("--code-style=intellij_idea", "generateEditorConfig"),
                ) {
                    SoftAssertions()
                        .apply {
                            assertNormalExitCode()
                            assertThat(normalOutput).containsLineMatching("ktlint_code_style = intellij_idea")
                        }.assertAll()
                }
        }

        @Test
        fun `Given that the code-style option is specified after the command`(
            @TempDir
            tempDir: Path,
        ) {
            CommandLineTestRunner(tempDir)
                .run(
                    "too-many-empty-lines",
                    listOf("generateEditorConfig", "--code-style=ktlint_official"),
                ) {
                    SoftAssertions()
                        .apply {
                            assertNormalExitCode()
                            assertThat(normalOutput).containsLineMatching("ktlint_code_style = ktlint_official")
                        }.assertAll()
                }
        }

        @Test
        fun `Given that no code-style option is specified then the command should fail`(
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
                            assertThat(errorOutput).containsLineMatching(
                                "Option --code-style must be set as to generate the '.editorconfig' correctly",
                            )
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
    fun `Enable android code style via parameter --code-style=android_studio`(
        @TempDir
        tempDir: Path,
    ) {
        CommandLineTestRunner(tempDir)
            .run(
                testProjectName = "too-many-empty-lines",
                arguments = listOf("--code-style=android_studio"),
            ) {
                assertThat(normalOutput).containsLineMatching(
                    Regex(
                        ".*WARN.*Parameter `--code-style=android_studio is deprecated. The code style should be defined as " +
                            "'.editorconfig' property 'ktlint_code_style'.*",
                    ),
                )
            }
    }
}
