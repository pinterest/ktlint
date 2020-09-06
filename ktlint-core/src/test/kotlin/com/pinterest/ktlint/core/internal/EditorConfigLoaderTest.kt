package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Test

internal class EditorConfigLoaderTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    private val editorConfigLoader = EditorConfigLoader(tempFileSystem)

    private fun FileSystem.normalizedPath(path: String): Path {
        val root = rootDirectories.joinToString(separator = "/")
        return getPath("$root$path")
    }

    private fun FileSystem.writeEditorConfigFile(
        filePath: String,
        content: String
    ) {
        Files.createDirectories(normalizedPath(filePath))
        Files.write(normalizedPath("$filePath/.editorconfig"), content.toByteArray())
    }

    @After
    fun tearDown() {
        tempFileSystem.close()
    }

    @Test
    fun testParentDirectoryFallback() {
        val projectDir = "/projects/project-1"
        val projectSubDirectory = "$projectDir/project-1-subdirectory"
        Files.createDirectories(tempFileSystem.normalizedPath(projectSubDirectory))
        //language=EditorConfig
        val editorConfigFiles = arrayOf(
            """
            [*]
            indent_size = 2
            """.trimIndent(),
            """
            root = true
            [*]
            indent_size = 2
            """.trimIndent(),
            """
            [*]
            indent_size = 4
            [*.{kt,kts}]
            indent_size = 2
            """.trimIndent(),
            """
            [*.{kt,kts}]
            indent_size = 4
            [*]
            indent_size = 2
            """.trimIndent()
        )

        editorConfigFiles.forEach { editorConfigFileContent ->
            tempFileSystem.writeEditorConfigFile(projectDir, editorConfigFileContent)

            val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kt")
            val editorConfig = editorConfigLoader.loadPropertiesForFile(lintFile)

            assertThat(editorConfig).isNotEmpty
            assertThat(editorConfig)
                .overridingErrorMessage(
                    "Expected \n%s\nto yield indent_size = 2",
                    editorConfigFileContent
                )
                .isEqualTo(
                    mapOf(
                        "indent_size" to "2",
                        "tab_width" to "2",
                        EditorConfigLoader.FILE_PATH_PROPERTY to lintFile.toString()
                    )
                )
        }
    }

    @Test
    fun testRootTermination() {
        val rootDir = "/projects"
        val project1Dir = "$rootDir/project-1"
        val project1Subdirectory = "$project1Dir/project-1-subdirectory"

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent()
        )

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            project1Dir,
            """
            root = true
            [*.{kt,kts}]
            indent_size = 4
            indent_style = space
            """.trimIndent()
        )

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            project1Subdirectory,
            """
            [*]
            indent_size = 2
            """.trimIndent()
        )

        val lintFileSubdirectory = tempFileSystem.normalizedPath(project1Subdirectory).resolve("test.kt")
        var parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFileSubdirectory)

        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "indent_size" to "2",
                "tab_width" to "2",
                "indent_style" to "space",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFileSubdirectory.toString()
            )
        )

        val lintFileMainDir = tempFileSystem.normalizedPath(project1Dir).resolve("test.kts")
        parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFileMainDir)

        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "indent_size" to "4",
                "tab_width" to "4",
                "indent_style" to "space",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFileMainDir.toString()
            )
        )

        val lintFileRoot = tempFileSystem.normalizedPath(rootDir).resolve("test.kt")
        parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFileRoot)

        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "end_of_line" to "lf",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFileRoot.toString()
            )
        )
    }

    @Test
    fun `Should parse assignment with spaces`() {
        val projectDir = "/project"
        @Language("EditorConfig") val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = true
            disabled_rules = import-ordering
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kt")
        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFile)

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "insert_final_newline" to "true",
                "disabled_rules" to "import-ordering",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFile.toString()
            )
        )
    }

    @Test
    fun `Should parse unset values`() {
        val projectDir = "/project"
        @Language("EditorConfig") val editorconfigFile =
            """
            [*.{kt,kts}]
            indent_size = unset
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kt")
        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFile)

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "indent_size" to "unset",
                "tab_width" to "unset",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFile.toString()
            )
        )
    }

    @Test
    fun `Should parse list with spaces after comma`() {
        val projectDir = "/project"
        @Language("EditorConfig") val editorconfigFile =
            """
            [*.{kt,kts}]
            disabled_rules=import-ordering, no-wildcard-imports
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)
        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kts")

        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFile)

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "disabled_rules" to "import-ordering, no-wildcard-imports",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFile.toString()
            )
        )
    }

    @Test
    fun `Should return emtpy map on null file path`() {
        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(null)

        assertThat(parsedEditorConfig).isEmpty()
    }

    @Test
    fun `Should return empty map for non supported file`() {
        val projectDir = "/project"
        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.txt")

        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFile)

        assertThat(parsedEditorConfig).isEmpty()
    }

    @Test
    fun `Should return properties for stdin from current directory`() {
        @Language("EditorConfig") val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = true
            disabled_rules = import-ordering
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(".", editorconfigFile)

        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(null, isStdIn = true, debug = true)

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).doesNotContainKey(EditorConfigLoader.FILE_PATH_PROPERTY)
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "insert_final_newline" to "true",
                "disabled_rules" to "import-ordering"
            )
        )
    }

    @Test
    fun `Should load properties from alternative provided editorconfig file`() {
        val rootDir = "/projects"
        val mainProjectDir = "$rootDir/project-1"
        val anotherDir = "$rootDir/project-2-dir"

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent()
        )

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            mainProjectDir,
            """
            root = true
            [*.{kt,kts}]
            indent_size = 4
            indent_style = space
            """.trimIndent()
        )

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            anotherDir,
            """
            [*]
            indent_size = 2
            """.trimIndent()
        )

        val lintFile = tempFileSystem.normalizedPath(mainProjectDir).resolve("test.kt")
        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(
            filePath = lintFile,
            alternativeEditorConfig = tempFileSystem.normalizedPath(anotherDir).resolve(".editorconfig")
        )

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "end_of_line" to "lf",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFile.toString(),
                "indent_size" to "2",
                "tab_width" to "2"
            )
        )
    }

    @Test
    fun `Should load properties from alternative editorconfig on stdin input`() {
        val rootDir = "/projects"
        val anotherDir = "$rootDir/project-2-dir"

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent()
        )

        //language=EditorConfig
        tempFileSystem.writeEditorConfigFile(
            anotherDir,
            """
            [*]
            indent_size = 2
            """.trimIndent()
        )

        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(
            filePath = null,
            alternativeEditorConfig = tempFileSystem.normalizedPath(anotherDir).resolve(".editorconfig"),
            isStdIn = true
        )

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "end_of_line" to "lf",
                "indent_size" to "2",
                "tab_width" to "2"
            )
        )
    }

    @Test
    fun `Should support editorconfig globs when loading properties for file specified under such glob`() {
        val projectDir = "/project"
        @Language("EditorConfig") val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = true
            disabled_rules = import-ordering

            [api/*.{kt,kts}]
            disabled_rules = class-must-be-internal
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("api").resolve("test.kt")
        Files.createDirectories(lintFile)

        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(lintFile, debug = true)

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "insert_final_newline" to "true",
                "disabled_rules" to "class-must-be-internal",
                EditorConfigLoader.FILE_PATH_PROPERTY to lintFile.toString()
            )
        )
    }
}
