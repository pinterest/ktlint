package com.pinterest.ktlint.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for [fileSequence] method.
 */
internal class FileUtilsFileSequenceTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())

    private val rootDir = tempFileSystem.rootDirectories.first().toString()
    private val javaFileRootDirectory = "${rootDir}Root.java"
    private val ktFileRootDirectory = "${rootDir}Root.kt"
    private val ktsFileRootDirectory = "${rootDir}Root.kts"
    private val javaFileInHiddenDirectory = "${rootDir}project1/.git/Ignored.java"
    private val ktFileInHiddenDirectory = "${rootDir}project1/.git/Ignored.kt"
    private val ktsFileInHiddenDirectory = "${rootDir}project1/.git/Ignored.kts"
    private val javaFileInProjectRootDirectory = "${rootDir}project1/ProjectRoot.java"
    private val ktFileInProjectRootDirectory = "${rootDir}project1/ProjectRoot.kt"
    private val ktsFileInProjectRootDirectory = "${rootDir}project1/ProjectRoot.kts"
    private val ktFile1InProjectSubDirectory = "${rootDir}project1/src/main/kotlin/One.kt"
    private val ktFile2InProjectSubDirectory = "${rootDir}project1/src/main/kotlin/example/Two.kt"
    private val ktsFileInProjectSubDirectory = "${rootDir}project1/src/scripts/Script.kts"
    private val javaFileInProjectSubDirectory = "${rootDir}project1/src/main/java/One.java"

    @BeforeEach
    internal fun setUp() {
        tempFileSystem.apply {
            createFile(javaFileRootDirectory)
            createFile(ktFileRootDirectory)
            createFile(ktsFileRootDirectory)
            createFile(javaFileInHiddenDirectory)
            createFile(ktFileInHiddenDirectory)
            createFile(ktsFileInHiddenDirectory)
            createFile(javaFileInProjectRootDirectory)
            createFile(ktFileInProjectRootDirectory)
            createFile(ktsFileInProjectRootDirectory)
            createFile(ktsFileInProjectSubDirectory)
            createFile(ktFile1InProjectSubDirectory)
            createFile(ktFile2InProjectSubDirectory)
            createFile(javaFileInProjectSubDirectory)
        }
    }

    @AfterEach
    internal fun tearDown() {
        tempFileSystem.close()
    }

    @Test
    fun `Given no patterns and no workdir then find all kt and kts files in root and all its sub directories except file in hidden directories`() {
        val foundFiles = getFiles()

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileRootDirectory,
                ktsFileRootDirectory,
                ktFileInProjectRootDirectory,
                ktsFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
                ktsFileInProjectSubDirectory
            ).doesNotContain(
                javaFileInHiddenDirectory,
                ktFileInHiddenDirectory,
                ktsFileInHiddenDirectory
            )
    }

    @Test
    fun `Given some patterns and no workdir then ignore all files in hidden directories`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "project1/**/*.kt".normalizeGlob(),
                "project1/*.kt".normalizeGlob()
            )
        )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory
            ).doesNotContain(
                javaFileInHiddenDirectory,
                ktFileInHiddenDirectory,
                ktsFileInHiddenDirectory
            )
    }

    @Nested
    inner class NegatePattern {
        @Test
        fun `Given some patterns including a negate pattern and no workdir then select all files except files in the negate pattern`() {
            val foundFiles = getFiles(
                patterns = listOf(
                    "project1/src/**/*.kt".normalizeGlob(),
                    "!project1/src/**/example/*.kt".normalizeGlob()
                )
            )

            assertThat(foundFiles)
                .containsExactlyInAnyOrder(ktFile1InProjectSubDirectory)
                .doesNotContain(ktFile2InProjectSubDirectory)
        }

        @Test
        fun `Given the Windows OS and some unescaped patterns including a negate pattern and no workdir then ignore all files in the negate pattern`() {
            assumeTrue(
                System
                    .getProperty("os.name")
                    .lowercase(Locale.getDefault())
                    .startsWith("windows")
            )

            val foundFiles = getFiles(
                patterns = listOf(
                    "project1\\src\\**\\*.kt".normalizeGlob(),
                    "!project1\\src\\**\\example\\*.kt".normalizeGlob()
                )
            )

            assertThat(foundFiles)
                .containsExactlyInAnyOrder(ktFile1InProjectSubDirectory)
                .doesNotContain(ktFile2InProjectSubDirectory)
        }
    }

    @Test
    fun `Given a pattern and a workdir then find all files in that workdir and all its sub directories that match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "**/main/**/*.kt".normalizeGlob()
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory
        )
    }

    @Test
    fun `Given an (relative) file path from the workdir then find all files in that workdir and all its sub directories that match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf("src/main/kotlin/One.kt".normalizeGlob()),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory
        )
    }

    @Test
    fun `Given an (absolute) file path and a workdir then find that absolute path and all files in the workdir and all its sub directories that match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "src/main/kotlin/One.kt".normalizeGlob(),
                ktFile2InProjectSubDirectory.normalizeGlob()
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory
        )
    }

    @Test
    fun `Given a glob containing an (absolute) file path and a workdir then find all files match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "${rootDir}project1/src/**/*.kt".normalizeGlob()
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory
        )
    }

    @Test
    fun `transforming globs with leading tilde`() {
        assumeTrue(
            System
                .getProperty("os.name")
                .lowercase(Locale.getDefault())
                .startsWith("linux")
        )

        val glob = tempFileSystem.toGlob(
            "~/project/src/main/kotlin/One.kt",
            File(rootDir).toPath()
        )
        val homeDir = System.getProperty("user.home")
        assertThat(glob).isEqualTo(
            "glob:$homeDir/project/src/main/kotlin/One.kt"
        )
    }

    private fun String.normalizePath() = replace('/', File.separatorChar)
    private fun String.normalizeGlob(): String = replace("/", rawGlobSeparator)

    private fun FileSystem.createFile(it: String) {
        val filePath = getPath(it.normalizePath())
        val fileDir = filePath.parent
        if (!Files.exists(fileDir)) Files.createDirectories(fileDir)
        Files.createFile(filePath)
    }

    private fun getFiles(
        patterns: List<String> = emptyList(),
        rootDir: Path = tempFileSystem.rootDirectories.first()
    ): List<String> = tempFileSystem
        .fileSequence(patterns, rootDir)
        .map { it.toString() }
        .toList()
}

internal val rawGlobSeparator: String get() {
    val os = System.getProperty("os.name")
    return when {
        os.startsWith("windows", ignoreCase = true) -> "\\"
        else -> "/"
    }
}
