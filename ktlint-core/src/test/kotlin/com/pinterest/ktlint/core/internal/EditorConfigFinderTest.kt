package com.pinterest.ktlint.core.internal

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.konan.file.File
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class EditorConfigFinderTest {
    @TempDir
    private lateinit var tempDir: Path

    @Nested
    inner class FindByFile {
        @Test
        fun `Given a kotlin file in a subdirectory and a root-editorconfig file in the same directory then get the path of that editorconfig file`() {
            val someSubDir = "some-project/src/main/kotlin"
            createFile("$someSubDir/.editorconfig", "root=true")
            val kotlinFilePath = createFile("$someSubDir/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual).contains("$someSubDir/.editorconfig")
        }

        @Test
        fun `Given a kotlin file in a subdirectory and a root-editorconfig file in a parent directory then get the path of that parent editorconfig file`() {
            val someProjectDirectory = "some-project"
            createFile("$someProjectDirectory/.editorconfig", "root=true")
            val kotlinFilePath = createFile("$someProjectDirectory/src/main/kotlin/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual).contains("$someProjectDirectory/.editorconfig")
        }

        @Test
        fun `Given a kotlin file in a subdirectory and a non-root-editorconfig file in that same directory and a root-editorconfig file in a parent directory then get the paths of both editorconfig files`() {
            val someProjectDirectory = "some-project"
            createFile("$someProjectDirectory/.editorconfig", "root=true")
            createFile("$someProjectDirectory/src/main/.editorconfig", "root=false")
            val kotlinFilePath = createFile("$someProjectDirectory/src/main/kotlin/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual).contains(
                "$someProjectDirectory/.editorconfig",
                "$someProjectDirectory/src/main/.editorconfig",
            )
        }

        @Test
        fun `Given a kotlin file in a subdirectory and a root-editorconfig file in the parent directory and another root-editorconfig file in a great-parent directory then get the paths of editorconfig files excluding root-editorconfig once the first one is found`() {
            val someProjectDirectory = "some-project"
            createFile("$someProjectDirectory/src/main/.editorconfig", "root=false")
            createFile("$someProjectDirectory/src/.editorconfig", "root=true")
            createFile("$someProjectDirectory/.editorconfig", "root=true")
            val kotlinFilePath = createFile("$someProjectDirectory/src/main/kotlin/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual)
                .contains(
                    "$someProjectDirectory/src/main/.editorconfig",
                    "$someProjectDirectory/src/.editorconfig",
                ).doesNotContain(
                    "$someProjectDirectory/.editorconfig",
                )
        }
    }

    @Nested
    inner class FindByDirectory {
        @Test
        fun `Given a directory containing a root-editorconfig file and a subdirectory containing a editorconfig file then get the paths of both editorconfig files`() {
            val someDirectory = "some-project"
            createFile("$someDirectory/.editorconfig", "root=true")
            createFile("$someDirectory/src/main/kotlin/.editorconfig", "some-property=some-value")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(toTempDirPath(someDirectory))
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual).contains(
                "$someDirectory/.editorconfig",
                "$someDirectory/src/main/kotlin/.editorconfig",
            )
        }

        @Test
        fun `Given a subdirectory containing an editorconfig file and a sibling subdirectory contain a editorconfig file in a parent directory then get the path of all editorconfig file except of the sibling subdirectory`() {
            val someProjectDirectory = "some-project"
            createFile("$someProjectDirectory/.editorconfig", "root=true")
            createFile("$someProjectDirectory/src/main/kotlin/.editorconfig", "some-property=some-value")
            createFile("$someProjectDirectory/src/test/kotlin/.editorconfig", "some-property=some-value")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(toTempDirPath("$someProjectDirectory/src/main/kotlin"))
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual)
                .contains(
                    "$someProjectDirectory/.editorconfig",
                    "$someProjectDirectory/src/main/kotlin/.editorconfig",
                ).doesNotContain(
                    "$someProjectDirectory/src/test/kotlin/.editorconfig",
                )
        }

        @Test
        fun `Given a directory containing an editorconfig file and multiple subdirectores containing a editorconfig file then get the path of all editorconfig files`() {
            val someProjectDirectory = "some-project"
            createFile("$someProjectDirectory/.editorconfig", "root=true")
            createFile("$someProjectDirectory/src/main/kotlin/.editorconfig", "some-property=some-value")
            createFile("$someProjectDirectory/src/test/kotlin/.editorconfig", "some-property=some-value")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(toTempDirPath("$someProjectDirectory"))
                    .map { it.toPathStringWithoutTempDirPrefix() }

            assertThat(actual).contains(
                "$someProjectDirectory/.editorconfig",
                "$someProjectDirectory/src/main/kotlin/.editorconfig",
                "$someProjectDirectory/src/test/kotlin/.editorconfig",
            )
        }
    }

    private fun createFile(fileName: String, content: String): Path {
        val dirPath = fileName.substringBeforeLast("/", "")
        Files.createDirectories(toTempDirPath(dirPath))
        return Files
            .createFile(toTempDirPath(fileName))
            .also { it.writeText(content) }
    }

    private fun toTempDirPath(subPath: String): Path =
        tempDir
            .absolutePathString()
            .plus(File.separator)
            .plus(subPath)
            .let { Paths.get(it) }

    private fun Path.toPathStringWithoutTempDirPrefix() =
        absolutePathString()
            .removePrefix(tempDir.absolutePathString())
            .removePrefix(File.separator)
            .replace(tempDir.fileSystem.separator, "/")
}
