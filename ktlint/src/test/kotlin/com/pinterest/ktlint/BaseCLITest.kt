package com.pinterest.ktlint

import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
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
        executionAssertions: ExecutionResult.() -> Unit,
    ) {
        val projectPath = prepareTestProject(testProjectName)
        val ktlintCommand =
            arguments.joinToString(prefix = "$ktlintCli ", separator = " ") {
                it.replace(BASE_DIR_PLACEHOLDER, tempDir.toString())
            }
        // Forking in a new shell process, so 'ktlint' will pickup new 'PATH' env variable value
        val pb = ProcessBuilder("/bin/sh", "-c", ktlintCommand)
        pb.directory(projectPath.toAbsolutePath().toFile())

        // Overriding user path to java executable to use java version test is running on
        val environment = pb.environment()
        environment["PATH"] = "${System.getProperty("java.home")}${File.separator}bin${File.pathSeparator}${System.getenv()["PATH"]}"

        val process = pb.start()
        val output = process.inputStream.bufferedReader().use { it.readLines() }
        val error = process.errorStream.bufferedReader().use { it.readLines() }
        process.waitFor(WAIT_TIME_SEC, TimeUnit.SECONDS)

        executionAssertions(ExecutionResult(process.exitValue(), output, error, projectPath))

        process.destroy()
    }

    private fun prepareTestProject(testProjectName: String): Path {
        val testProjectPath = testProjectsPath.resolve(testProjectName)
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

    data class ExecutionResult(
        val exitCode: Int,
        val normalOutput: List<String>,
        val errorOutput: List<String>,
        val testProject: Path,
    ) {
        fun assertNormalExitCode() {
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
        }

        fun assertErrorExitCode() {
            assertThat(exitCode)
                .withFailMessage("Execution was expected to finish with error. However, exitCode is $exitCode")
                .isNotEqualTo(0)
        }

        fun assertErrorOutputIsEmpty() {
            assertThat(errorOutput.isEmpty())
                .withFailMessage(
                    "Expected error output to be empty but was:".followedByIndentedList(errorOutput),
                ).isTrue
        }

        fun assertSourceFileWasFormatted(
            filePathInProject: String,
        ) {
            val originalFile = testProjectsPath.resolve(testProject.last()).resolve(filePathInProject)
            val newFile = testProject.resolve(filePathInProject)

            assert(originalFile.toFile().readText() != newFile.toFile().readText()) {
                "Format did not change source file $filePathInProject content:\n${originalFile.toFile().readText()}"
            }
        }
    }

    companion object {
        private const val WAIT_TIME_SEC = 3L
        val testProjectsPath: Path = Paths.get("src", "test", "resources", "cli")
        const val BASE_DIR_PLACEHOLDER = "__TEMP_DIR__"
    }
}

private fun String.followedByIndentedList(lines: List<String>, indentLevel: Int = 1): String =
    lines
        .ifEmpty { listOf("<empty>") }
        .joinToString(prefix = "$this\n", separator = "\n") {
            "    ".repeat(indentLevel).plus(it)
        }
