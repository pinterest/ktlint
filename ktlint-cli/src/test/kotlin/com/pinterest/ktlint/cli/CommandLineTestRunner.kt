package com.pinterest.ktlint.cli

import com.pinterest.ktlint.cli.environment.OsEnvironment
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import mu.KotlinLogging
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.Assertions
import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.fail
import java.io.File
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.relativeToOrSelf

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

class CommandLineTestRunner(
    private val tempDir: Path,
) {
    private val ktlintCli: String = System.getProperty("ktlint-cli")

    /**
     * Run ktlint CLI in a separate process. All files in directory [testProjectName] are copied to a temporary
     * directory. Paths in the [arguments] that refer to files in directory [testProjectName] need to be prefixed with
     * the placeholder [BASE_DIR_PLACEHOLDER] to obtain a fully qualified path. During the test execution this
     * placeholder is replaces with the actual directory name that is created for that unit test.
     */
    fun run(
        testProjectName: String,
        arguments: List<String> = emptyList(),
        stdin: InputStream? = null,
        executionAssertions: ExecutionResult.() -> Unit,
    ) {
        val projectPath = prepareTestProject(testProjectName)
        // Forking in a new shell process, so 'ktlint' will pick up new 'PATH' env variable value
        val process =
            ProcessBuilder(*interpreterPathAndArgs(), ktlintCommand(arguments))
                .apply {
                    prependPathWithJavaBinHome()
                    directory(projectPath.toAbsolutePath().toFile())
                }.start()

        if (stdin != null) {
            process.outputStream.use(stdin::copyTo)
        }

        // Give the process some time to complete
        (0..WAIT_INTERVAL_MAX_OCCURRENCES).forEach { _ ->
            if (process.isAlive) {
                // Check regularly whether the ktlint command has finished to speed up the unit testing
                process.waitFor(WAIT_INTERVAL_DURATION, TimeUnit.MILLISECONDS)
            }
        }

        // Get the output from the process before destroying it as otherwise the streams are not collected completely
        val output = process.inputStream.bufferedReader().use { it.readLines() }
        val error = process.errorStream.bufferedReader().use { it.readLines() }

        if (process.isAlive) {
            // Destroy the process as it is still running
            process.destroyForcibly()

            // For unknown reasons the process sometimes results in a timeout although the ktlint command is terminated when the last line
            // of the output contains the debug line containing the exit code. If this line is found, consider it as a normal termination.
            val exitCode =
                output
                    .lastOrNull { it.contains(EXIT_KTLINT_WITH_EXIT_CODE_DEBUG_LINE) }
                    ?.substringAfter(EXIT_KTLINT_WITH_EXIT_CODE_DEBUG_LINE)
                    ?.toInt()
            if (exitCode != null) {
                executionAssertions(ExecutionResult(exitCode, output, error, projectPath))
            } else {
                // Ktlint is either not terminated or is terminated with a non-zero exit code.
                val maxDurationInSeconds = (WAIT_INTERVAL_DURATION * WAIT_INTERVAL_MAX_OCCURRENCES).div(1000.0)
                fail {
                    "CLI test has been aborted as it could not be completed in $maxDurationInSeconds seconds"
                        .followedByIndentedList(
                            listOf(
                                "RESULTS OF STDOUT:".followedByIndentedList(output, 2),
                                "RESULTS OF STDERR:".followedByIndentedList(error, 2),
                            ),
                        )
                }
            }
        } else {
            executionAssertions(ExecutionResult(process.exitValue(), output, error, projectPath))

            // Only destroy the process when it is no longer needed (so after asserting)
            process.destroy()
        }
    }

    private fun prepareTestProject(testProjectName: String): Path {
        val testProjectPath = TEST_PROJECTS_PATHS.resolve(testProjectName)
        assert(Files.exists(testProjectPath)) {
            "Test project $testProjectName does not exist!"
        }

        return tempDir.resolve(testProjectName).also { testProjectPath.copyRecursively(it) }
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows")

    /**
     * @return the path to the command interpreter along with the necessary
     *   arguments.
     */
    private fun interpreterPathAndArgs(): Array<String> =
        when {
            isWindows() -> {
                val environment = OsEnvironment()

                /*
                 * Sometimes Windows has ComSpec undefined, so we need to
                 * provide a default.
                 */
                val comSpec = environment["ComSpec"] ?: "cmd.exe"
                arrayOf(comSpec, "/C")
            }

            else -> arrayOf("/bin/sh", "-c")
        }

    private fun ktlintCommand(arguments: List<String>): String =
        mutableListOf<String>()
            .apply {
                if (isWindows()) {
                    // KtLint is not an executable command on Windows OS
                    add("java")

                    val javaVersion = System.getProperty("java.specification.version").javaVersionAsInt()
                    if (javaVersion != null) {
                        // https://docs.gradle.org/7.5/userguide/upgrading_version_7.html#removes_implicit_add_opens_for_test_workers
                        if (javaVersion >= 16) {
                            add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                        }
                    }
                    add("-jar")
                }

                add(ktlintCli)

                // Always run with debug logging as this is convenient when test fails and when ktlint is finished it
                // prints the log line "Finished in ###ms / ... " which is used as fallback to determine whether ktlint
                // did finish correctly.
                add("-l=trace")

                addAll(arguments)
            }.joinToString(separator = " ")
            .also { LOGGER.debug("Command to be executed: $it") }

    private fun String?.javaVersionAsInt(): Int? {
        if (this == null) {
            return null
        }

        val matchResult =
            JAVA_VERSION_REGEX
                .matchEntire(this)
                ?: // Java 9+ has no leading `1.` as prefix in the version number
                return toIntOrNull()

        val matchGroup =
            matchResult
                .groups["version"]
                ?: return null

        return matchGroup.value.toIntOrNull()
    }

    private fun ProcessBuilder.prependPathWithJavaBinHome() {
        val environment = environment()
        val pathKey =
            when {
                isWindows() -> {
                    // On Windows, environment keys are case-insensitive, which is not handled by the JVM
                    environment.keys.firstOrNull { key ->
                        key.equals(PATH, ignoreCase = true)
                    } ?: PATH
                }
                else -> PATH
            }
        environment[pathKey] = "$JAVA_HOME_BIN_DIR${File.pathSeparator}${OsEnvironment()[PATH]}"
    }

    private fun Path.copyRecursively(dest: Path) {
        Files.walkFileTree(
            this,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    val relativeDir = dir.relativeToOrSelf(this@copyRecursively)
                    dest.resolve(relativeDir).createDirectories()
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    val relativeFile = file.relativeToOrSelf(this@copyRecursively)
                    val destinationFile = dest.resolve(relativeFile)
                    LOGGER.trace { "Copy '$relativeFile' to '$destinationFile'" }
                    file.copyTo(destinationFile)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    data class ExecutionResult(
        val exitCode: Int,
        val normalOutput: List<String>,
        val errorOutput: List<String>,
        val testProject: Path,
    ) {
        fun assertNormalExitCode(): AbstractIntegerAssert<*> =
            Assertions.assertThat(exitCode)
                .withFailMessage(
                    "Expected process to exit with exitCode 0, but was $exitCode."
                        .followedByIndentedList(
                            listOf(
                                "RESULTS OF STDOUT:".followedByIndentedList(normalOutput, 2),
                                "RESULTS OF STDERR:".followedByIndentedList(errorOutput, 2),
                            ),
                        ),
                ).isEqualTo(0)

        fun assertErrorExitCode(): AbstractIntegerAssert<*> =
            Assertions.assertThat(exitCode)
                .withFailMessage("Execution was expected to finish with error. However, exitCode is $exitCode")
                .isNotEqualTo(0)

        fun assertErrorOutputIsEmpty(): AbstractBooleanAssert<*> =
            Assertions.assertThat(errorOutput.isEmpty())
                .withFailMessage(
                    "Expected error output to be empty but was:".followedByIndentedList(errorOutput),
                ).isTrue

        fun assertSourceFileWasFormatted(filePathInProject: String): AbstractAssert<*, *> {
            val originalCode =
                TEST_PROJECTS_PATHS
                    .resolve(testProject.last())
                    .resolve(filePathInProject)
                    .toFile()
                    .readText()
            val formattedCode =
                testProject
                    .resolve(filePathInProject)
                    .toFile()
                    .readText()

            return Assertions.assertThat(formattedCode).isNotEqualTo(originalCode)
        }
    }

    companion object {
        private const val WAIT_INTERVAL_DURATION = 100L
        private val WAIT_INTERVAL_MAX_OCCURRENCES =
            // The local machine is most often faster than the Github CICD when running the CLI tests. A system environment property has
            // been defined in the gradle-pr-build workflow to increase the timeout.
            System
                .getenv("CLI_TEST_MAX_DURATION_IN_SECONDS")
                ?.toIntOrNull()
                ?.let { it * 1000 / WAIT_INTERVAL_DURATION }
                ?: 10 // Default applies to local machine on which the system environment property most likely is not set
        private val TEST_PROJECTS_PATHS: Path = Path("src", "test", "resources", "cli")
        private const val PATH = "PATH"
        private val JAVA_VERSION_REGEX = Regex("""^1\.(?<version>\d+)(?:\.[^.].*)?$""")
        private const val EXIT_KTLINT_WITH_EXIT_CODE_DEBUG_LINE = "Exit ktlint with exit code: "

        // Path to java bin directory on which tests will be executed
        private val JAVA_HOME_BIN_DIR = Path(System.getProperty("java.home")).resolve("bin")
    }
}

@Suppress("unused")
private fun String.followedByIndentedList(
    lines: List<String>,
    indentLevel: Int = 1,
): String =
    lines
        .ifEmpty { listOf("<empty>") }
        .joinToString(prefix = "$this\n", separator = "\n") {
            "    ".repeat(indentLevel).plus(it)
        }

@Suppress("unused")
internal fun ListAssert<String>.containsLineMatching(string: String): ListAssert<String> =
    this.anyMatch {
        it.contains(string)
    }

@Suppress("unused")
internal fun ListAssert<String>.containsLineMatching(regex: Regex): ListAssert<String> =
    this.anyMatch {
        it.matches(regex)
    }

@Suppress("unused")
internal fun ListAssert<String>.doesNotContainLineMatching(string: String): ListAssert<String> =
    this.noneMatch {
        it.contains(string)
    }

@Suppress("unused")
internal fun ListAssert<String>.doesNotContainLineMatching(regex: Regex): ListAssert<String> =
    this.noneMatch {
        it.matches(regex)
    }
