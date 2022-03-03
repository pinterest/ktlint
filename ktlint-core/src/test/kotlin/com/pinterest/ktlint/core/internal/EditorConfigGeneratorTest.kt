package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
internal class EditorConfigGeneratorTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    private val editorConfigLoader = EditorConfigLoader(tempFileSystem)
    private val editorConfigGenerator = EditorConfigGenerator(editorConfigLoader)

    private val rootDir = "/project"
    private val rules = setOf(TestRule())

    @Test
    fun `Should use default rule value if property is missing`() {
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            """.trimIndent()
        )

        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules
        )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "${TestRule.PROP_1_NUM} = ${TestRule.PROP_1_DEFAULT}",
            "${TestRule.PROP_2_BOOL} = ${TestRule.PROP_2_DEFAULT}"
        )
    }

    @Test
    fun `Should use default Android code style value if value is missing and android code style is active`() {
        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules,
            isAndroidCodeStyle = true
        )

        assertThat(generatedEditorConfig.lines()).contains(
            "${TestRule.PROP_1_NUM} = ${TestRule.PROP_1_DEFAULT_ANDROID}",
            "${TestRule.PROP_2_BOOL} = ${TestRule.PROP_2_DEFAULT_ANDROID}"
        )
    }

    @Test
    fun `Should not modify existing editorconfig property`() {
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            indent_size = 4

            [*.{kt,kts}]
            ${TestRule.PROP_2_BOOL} = false
            """.trimIndent()
        )

        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules
        )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "${TestRule.PROP_1_NUM} = ${TestRule.PROP_1_DEFAULT}",
            "${TestRule.PROP_2_BOOL} = false"
        )
    }

    @Test
    fun `Should not modify existing editorconfig global property`() {
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            indent_size = 4
            ${TestRule.PROP_2_BOOL} = false
            """.trimIndent()
        )

        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules
        )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "${TestRule.PROP_1_NUM} = ${TestRule.PROP_1_DEFAULT}"
        )
        assertThat(generatedEditorConfig.lines()).doesNotContain(
            "${TestRule.PROP_2_BOOL} = false"
        )
    }

    private fun FileSystem.writeEditorConfigFile(
        filePath: String,
        content: String
    ) {
        Files.createDirectories(normalizedPath(filePath))
        Files.write(normalizedPath("$filePath/.editorconfig"), content.toByteArray())
    }

    private fun FileSystem.normalizedPath(path: String): Path {
        val root = rootDirectories.joinToString(separator = "/")
        return getPath("$root$path")
    }

    private class TestRule : Rule("test-id"), UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType(
                    PROP_1_NUM,
                    "",
                    PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                    setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                ),
                defaultValue = PROP_1_DEFAULT,
                defaultAndroidValue = PROP_1_DEFAULT_ANDROID
            ),
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType(
                    PROP_2_BOOL,
                    "",
                    PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    setOf("true", "false")
                ),
                defaultValue = PROP_2_DEFAULT,
                defaultAndroidValue = PROP_2_DEFAULT_ANDROID
            )
        )

        override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) {
            TODO("Not yet implemented")
        }

        companion object {
            const val PROP_1_NUM = "kotlin_test_if_else_num"
            const val PROP_1_DEFAULT = 10
            const val PROP_1_DEFAULT_ANDROID = 11
            const val PROP_2_BOOL = "insert_final_newline"
            const val PROP_2_DEFAULT = true
            const val PROP_2_DEFAULT_ANDROID = false
        }
    }
}
