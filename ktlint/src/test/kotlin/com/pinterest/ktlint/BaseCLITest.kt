package com.pinterest.ktlint

import java.io.File
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir

abstract class BaseCLITest {
    private val ktlintCli: String = System.getProperty("ktlint-cli")

    @TempDir
    private lateinit var tempDir: Path

    /**
     * Run ktlint CLI in a separate process. All files in directory [testProjectName] are copied to a temporary
     * directory. Paths in the [arguments] that refer to files in directory [testProjectName] need to be prefixed with
     * the placeholder [BASE_DIR_PLACEHOLDER] to obtain a fully qualified path. During the test execution this
     * placeholder is replaces with the actual directory name that is created for that unit test.
     */
    fun runKtLintCliProcess(
        testProjectName: String,
        arguments: List<String> = emptyList(),
        stdin: InputStream? = null,
        executionAssertions: ExecutionResult.() -> Unit,
    ) {
        val projectPath = prepareTestProject(testProjectName)
        val ktlintCommand =
            arguments.joinToString(prefix = "$ktlintCli ", separator = " ") {
                it.replace(BASE_DIR_PLACEHOLDER, tempDir.toString())
            }
        // Forking in a new shell process, so 'ktlint' will pickup new 'PATH' env variable value
        val processBuilder = ProcessBuilder("/bin/sh", "-c", ktlintCommand)
        processBuilder.directory(projectPath.toAbsolutePath().toFile())

        // Overriding user path to java executable to use java version test is running on
        val environment = processBuilder.environment()
        environment["PATH"] = "${System.getProperty("java.home")}${File.separator}bin${File.pathSeparator}${System.getenv()["PATH"]}"

        val process = processBuilder.start()

        if (stdin != null) {
            process.outputStream.use(stdin::copyTo)
        }

        if (process.completedInAllowedDuration()) {
            val output = process.inputStream.bufferedReader().use { it.readLines() }
            val error = process.errorStream.bufferedReader().use { it.readLines() }

            executionAssertions(ExecutionResult(process.exitValue(), output, error, projectPath))

            // Destroy process only after output is collected as other the streams are not completed.
            process.destroy()
        } else {
            // Destroy before failing the test as the process otherwise keeps running
            process.destroyForcibly()

            val maxDurationInSeconds = (WAIT_INTERVAL_DURATION * WAIT_INTERVAL_MAX_OCCURRENCES).div(1000.0)
            fail {
                "CLI test has been aborted as it could not be completed in $maxDurationInSeconds seconds"
            }
        }
    }

    private fun Process.completedInAllowedDuration(): Boolean {
        (0..WAIT_INTERVAL_MAX_OCCURRENCES).forEach { _ ->
            if (isAlive) {
                waitFor(WAIT_INTERVAL_DURATION, TimeUnit.MILLISECONDS)
            } else {
                return true
            }
        }
        return false
    }

    private fun prepareTestProject(testProjectName: String): Path {
        val testProjectPath = TEST_PROJECTS_PATHS.resolve(testProjectName)
        assert(Files.exists(testProjectPath)) {
            "Test project $testProjectName does not exist!"
        }

        return tempDir.resolve(testProjectName).also { testProjectPath.copyRecursively(it) }
    }

    private fun Path.copyRecursively(dest: Path) {
        Files.walkFileTree(
            this,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.createDirectories(dest.resolve(relativize(dir)))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.copy(file, dest.resolve(relativize(file)))
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    protected fun ListAssert<String>.containsLineMatching(string: String): ListAssert<String> =
        this.anyMatch {
            it.contains(string)
        }

    protected fun ListAssert<String>.containsLineMatching(regex: Regex): ListAssert<String> =
        this.anyMatch {
            it.matches(regex)
        }

    protected fun ListAssert<String>.doesNotContainLineMatching(string: String): ListAssert<String> =
        this.noneMatch {
            it.contains(string)
        }

    protected fun ListAssert<String>.doesNotContainLineMatching(regex: Regex): ListAssert<String> =
        this.noneMatch {
            it.matches(regex)
        }

    data class ExecutionResult(
        val exitCode: Int,
        val normalOutput: List<String>,
        val errorOutput: List<String>,
        val testProject: Path,
    ) {
        fun assertNormalExitCode(): AbstractIntegerAssert<*> =
            assertThat(exitCode)
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
            assertThat(exitCode)
                .withFailMessage("Execution was expected to finish with error. However, exitCode is $exitCode")
                .isNotEqualTo(0)

        fun assertErrorOutputIsEmpty(): AbstractBooleanAssert<*> =
            assertThat(errorOutput.isEmpty())
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

            return assertThat(formattedCode).isNotEqualTo(originalCode)
        }
    }

    companion object {
        private const val WAIT_INTERVAL_DURATION = 100L
        private const val WAIT_INTERVAL_MAX_OCCURRENCES = 1000
        val TEST_PROJECTS_PATHS: Path = Paths.get("src", "test", "resources", "cli")
        const val BASE_DIR_PLACEHOLDER = "__TEMP_DIR__"
    }
}

private fun String.followedByIndentedList(lines: List<String>, indentLevel: Int = 1): String =
    lines
        .ifEmpty { listOf("<empty>") }
        .joinToString(prefix = "$this\n", separator = "\n") {
            "    ".repeat(indentLevel).plus(it)
        }
