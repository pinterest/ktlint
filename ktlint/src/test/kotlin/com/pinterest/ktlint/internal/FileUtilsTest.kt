package com.pinterest.ktlint.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Tests for [fileSequence] method.
 */
internal class FileUtilsTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())

    private val rootDir = tempFileSystem.rootDirectories.first().toString()
    private val javaFileRootDirectory = "${rootDir}Root.java".normalizePath()
    private val ktFileRootDirectory = "${rootDir}Root.kt".normalizePath()
    private val ktsFileRootDirectory = "${rootDir}Root.kts".normalizePath()
    private val javaFileInHiddenDirectory = "${rootDir}project1/.git/Ignored.java".normalizePath()
    private val ktFileInHiddenDirectory = "${rootDir}project1/.git/Ignored.kt".normalizePath()
    private val ktsFileInHiddenDirectory = "${rootDir}project1/.git/Ignored.kts".normalizePath()
    private val javaFileInProjectRootDirectory = "${rootDir}project1/ProjectRoot.java".normalizePath()
    private val ktFileInProjectRootDirectory = "${rootDir}project1/ProjectRoot.kt".normalizePath()
    private val ktsFileInProjectRootDirectory = "${rootDir}project1/ProjectRoot.kts".normalizePath()
    private val ktFile1InProjectSubDirectory = "${rootDir}project1/src/main/kotlin/One.kt".normalizePath()
    private val ktFile2InProjectSubDirectory = "${rootDir}project1/src/main/kotlin/example/Two.kt".normalizePath()
    private val ktsFileInProjectSubDirectory = "${rootDir}project1/src/scripts/Script.kts".normalizePath()
    private val javaFileInProjectSubDirectory = "${rootDir}project1/src/main/java/One.java".normalizePath()

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
                ktsFileInProjectSubDirectory,
            ).doesNotContain(
                javaFileInHiddenDirectory,
                ktFileInHiddenDirectory,
                ktsFileInHiddenDirectory,
            )
    }

    @Test
    fun `Given some patterns and no workdir then ignore all files in hidden directories`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "project1/**/*.kt",
                "project1/*.kt",
            ),
        )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
            ).doesNotContain(
                javaFileInHiddenDirectory,
                ktFileInHiddenDirectory,
                ktsFileInHiddenDirectory,
            )
    }

    @Test
    fun `Given some patterns including a negate pattern and no workdir then select all files except files in the negate pattern`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "project1/src/**/*.kt",
                "!project1/src/**/example/*.kt",
            ),
        )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(ktFile1InProjectSubDirectory)
            .doesNotContain(ktFile2InProjectSubDirectory)
    }

    @Test
    fun `Given a pattern and a workdir then find all files in that workdir and all its sub directories that match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "**/main/**/*.kt",
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    @ParameterizedTest(name = "Pattern: {0}")
    @ValueSource(
        strings = [
            // Redundant path below should resolve to "**/main/**/*.kt" for the test to succeed
            "./**/main/**/*.kt",
            "**/./main/**/*.kt",
            "**/main/./**/*.kt",
            "**/main/**/./*.kt",
            "xx/../**/main/**/./*.kt",
            "**/xx/../main/**/*.kt",
            "**/main/xx/../**/*.kt",
            "**/main/**/./xx/../*.kt",
        ],
    )
    fun `Given a pattern containing redundant elements then find all files in that workdir and all its sub directories that match the pattern without the redundant items`(
        pattern: String,
    ) {
        val foundFiles = getFiles(
            patterns = listOf(pattern),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    @Test
    fun `Given an (relative) file path from the workdir then find all files in that workdir and all its sub directories that match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf("src/main/kotlin/One.kt"),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
        )
    }

    @Test
    fun `Given an (absolute) file path and a workdir then find that absolute path and all files in the workdir and all its sub directories that match the pattern`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "src/main/kotlin/One.kt",
                ktFile2InProjectSubDirectory,
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    // Jimfs does not currently support the Windows syntax for an absolute path on the current drive (e.g. "\foo\bar")
    @DisabledOnOs(OS.WINDOWS)
    @ParameterizedTest(name = "Pattern: {0}")
    @ValueSource(
        strings = [
            "~/project/src/main/kotlin/One.kt",
            "~/project/src/main/kotlin/*.kt",
            "~/project/src/main/kotlin/",
            "~/project/src/main/kotlin",
            "~/project/src/main/**/*.kt",
        ],
    )
    fun `Given a non-Windows OS and a pattern that starts with a tilde then transform the globs to the user home directory`(
        pattern: String,
    ) {
        val homeDir = System.getProperty("user.home")
        val filePath = "$homeDir/project/src/main/kotlin/One.kt".normalizePath()
        tempFileSystem.createFile(filePath)

        val foundFiles = getFiles(
            patterns = listOf(pattern),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(filePath)
    }

    @Test
    fun `Given a pattern containing a double star and a workdir without subdirectories then find all files in that workdir`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "**/*.kt",
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1/src/main/kotlin/".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    @Test
    fun `Given a pattern containing multiple double star patters and a workdir without subdirectories then find all files in that workdir`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "src/**/kotlin/**/*.kt",
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    @Test
    fun `Given a (relative) directory path (but not a glob) from the workdir then find all files in that workdir and it subdirectories having the default kotlin extensions`() {
        LOGGER.info {
            val patterns = "src/main/kotlin"
            val dir = "${rootDir}project1".normalizePath()
            "`Given a (relative) directory path (but not a glob) from the workdir then find all files in that workdir and it subdirectories having the default kotlin extensions`\n" +
                "\tpatterns = $patterns\n" +
                "\trootDir = $dir"
        }
        val foundFiles = getFiles(
            patterns = listOf("src/main/kotlin"),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        ).doesNotContain(
            javaFileInProjectSubDirectory,
        )
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `Given the Windows OS and some globs using backslash as file separator the convert the globs to using a forward slash`() {
        val foundFiles = getFiles(
            patterns = listOf(
                "project1\\src\\**\\*.kt",
                "!project1\\src\\**\\example\\*.kt",
            ),
        )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(ktFile1InProjectSubDirectory)
            .doesNotContain(ktFile2InProjectSubDirectory)
    }

    @DisabledOnOs(OS.WINDOWS)
    @ParameterizedTest(name = "Pattern: {0}")
    @ValueSource(
        strings = [
            "../**/*.kt",
            "../**/src/main/kotlin/One.kt",
            "src/../../project1/src/**/*.kt",
            "src/../../project1/src/main/kotlin/*.kt",
        ],
    )
    fun `On non-WindowsOS, a pattern containing a double-dot (parent directory) reference may leave the current directory`(
        pattern: String,
    ) {
        val foundFiles = getFiles(
            patterns = listOf(pattern),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).contains(ktFile1InProjectSubDirectory)
    }

    @EnabledOnOs(OS.WINDOWS)
    @ParameterizedTest(name = "Pattern: {0}")
    @ValueSource(
        strings = [
            "../**/*.kt",
            "../**/src/main/kotlin/One.kt",
            "src/../../project1/src/**/*.kt",
            "src/../../project1/src/main/kotlin/*.kt",
        ],
    )
    fun `On WindowsOS, a pattern containing a double-dot (parent directory) reference may not leave the current directory`(
        pattern: String,
    ) {
        val foundFiles = getFiles(
            patterns = listOf(
                pattern,
                "/some/non/existing/file", // This prevents the default patterns to be added
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).isEmpty()
    }

    @DisabledOnOs(OS.WINDOWS)
    @ParameterizedTest(name = "Pattern: {0}")
    @ValueSource(
        strings = [
            "**/../**/*.kt",
            "**/../src/main/kotlin/One.kt",
            "src/main/k*/../kotlin/One.kt",
        ],
    )
    fun `On non-WindowsOS, a pattern containing a wildcard may followed by a double-dot (parent directory) reference`(
        pattern: String,
    ) {
        val foundFiles = getFiles(
            patterns = listOf(pattern),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).contains(ktFile1InProjectSubDirectory)
    }

    @EnabledOnOs(OS.WINDOWS)
    @ParameterizedTest(name = "Pattern: {0}")
    @ValueSource(
        strings = [
            "**/../**/*.kt",
            "**/../src/main/kotlin/One.kt",
            "src/main/k*/../kotlin/One.kt",
        ],
    )
    fun `On WindowsOS, a pattern containing a wildcard may followed by a double-dot (parent directory) reference`(
        pattern: String,
    ) {
        val foundFiles = getFiles(
            patterns = listOf(
                pattern,
                "/some/non/existing/file", // This prevents the default patterns to be added
            ),
            rootDir = tempFileSystem.getPath("${rootDir}project1".normalizePath()),
        )

        assertThat(foundFiles).isEmpty()
    }

    private fun String.normalizePath() = replace("/", tempFileSystem.separator)

    private fun FileSystem.createFile(it: String) {
        val filePath = getPath(it.normalizePath())
        val fileDir = filePath.parent
        if (!Files.exists(fileDir)) Files.createDirectories(fileDir)
        Files.createFile(filePath)
    }

    private fun getFiles(
        patterns: List<String> = emptyList(),
        rootDir: Path = tempFileSystem.rootDirectories.first(),
    ): List<String> = tempFileSystem
        .fileSequence(patterns, rootDir)
        .map { it.normalize().toString() }
        .toList()
        .also {
            LOGGER.info {
                "Getting files with [patterns = $patterns] and [rootdir = $rootDir] returns [files = $it]"
            }
        }
}
