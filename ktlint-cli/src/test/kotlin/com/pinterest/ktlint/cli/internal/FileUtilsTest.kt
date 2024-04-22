package com.pinterest.ktlint.cli.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.test.KtlintTestFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Tests for [fileSequence] method.
 */
internal class FileUtilsTest {
    private val ktlintTestFileSystem = KtlintTestFileSystem()

    private val javaFileRootDirectory = "Root.java"
    private val ktFileRootDirectory = "Root.kt"
    private val ktsFileRootDirectory = "Root.kts"
    private val javaFileInHiddenDirectory = "project1/.hidden/Ignored.java"
    private val ktFileInHiddenDirectory = "project1/.hidden/Ignored.kt"
    private val ktsFileInHiddenDirectory = "project1/.hidden/Ignored.kts"
    private val javaFileInProjectRootDirectory = "project1/ProjectRoot.java"
    private val ktFileInProjectRootDirectory = "project1/ProjectRoot.kt"
    private val ktsFileInProjectRootDirectory = "project1/ProjectRoot.kts"
    private val ktFile1InProjectSubDirectory = "project1/src/main/kotlin/One.kt"
    private val ktFile2InProjectSubDirectory = "project1/src/main/kotlin/example/Two.kt"
    private val ktsFileInProjectSubDirectory = "project1/src/scripts/Script.kts"
    private val javaFileInProjectSubDirectory = "project1/src/main/java/One.java"
    private val someFileInOtherProjectRootDirectory = "other-project/SomeFile.txt"

    @BeforeEach
    internal fun setUp() {
        ktlintTestFileSystem.apply {
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
            createFile(someFileInOtherProjectRootDirectory)
        }
    }

    @AfterEach
    internal fun tearDown() {
        ktlintTestFileSystem.close()
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
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
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
    fun `Given the root directory where scanning starts is hidden, then the patterns should work`() {
        val foundFiles =
            getFiles(
                patterns = listOf("*.kt"),
                rootDir = ktlintTestFileSystem.resolve("project1/.hidden"),
            )

        assertThat(foundFiles).containsExactlyInAnyOrder(ktFileInHiddenDirectory)
    }

    @Test
    fun `Given some patterns including a negate pattern and no workdir then select all files except files in the negate pattern`() {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
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
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        "**/main/**/*.kt",
                    ),
                rootDir = ktlintTestFileSystem.resolve("project1"),
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
        val foundFiles =
            getFiles(
                patterns = listOf(pattern),
                rootDir = ktlintTestFileSystem.resolve("project1"),
            )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    @Test
    fun `Given an (relative) file path from the workdir then find all files in that workdir and all its sub directories that match the pattern`() {
        val foundFiles =
            getFiles(
                patterns = listOf("src/main/kotlin/One.kt"),
                rootDir = ktlintTestFileSystem.resolve("project1"),
            )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
        )
    }

    @Test
    fun `Given an (absolute) file path and a workdir then find that absolute path and all files in the workdir and all its sub directories that match the pattern`() {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        "src/main/kotlin/One.kt",
                        "${ktlintTestFileSystem.resolve(ktFile2InProjectSubDirectory).toAbsolutePath()}",
                    ),
                rootDir = ktlintTestFileSystem.resolve("project1"),
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
        val filePath = "$homeDir/project/src/main/kotlin/One.kt"
        ktlintTestFileSystem.apply {
            writeFile(filePath, SOME_CONTENT)
        }

        val foundFiles =
            getFiles(
                patterns = listOf(pattern),
                rootDir = ktlintTestFileSystem.fileSystem.rootDirectories.first(),
            )

        assertThat(foundFiles).containsExactlyInAnyOrder(filePath)
    }

    @Test
    fun `Given a pattern containing a double star and a workdir without subdirectories then find all files in that workdir`() {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        "**/*.kt",
                    ),
                rootDir = ktlintTestFileSystem.resolve("project1/src/main/kotlin/"),
            )

        assertThat(foundFiles).containsExactlyInAnyOrder(
            ktFile1InProjectSubDirectory,
            ktFile2InProjectSubDirectory,
        )
    }

    @Test
    fun `Given a pattern containing multiple double star patterns and a workdir without subdirectories then find all files in that workdir`() {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        "src/**/kotlin/**/*.kt",
                    ),
                rootDir = ktlintTestFileSystem.resolve("project1"),
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
            val dir = "/project1"
            "`Given a (relative) directory path (but not a glob) from the workdir then find all files in that workdir and it " +
                "subdirectories having the default kotlin extensions`\n" +
                "\tpatterns = $patterns\n" +
                "\trootDir = $dir"
        }
        val foundFiles =
            getFiles(
                patterns = listOf("src/main/kotlin"),
                rootDir = ktlintTestFileSystem.resolve("project1"),
            )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
            ).doesNotContain(
                javaFileInProjectSubDirectory,
            )
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `Given the Windows OS and some globs using backslash as file separator the convert the globs to using a forward slash`() {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
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
        val foundFiles =
            getFiles(
                patterns = listOf(pattern),
                rootDir = ktlintTestFileSystem.resolve("project1"),
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
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        pattern,
                        // Prevents the default patterns to be added by specifying a file
                        "/some/non/existing/file",
                    ),
                rootDir = ktlintTestFileSystem.resolve("project1"),
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
    fun `On non-WindowsOS, a pattern containing a wildcard may followed by a double-dot (parent directory) reference`(pattern: String) {
        val foundFiles =
            getFiles(
                patterns = listOf(pattern),
                rootDir = ktlintTestFileSystem.resolve("project1"),
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
    fun `On WindowsOS, a pattern containing a wildcard may followed by a double-dot (parent directory) reference`(pattern: String) {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        pattern,
                        // Prevents the default patterns to be added by specifying a file
                        "/some/non/existing/file",
                    ),
                rootDir = ktlintTestFileSystem.resolve("project1"),
            )

        assertThat(foundFiles).isEmpty()
    }

    @Test
    fun `Issue 1847 - Given a negate pattern only then include the default patterns and select all files except files in the negate pattern`() {
        val foundFiles =
            getFiles(
                patterns =
                    listOf(
                        "!project1/**/*.kt",
                    ),
            )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileRootDirectory,
                ktsFileRootDirectory,
                ktsFileInProjectRootDirectory,
                ktsFileInProjectSubDirectory,
            ).doesNotContain(
                ktFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
            )
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Issue 2002 - On non-Windows OS, find files in a sibling directory based on a relative path to the working directory`() {
        val foundFiles =
            getFiles(
                patterns = listOf("../project1"),
                rootDir = ktlintTestFileSystem.resolve(someFileInOtherProjectRootDirectory).parent.toAbsolutePath(),
            )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileInProjectRootDirectory,
                ktsFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
                ktsFileInProjectSubDirectory,
            ).doesNotContain(
                ktFileRootDirectory,
                ktsFileRootDirectory,
            )
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun `Issue 2002 - On non-Windows OS, find files in a sibling directory based on a relative glob`() {
        val foundFiles =
            getFiles(
                patterns = listOf("../project1/**/*.kt"),
                rootDir = ktlintTestFileSystem.resolve("other-project"),
            )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
            ).doesNotContain(
                ktFileRootDirectory,
            )
    }

    @Test
    fun `Issue 2002 - Find files in a sibling directory based on an absolute path`() {
        val foundFiles =
            getFiles(
                patterns = listOf(ktlintTestFileSystem.resolve(ktFileInProjectRootDirectory).parent.absolutePathString()),
                rootDir = ktlintTestFileSystem.resolve(someFileInOtherProjectRootDirectory).parent.toAbsolutePath(),
            )

        assertThat(foundFiles)
            .containsExactlyInAnyOrder(
                ktFileInProjectRootDirectory,
                ktsFileInProjectRootDirectory,
                ktFile1InProjectSubDirectory,
                ktFile2InProjectSubDirectory,
                ktsFileInProjectSubDirectory,
            ).doesNotContain(
                ktFileRootDirectory,
                ktsFileRootDirectory,
            )
    }

    private fun KtlintTestFileSystem.createFile(fileName: String) =
        writeFile(
            relativeDirectoryToRoot = fileName.substringBeforeLast("/", ""),
            fileName = fileName.substringAfterLast("/", fileName),
            content = SOME_CONTENT,
        )

    private fun getFiles(
        patterns: List<String> = DEFAULT_PATTERNS,
        rootDir: Path = ktlintTestFileSystem.rootDirectoryPath(),
    ): List<String> =
        ktlintTestFileSystem
            .fileSystem
            .fileSequence(patterns, rootDir)
            .map { it.normalize().toString() }
            .toList()
            .map { ktlintTestFileSystem.unixPathStringRelativeToRootDirectoryOfFileSystem(it) }
            .also {
                LOGGER.info {
                    "Getting files with [patterns = $patterns] and [rootdir = $rootDir] returns [files = $it]"
                }
            }

    private fun KtlintTestFileSystem.rootDirectoryPath() = resolve()

    private companion object {
        const val SOME_CONTENT = "// Not relevant for test"
    }
}
