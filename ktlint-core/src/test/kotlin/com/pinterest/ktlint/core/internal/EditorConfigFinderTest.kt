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
    @Nested
    inner class FindByFile {
        @Test
        fun `Given a kotlin file in a subdirectory and a root-editorconfig file in the same directory then get the path of that editorconfig file`(
            @TempDir
            tempDir: Path,
        ) {
            val someSubDir = "some-project/src/main/kotlin"
            tempDir.createFile("$someSubDir/.editorconfig", "root=true")
            val kotlinFilePath = tempDir.createFile("$someSubDir/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

            assertThat(actual).contains("$someSubDir/.editorconfig")
        }

        @Test
        fun `Given a kotlin file in a subdirectory and a root-editorconfig file in a parent directory then get the path of that parent editorconfig file`(
            @TempDir
            tempDir: Path,
        ) {
            val someProjectDirectory = "some-project"
            tempDir.createFile("$someProjectDirectory/.editorconfig", "root=true")
            val kotlinFilePath = tempDir.createFile("$someProjectDirectory/src/main/kotlin/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

            assertThat(actual).contains("$someProjectDirectory/.editorconfig")
        }

        @Test
        fun `Given a kotlin file in a subdirectory and a non-root-editorconfig file in that same directory and a root-editorconfig file in a parent directory then get the paths of both editorconfig files`(
            @TempDir
            tempDir: Path,
        ) {
            val someProjectDirectory = "some-project"
            tempDir.createFile("$someProjectDirectory/.editorconfig", "root=true")
            tempDir.createFile("$someProjectDirectory/src/main/.editorconfig", "root=false")
            val kotlinFilePath = tempDir.createFile("$someProjectDirectory/src/main/kotlin/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

            assertThat(actual).contains(
                "$someProjectDirectory/.editorconfig",
                "$someProjectDirectory/src/main/.editorconfig",
            )
        }

        @Test
        fun `Given a kotlin file in a subdirectory and a root-editorconfig file in the parent directory and another root-editorconfig file in a great-parent directory then get the paths of editorconfig files excluding root-editorconfig once the first one is found`(
            @TempDir
            tempDir: Path,
        ) {
            val someProjectDirectory = "some-project"
            tempDir.createFile("$someProjectDirectory/src/main/.editorconfig", "root=false")
            tempDir.createFile("$someProjectDirectory/src/.editorconfig", "root=true")
            tempDir.createFile("$someProjectDirectory/.editorconfig", "root=true")
            val kotlinFilePath = tempDir.createFile("$someProjectDirectory/src/main/kotlin/test.kt", "val foo = 42")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(kotlinFilePath)
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

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
        fun `Given a directory containing a root-editorconfig file and a subdirectory containing a editorconfig file then get the paths of both editorconfig files`(
            @TempDir
            tempDir: Path,
        ) {
            val someDirectory = "some-project"
            tempDir.createFile("$someDirectory/.editorconfig", "root=true")
            tempDir.createFile("$someDirectory/src/main/kotlin/.editorconfig", "some-property=some-value")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(tempDir.plus(someDirectory))
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

            assertThat(actual).contains(
                "$someDirectory/.editorconfig",
                "$someDirectory/src/main/kotlin/.editorconfig",
            )
        }

        @Test
        fun `Given a subdirectory containing an editorconfig file and a sibling subdirectory contain a editorconfig file in a parent directory then get the path of all editorconfig file except of the sibling subdirectory`(
            @TempDir
            tempDir: Path,
        ) {
            val someProjectDirectory = "some-project"
            tempDir.createFile("$someProjectDirectory/.editorconfig", "root=true")
            tempDir.createFile("$someProjectDirectory/src/main/kotlin/.editorconfig", "some-property=some-value")
            tempDir.createFile("$someProjectDirectory/src/test/kotlin/.editorconfig", "some-property=some-value")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(tempDir.plus("$someProjectDirectory/src/main/kotlin"))
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

            assertThat(actual)
                .contains(
                    "$someProjectDirectory/.editorconfig",
                    "$someProjectDirectory/src/main/kotlin/.editorconfig",
                ).doesNotContain(
                    "$someProjectDirectory/src/test/kotlin/.editorconfig",
                )
        }

        @Test
        fun `Given a directory containing an editorconfig file and multiple subdirectores containing a editorconfig file then get the path of all editorconfig files`(
            @TempDir
            tempDir: Path,
        ) {
            val someProjectDirectory = "some-project"
            tempDir.createFile("$someProjectDirectory/.editorconfig", "root=true")
            tempDir.createFile("$someProjectDirectory/src/main/kotlin/.editorconfig", "some-property=some-value")
            tempDir.createFile("$someProjectDirectory/src/test/kotlin/.editorconfig", "some-property=some-value")

            val actual =
                EditorConfigFinder()
                    .findEditorConfigs(tempDir.plus(someProjectDirectory))
                    .mapNotNull { it.toRelativePathStringIn(tempDir) }

            assertThat(actual).contains(
                "$someProjectDirectory/.editorconfig",
                "$someProjectDirectory/src/main/kotlin/.editorconfig",
                "$someProjectDirectory/src/test/kotlin/.editorconfig",
            )
        }
    }

    private fun Path.createFile(fileName: String, content: String): Path {
        val dirPath = fileName.substringBeforeLast("/", "")
        Files.createDirectories(this.plus(dirPath))
        return Files
            .createFile(this.plus(fileName))
            .also { it.writeText(content) }
    }

    private fun Path.plus(subPath: String): Path =
        this
            .absolutePathString()
            .plus(File.separator)
            .plus(subPath)
            .let { Paths.get(it) }

    private fun Path.toRelativePathStringIn(tempDir: Path): String? =
        this
            .absolutePathString()
            .takeIf {
                // Ignore files created in temp dirs of other tests
                it.startsWith(tempDir.absolutePathString())
            }?.removePrefix(tempDir.absolutePathString())
            ?.removePrefix(File.separator)
            ?.replace(tempDir.fileSystem.separator, "/")
}
