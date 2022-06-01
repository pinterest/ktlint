package com.pinterest.ktlint.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [fileSequence] method.
 */
internal class FileUtilsFileSequenceTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())

    private val rootDir = tempFileSystem.rootDirectories.first().toString()
    private val project1Files = listOf(
        "${rootDir}project1/.git/ignored.kts",
        "${rootDir}project1/build.gradle.kts",
        "${rootDir}project1/src/scripts/someScript.kts",
        "${rootDir}project1/src/main/kotlin/One.kt",
        "${rootDir}project1/src/main/kotlin/example/Two.kt"
    ).map { it.normalizePath() }

    private val project2Files = listOf(
        "${rootDir}project2/build.gradle.kts",
        "${rootDir}project2/src/main/java/Three.kt"
    ).map { it.normalizePath() }

    @BeforeEach
    internal fun setUp() {
        project1Files.createFiles()
    }

    @AfterEach
    internal fun tearDown() {
        tempFileSystem.close()
    }

    @Test
    fun `On empty patterns should include all kt and kts files`() {
        val foundFiles = getFiles()

        assertThat(foundFiles).hasSize(project1Files.size - 1)
        assertThat(foundFiles).containsAll(project1Files.drop(1))
    }

    @Test
    fun `Should ignore hidden dirs on empty patterns`() {
        val foundFiles = getFiles()

        assertThat(foundFiles).doesNotContain(project1Files.first())
    }

    @Test
    fun `Should return only files matching passed pattern`() {
        val expectedResult = project1Files.drop(1).filter { it.endsWith(".kt") }
        val foundFiles = getFiles(
            listOf(
                "**/src/**/*.kt".normalizeGlob()
            )
        )

        assertThat(foundFiles).hasSize(expectedResult.size)
        assertThat(foundFiles).containsAll(expectedResult)
    }

    @Test
    fun `Should ignore hidden folders when some pattern is passed`() {
        val expectedResult = project1Files.drop(1).filter { it.endsWith(".kts") }
        val foundFiles = getFiles(
            listOf(
                "project1/**/*.kts".normalizeGlob(),
                "project1/*.kts".normalizeGlob()
            )
        )

        assertThat(foundFiles).hasSize(expectedResult.size)
        assertThat(foundFiles).containsAll(expectedResult)
    }

    @Test
    fun `Should ignore files in negated pattern`() {
        val foundFiles = getFiles(
            listOf(
                "project1/src/**/*.kt".normalizeGlob(),
                "!project1/src/**/example/*.kt".normalizeGlob()
            )
        )

        assertThat(foundFiles).hasSize(1)
        assertThat(foundFiles).containsAll(project1Files.subList(3, 4))
    }

    @Test
    fun `Should return only files for the the current rootDir matching passed patterns`() {
        project2Files.createFiles()

        val foundFiles = getFiles(
            listOf(
                "**/main/**/*.kt".normalizeGlob()
            ),
            tempFileSystem.getPath("${rootDir}project2".normalizePath())
        )

        assertThat(foundFiles).hasSize(1)
        assertThat(foundFiles).containsAll(project2Files.subList(1, 1))
    }

    @Test
    fun `Should treat correctly root path without separator in the end`() {
        val foundFiles = getFiles(
            patterns = listOf("src/main/kotlin/One.kt".normalizeGlob()),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )

        assertThat(foundFiles).hasSize(1)
        assertThat(foundFiles.first()).isEqualTo(project1Files[3])
    }

    @Test
    fun `Should support unescaped slashes for Windows`() {
        assumeTrue(System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("windows"))

        val foundFiles = getFiles(
            listOf(
                "project1\\src\\**\\*.kt".normalizeGlob(),
                "!project1\\src\\**\\example\\*.kt".normalizeGlob()
            )
        )

        assertThat(foundFiles).hasSize(1)
        assertThat(foundFiles).containsAll(project1Files.subList(3, 4))
    }

    @Test
    fun `Should support absolute paths`() {
        val globs = listOf(
            "src/main/kotlin/One.kt".normalizeGlob(),
            "${rootDir}project1/src/main/kotlin/example/Two.kt".normalizeGlob()
        )

        val files = getFiles(
            patterns = globs,
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )
        assertThat(files).containsExactlyElementsOf(
            project1Files.subList(3, 5)
        )
    }

    @Test
    fun `Should support globs containing absolute paths`() {
        val globs = listOf(
            "${rootDir}project1/src/**/*.kt".normalizeGlob()
        )

        val files = getFiles(
            patterns = globs,
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath())
        )
        assertThat(files).containsExactlyElementsOf(
            project1Files.subList(3, 5)
        )
    }

    @Test
    fun `transforming globs with leading tilde`() {
        assumeTrue(System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("linux"))

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

    private fun List<String>.createFiles() = forEach {
        val filePath = tempFileSystem.getPath(it)
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
