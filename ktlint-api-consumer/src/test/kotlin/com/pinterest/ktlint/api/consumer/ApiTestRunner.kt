package com.pinterest.ktlint.api.consumer

import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.relativeToOrSelf
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

class ApiTestRunner(private val tempDir: Path) {
    fun prepareTestProject(testProjectName: String): Path {
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

    companion object {
        private val TEST_PROJECTS_PATHS: Path = Path("src", "test", "resources", "api")
    }
}
