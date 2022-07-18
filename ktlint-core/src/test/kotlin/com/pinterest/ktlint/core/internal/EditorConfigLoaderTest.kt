package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.insertNewLineProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.internal.EditorConfigLoader.Companion.convertToRawValues
import com.pinterest.ktlint.ruleset.standard.FinalNewlineRule
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class EditorConfigLoaderTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    private val editorConfigLoader = EditorConfigLoader(tempFileSystem)
    private val rules = setOf(TestRule())

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

    @AfterEach
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
            val editorConfig = editorConfigLoader.loadPropertiesForFile(lintFile, rules = rules)
            val parsedEditorConfig = editorConfig.convertToRawValues()

            assertThat(parsedEditorConfig).isNotEmpty
            assertThat(parsedEditorConfig)
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
        var editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFileSubdirectory, rules = rules)
        var parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "indent_size" to "2",
                "tab_width" to "2",
                "indent_style" to "space"
            )
        )

        val lintFileMainDir = tempFileSystem.normalizedPath(project1Dir).resolve("test.kts")
        editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFileMainDir, rules = rules)
        parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "indent_size" to "4",
                "tab_width" to "4",
                "indent_style" to "space"
            )
        )

        val lintFileRoot = tempFileSystem.normalizedPath(rootDir).resolve("test.kt")
        editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFileRoot, rules = rules)
        parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "end_of_line" to "lf"
            )
        )
    }

    @Test
    fun `Should parse assignment with spaces`() {
        val projectDir = "/project"

        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = true
            disabled_rules = import-ordering
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kt")
        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFile, rules = rules)
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "insert_final_newline" to "true",
                "disabled_rules" to "import-ordering"
            )
        )
    }

    @Test
    fun `Should parse unset values`() {
        val projectDir = "/project"

        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            indent_size = unset
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kt")
        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFile, rules = rules)
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "indent_size" to "unset",
                "tab_width" to "unset"
            )
        )
    }

    @Test
    fun `Should parse list with spaces after comma`() {
        val projectDir = "/project"

        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            disabled_rules=import-ordering, no-wildcard-imports
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)
        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kts")

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFile, rules = rules)
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "disabled_rules" to "import-ordering, no-wildcard-imports"
            )
        )
    }

    @Test
    fun `Should return the override properties only on null file path`() {
        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(
            filePath = null,
            rules = rules,
            editorConfigOverride = EditorConfigOverride.from(insertNewLineProperty to "true")
        )

        assertThat(parsedEditorConfig.convertToRawValues()).containsExactly(
            entry("insert_final_newline", "true")
        )
    }

    @Test
    fun `Should return the override properties only non supported file`() {
        val parsedEditorConfig = editorConfigLoader.loadPropertiesForFile(
            filePath = null,
            rules = rules,
            editorConfigOverride = EditorConfigOverride.from(insertNewLineProperty to "true")
        )

        assertThat(parsedEditorConfig.convertToRawValues()).containsExactly(
            entry("insert_final_newline", "true")
        )
    }

    @Test
    fun `Should return properties for stdin from current directory`() {
        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = true
            disabled_rules = import-ordering
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(".", editorconfigFile)

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(
            filePath = null,
            isStdIn = true,
            rules = rules,
            debug = true
        )
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
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
        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(
            filePath = lintFile,
            alternativeEditorConfig = tempFileSystem.normalizedPath(anotherDir).resolve(".editorconfig"),
            rules = rules
        )
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

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

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(
            filePath = null,
            alternativeEditorConfig = tempFileSystem.normalizedPath(anotherDir).resolve(".editorconfig"),
            isStdIn = true,
            rules = rules
        )
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

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

        @Language("EditorConfig")
        val editorconfigFile =
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

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(lintFile, debug = true, rules = rules)
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "insert_final_newline" to "true",
                "disabled_rules" to "class-must-be-internal"
            )
        )
    }

    @Test
    fun `Should add property from override`() {
        val projectDir = "/project"

        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            disabled_rules=import-ordering, no-wildcard-imports
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kts")

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(
            lintFile,
            rules = rules.plus(FinalNewlineRule()),
            editorConfigOverride = EditorConfigOverride.from(insertNewLineProperty to "true")
        )
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "disabled_rules" to "import-ordering, no-wildcard-imports",
                "insert_final_newline" to "true"
            )
        )
    }

    @Test
    fun `Should replace property from override`() {
        val projectDir = "/project"

        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = true
            """.trimIndent()
        tempFileSystem.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = tempFileSystem.normalizedPath(projectDir).resolve("test.kts")

        val editorConfigProperties = editorConfigLoader.loadPropertiesForFile(
            lintFile,
            rules = rules.plus(FinalNewlineRule()),
            editorConfigOverride = EditorConfigOverride.from(insertNewLineProperty to "false")
        )
        val parsedEditorConfig = editorConfigProperties.convertToRawValues()

        assertThat(parsedEditorConfig).isNotEmpty
        assertThat(parsedEditorConfig).isEqualTo(
            mapOf(
                "insert_final_newline" to "false"
            )
        )
    }

    private class TestRule : Rule("editorconfig-test"), UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = emptyList()

        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) {
            throw NotImplementedError()
        }
    }
}
