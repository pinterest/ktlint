package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem
import java.nio.file.Files
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class EditorConfigInternalTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())

    private fun FileSystem.writeEditorConfigFile(
        filePath: String,
        content: String
    ) {
        Files.createDirectories(getPath(filePath))
        Files.write(getPath("$filePath/.editorconfig"), content.toByteArray())
    }

    @After
    fun tearDown() {
        tempFileSystem.close()
    }

    @Test
    fun testParentDirectoryFallback() {
        val projectDir = "/projects/project-1"
        val projectSubDirectory = "$projectDir/project-1-subdirectory"
        Files.createDirectories(tempFileSystem.getPath(projectSubDirectory))
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

            val editorConfig = EditorConfigInternal.of(
                tempFileSystem.getPath(projectSubDirectory)
            )

            assertThat(editorConfig?.parent).isNull()
            assertThat(editorConfig?.toMap())
                .overridingErrorMessage(
                    "Expected \n%s\nto yield indent_size = 2",
                    editorConfigFileContent
                )
                .isEqualTo(
                    mapOf(
                        "indent_size" to "2",
                        "tab_width" to "2"
                    )
                )
        }
    }

    @Test
    fun testRootTermination() {
        val rootDir = "/projects"
        val project1Dir = "$rootDir/project-1"
        val project1Subdirectory = "$project1Dir/project-1-subdirectory"
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent()
        )
        tempFileSystem.writeEditorConfigFile(
            project1Dir,
            """
            root = true
            [*.{kt,kts}]
            indent_size = 4
            indent_style = space
            """.trimIndent()
        )
        tempFileSystem.writeEditorConfigFile(
            project1Subdirectory,
            """
            [*]
            indent_size = 2
            """.trimIndent()
        )

        var parsedEditorConfig = EditorConfigInternal.of(
            tempFileSystem.getPath(project1Subdirectory)
        )
        assertThat(parsedEditorConfig?.parent).isNotNull
        assertThat(parsedEditorConfig?.parent?.parent).isNull()
        assertThat(parsedEditorConfig?.toMap()).isEqualTo(
            mapOf(
                "indent_size" to "2",
                "tab_width" to "2",
                "indent_style" to "space"
            )
        )

        parsedEditorConfig = EditorConfigInternal.of(
            tempFileSystem.getPath(project1Dir)
        )
        assertThat(parsedEditorConfig?.parent).isNull()
        assertThat(parsedEditorConfig?.toMap()).isEqualTo(
            mapOf(
                "indent_size" to "4",
                "tab_width" to "4",
                "indent_style" to "space"
            )
        )

        parsedEditorConfig = EditorConfigInternal.of(
            tempFileSystem.getPath(rootDir)
        )
        assertThat(parsedEditorConfig?.parent).isNull()
        assertThat(parsedEditorConfig?.toMap()).isEqualTo(
            mapOf(
                "end_of_line" to "lf"
            )
        )
    }

    @Test
    fun testSectionParsing() {
        assertThat(EditorConfigInternal.parseSection("*")).isEqualTo(listOf("*"))
        assertThat(EditorConfigInternal.parseSection("*.{js,py}")).isEqualTo(listOf("*.js", "*.py"))
        assertThat(EditorConfigInternal.parseSection("*.py")).isEqualTo(listOf("*.py"))
        assertThat(EditorConfigInternal.parseSection("Makefile")).isEqualTo(listOf("Makefile"))
        assertThat(EditorConfigInternal.parseSection("lib/**.js")).isEqualTo(listOf("lib/**.js"))
        assertThat(EditorConfigInternal.parseSection("{package.json,.travis.yml}"))
            .isEqualTo(listOf("package.json", ".travis.yml"))
    }

    @Test
    fun testMalformedSectionParsing() {
        assertThat(EditorConfigInternal.parseSection("")).isEqualTo(listOf(""))
        assertThat(EditorConfigInternal.parseSection(",*")).isEqualTo(listOf("", "*"))
        assertThat(EditorConfigInternal.parseSection("*,")).isEqualTo(listOf("*", ""))
        assertThat(EditorConfigInternal.parseSection("*.{js,py")).isEqualTo(listOf("*.js", "*.py"))
        assertThat(EditorConfigInternal.parseSection("*.{js,{py")).isEqualTo(listOf("*.js", "*.{py"))
        assertThat(EditorConfigInternal.parseSection("*.py}")).isEqualTo(listOf("*.py}"))
    }

    @Test
    fun `Should parse assignment with spaces`() {
        val projectDir = "/project"
        val editorconfigFile =
            """
            [*.{kt, kts}]
            insert_final_newline = true
            disabled_rules = import-ordering
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val parsedEditorConfig = EditorConfigInternal.of(
            tempFileSystem.getPath(projectDir)
        )

        assertThat(parsedEditorConfig).isNotNull
        assertThat(parsedEditorConfig?.toMap()).isEqualTo(
            mapOf(
                "insert_final_newline" to "true",
                "disabled_rules" to "import-ordering"
            )
        )
    }

    @Test
    fun `Should parse list with spaces after comma`() {
        val projectDir = "/project"
        val editorconfigFile =
            """
            [*.{kt, kts}]
            disabled_rules = import-ordering, no-wildcard-imports
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val parsedEditorConfig = EditorConfigInternal.of(
            tempFileSystem.getPath(projectDir)
        )

        assertThat(parsedEditorConfig).isNotNull
        assertThat(parsedEditorConfig?.toMap()).isEqualTo(
            mapOf(
                "disabled_rules" to "import-ordering, no-wildcard-imports"
            )
        )
    }
}
