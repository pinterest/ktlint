package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.ruleset.standard.FinalNewlineRule
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("EditorConfigKeyCorrectness")
internal class EditorConfigLoaderTest {
    private val fileSystemMock = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    private val editorConfigLoader = EditorConfigLoader(fileSystemMock)
    private val rules = setOf(TestRule())

    @AfterEach
    fun tearDown() {
        fileSystemMock.close()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """
            [*]
            indent_size = 2
            """,
            """
            root = true
            [*]
            indent_size = 2
            """,
            """
            [*]
            indent_size = 4
            [*.{kt,kts}]
            indent_size = 2
            """,
            """
            [*.{kt,kts}]
            indent_size = 4
            [*]
            indent_size = 2
            """,
        ],
    )
    fun testParentDirectoryFallback(editorConfigFileContent: String) {
        val projectDir = "/projects/project-1"
        val projectSubDirectory = "$projectDir/project-1-subdirectory"
        Files.createDirectories(fileSystemMock.normalizedPath(projectSubDirectory))

        fileSystemMock.writeEditorConfigFile(projectDir, editorConfigFileContent.trimIndent())

        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("test.kt")
        val editorConfig = editorConfigLoader.load(lintFile, rules = rules)

        assertThat(editorConfig.convertToPropertyValues())
            .containsExactlyInAnyOrder(
                "indent_size = 2",
                "tab_width = 2",
            )
    }

    //language=
    @Test
    fun testRootTermination() {
        val rootDir = "/projects"
        val project1Dir = "$rootDir/project-1"
        val project1Subdirectory = "$project1Dir/project-1-subdirectory"

        //language=EditorConfig
        fileSystemMock.writeEditorConfigFile(
            rootDir,
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent(),
        )

        //language=EditorConfig
        fileSystemMock.writeEditorConfigFile(
            project1Dir,
            """
            root = true
            [*.{kt,kts}]
            indent_size = 4
            indent_style = space
            """.trimIndent(),
        )

        //language=EditorConfig
        fileSystemMock.writeEditorConfigFile(
            project1Subdirectory,
            """
            [*]
            indent_size = 2
            """.trimIndent(),
        )

        val lintFileSubdirectory = fileSystemMock.normalizedPath(project1Subdirectory).resolve("test.kt")
        var editorConfigProperties = editorConfigLoader.load(lintFileSubdirectory, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "indent_size = 2",
            "tab_width = 2",
            "indent_style = space",
        )

        val lintFileMainDir = fileSystemMock.normalizedPath(project1Dir).resolve("test.kts")
        editorConfigProperties = editorConfigLoader.load(lintFileMainDir, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "indent_size = 4",
            "tab_width = 4",
            "indent_style = space",
        )

        val lintFileRoot = fileSystemMock.normalizedPath(rootDir).resolve("test.kt")
        editorConfigProperties = editorConfigLoader.load(lintFileRoot, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "end_of_line = lf",
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
            ktlint_disabled_rules = import-ordering
            ktlint_standard_import-ordering = disabled
            """.trimIndent()
        fileSystemMock.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("test.kt")
        val editorConfigProperties = editorConfigLoader.load(lintFile, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "insert_final_newline = true",
            "disabled_rules = import-ordering",
            "ktlint_disabled_rules = import-ordering",
            "ktlint_standard_import-ordering = disabled",
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
        fileSystemMock.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("test.kt")
        val editorConfigProperties = editorConfigLoader.load(lintFile, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "indent_size = unset",
            "tab_width = unset",
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
            ktlint_disabled_rules=import-ordering, no-wildcard-imports
            """.trimIndent()
        fileSystemMock.writeEditorConfigFile(projectDir, editorconfigFile)
        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("test.kts")

        val editorConfigProperties = editorConfigLoader.load(lintFile, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "disabled_rules = import-ordering, no-wildcard-imports",
            "ktlint_disabled_rules = import-ordering, no-wildcard-imports",
        )
    }

    @Test
    fun `Should return the override properties only on null file path`() {
        val parsedEditorConfig = editorConfigLoader.load(
            filePath = null,
            rules = rules,
            editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "true"),
        )

        assertThat(parsedEditorConfig.convertToPropertyValues()).containsExactlyInAnyOrder(
            "insert_final_newline = true",
        )
    }

    @Test
    fun `Should return the override properties only non supported file`() {
        val parsedEditorConfig = editorConfigLoader.load(
            filePath = null,
            rules = rules,
            editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "true"),
        )

        assertThat(parsedEditorConfig.convertToPropertyValues()).containsExactlyInAnyOrder(
            "insert_final_newline = true",
        )
    }

    @Test
    fun `Should return properties for stdin from editorconfig file in current directory and override properties`() {
        @Language("EditorConfig")
        val editorconfigFile =
            """
            [*.{kt,kts}]
            insert_final_newline = false
            disabled_rules = import-ordering
            ktlint_disabled_rules = import-ordering
            ktlint_standard_import-ordering = disabled
            """.trimIndent()
        fileSystemMock.writeEditorConfigFile(".", editorconfigFile)

        val editorConfigProperties = editorConfigLoader.load(
            filePath = null,
            rules = rules,
            editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to true),
        )

        assertThat(editorConfigProperties.convertToPropertyValues())
            .containsExactlyInAnyOrder(
                "insert_final_newline = true",
                "disabled_rules = import-ordering",
                "ktlint_disabled_rules = import-ordering",
                "ktlint_standard_import-ordering = disabled",
            )
    }

    @Test
    fun `Given a project with editorconfig properties (root=true) and override properties then ignore properties from root dir but apply the override properties`() {
        //language=
        val rootDir = "/projects"
        //language=
        val mainProjectDir = "$rootDir/project-1"

        // Ignore the properties from the rootDir as "root = true" for project-1
        //language=EditorConfig
        fileSystemMock.writeEditorConfigFile(
            rootDir,
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent(),
        )

        //language=EditorConfig
        fileSystemMock.writeEditorConfigFile(
            mainProjectDir,
            """
            root = true
            [*.{kt,kts}]
            indent_size = 4
            indent_style = space
            """.trimIndent(),
        )

        val lintFile = fileSystemMock.normalizedPath(mainProjectDir).resolve("test.kt")
        val editorConfigProperties =
            editorConfigLoader
                .load(
                    filePath = lintFile,
                    rules = rules,
                    editorConfigOverride = EditorConfigOverride.from(INDENT_SIZE_PROPERTY to 2),
                )

        assertThat(editorConfigProperties.convertToPropertyValues())
            .containsExactlyInAnyOrder(
                "indent_style = space",
                "indent_size = 2",
                "tab_width = 2",
            )
    }

    //language=
    @Test
    fun `Should load properties from override on stdin input`() {
        fileSystemMock.writeEditorConfigFile(
            ".",
            //language=EditorConfig
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent(),
        )

        val editorConfigProperties = editorConfigLoader.load(
            filePath = null,
            rules = rules,
            editorConfigOverride = EditorConfigOverride.from(INDENT_SIZE_PROPERTY to 2),
        )

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "end_of_line = lf",
            "indent_size = 2",
            "tab_width = 2",
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
            ktlint_disabled_rules = import-ordering
            ktlint_standard_import-ordering = disabled

            [api/*.{kt,kts}]
            disabled_rules = class-must-be-internal
            ktlint_disabled_rules = class-must-be-internal
            """.trimIndent()
        fileSystemMock.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("api").resolve("test.kt")
        Files.createDirectories(lintFile)

        val editorConfigProperties = editorConfigLoader.load(lintFile, rules = rules)

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "insert_final_newline = true",
            "disabled_rules = class-must-be-internal",
            "ktlint_disabled_rules = class-must-be-internal",
            "ktlint_standard_import-ordering = disabled",
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
            ktlint_disabled_rules=import-ordering, no-wildcard-imports
            ktlint_standard_import-ordering=disabled
            ktlint_standard_no-wildcard-imports=disabled
            """.trimIndent()
        fileSystemMock.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("test.kts")

        val editorConfigProperties = editorConfigLoader.load(
            lintFile,
            rules = rules.plus(FinalNewlineRule()),
            editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "true"),
        )

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "disabled_rules = import-ordering, no-wildcard-imports",
            "ktlint_standard_import-ordering = disabled",
            "ktlint_standard_no-wildcard-imports = disabled",
            "ktlint_disabled_rules = import-ordering, no-wildcard-imports",
            "insert_final_newline = true",
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
        fileSystemMock.writeEditorConfigFile(projectDir, editorconfigFile)

        val lintFile = fileSystemMock.normalizedPath(projectDir).resolve("test.kts")

        val editorConfigProperties = editorConfigLoader.load(
            lintFile,
            rules = rules.plus(FinalNewlineRule()),
            editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "false"),
        )

        assertThat(editorConfigProperties.convertToPropertyValues()).containsExactlyInAnyOrder(
            "insert_final_newline = false",
        )
    }

    private fun FileSystem.normalizedPath(path: String): Path {
        val root = rootDirectories.joinToString(separator = "/")
        return getPath("$root$path")
    }

    private fun FileSystem.writeEditorConfigFile(
        filePath: String,
        content: String,
    ) {
        Files.createDirectories(normalizedPath(filePath))
        Files.write(normalizedPath("$filePath/.editorconfig"), content.toByteArray())
    }

    private fun EditorConfigProperties.convertToPropertyValues(): List<String> {
        return if (isEmpty()) {
            emptyList()
        } else {
            map {
                val value = if (it.value.isUnset) {
                    "unset"
                } else {
                    it.value.sourceValue
                }
                "${it.key} = $value"
            }
        }
    }

    private class TestRule : Rule("editorconfig-test"), UsesEditorConfigProperties {
        override val editorConfigProperties: List<EditorConfigProperty<*>> = emptyList()

        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ) {
            throw NotImplementedError()
        }
    }
}
