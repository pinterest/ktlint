package com.pinterest.ktlint

import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.io.TempDir

abstract class BaseCLITest {
    private val ktlintCli: String = System.getProperty("ktlint-cli")

    @TempDir
    private lateinit var tempDir: Path

    fun runKtLintCliProcess(
        testProjectName: String,
        arguments: List<String> = emptyList(),
        executionAssertions: ExecutionResult.() -> Unit
    ) {
        val projectPath = prepareTestProject(testProjectName)
        val ktlintCommand = "$ktlintCli ${arguments.joinToString()}"
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
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    Files.createDirectories(dest.resolve(relativize(dir)))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    Files.copy(file, dest.resolve(relativize(file)))
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    data class ExecutionResult(
        val exitCode: Int,
        val normalOutput: List<String>,
        val errorOutput: List<String>,
        val testProject: Path
    ) {
        fun assertNormalExitCode() {
            assert(exitCode == 0) {
                "Execution was not finished normally: $exitCode"
            }
        }

        fun assertErrorExitCode() {
            assert(exitCode == 1) {
                "Execution was finished without error: $exitCode"
            }
        }

        fun assertErrorOutputIsEmpty() {
            assert(errorOutput.isEmpty()) {
                "Error output contains following lines:\n${errorOutput.joinToString(separator = "\n")}"
            }
        }

        fun assertSourceFileWasFormatted(
            filePathInProject: String
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
    }
}
