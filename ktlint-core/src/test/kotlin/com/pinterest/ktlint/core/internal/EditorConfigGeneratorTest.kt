package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue.android
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.CodeStyleValue.official
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test

internal class EditorConfigGeneratorTest {
    private val tempFileSystem = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    private val editorConfigLoader = EditorConfigLoader(tempFileSystem)
    private val editorConfigGenerator = EditorConfigGenerator(editorConfigLoader)

    private val rootDir = "/project"
    private val rules = setOf<Rule>(TestRule1())

    @Test
    fun `Should contain the default editor config properties`() {
        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = emptySet(),
            codeStyle = official
        )

        assertThat(generatedEditorConfig.lines()).containsExactly(
            "disabled_rules = ",
            "indent_size = 4",
            "indent_style = space",
            "insert_final_newline = true",
            "ktlint_code_style = official",
            "max_line_length = -1"
        )
    }

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
            rules = rules,
            codeStyle = official
        )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE"
        )
    }

    @Test
    fun `Should use default Android code style value if value is missing and android code style is active`() {
        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules,
            codeStyle = android
        )

        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE_ANDROID",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE_ANDROID"
        )
    }

    @Test
    fun `Given distinct rules that use the same property with the same default value then is should be written only once to the editorconfig file`() {
        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = setOf(
                object : TestRule("test-rule-two"), UsesEditorConfigProperties {
                    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
                        EDITOR_CONFIG_PROPERTY_2,
                        EDITOR_CONFIG_PROPERTY_1
                    )
                },
                object : TestRule("test-rule-two"), UsesEditorConfigProperties {
                    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
                        EDITOR_CONFIG_PROPERTY_1
                    )
                }
            ),
            codeStyle = official
        )

        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE"
        )
    }

    @Test
    fun `Given distinct rules that use the same property with different default values then the distinct values should be written to the editorconfig file`() {
        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = setOf(
                object : TestRule("test-rule-two"), UsesEditorConfigProperties {
                    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
                        EDITOR_CONFIG_PROPERTY_3_WITH_DEFAULT_VALUE_A
                    )
                },
                object : TestRule("test-rule-two"), UsesEditorConfigProperties {
                    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
                        EDITOR_CONFIG_PROPERTY_3_WITH_DEFAULT_VALUE_B
                    )
                }
            ),
            codeStyle = official
        )

        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_3_NAME = $PROPERTY_3_VALUE_A",
            "$PROPERTY_3_NAME = $PROPERTY_3_VALUE_B"
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
            $PROPERTY_1_NAME = false
            """.trimIndent()
        )

        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules,
            codeStyle = official
        )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = false",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE"
        )
    }

    @Test
    fun `Should not modify existing editorconfig global property`() {
        tempFileSystem.writeEditorConfigFile(
            rootDir,
            """
            root = true
            indent_size = 4
            $PROPERTY_1_NAME = false
            """.trimIndent()
        )

        val generatedEditorConfig = editorConfigGenerator.generateEditorconfig(
            filePath = tempFileSystem.normalizedPath(rootDir).resolve("test.kt"),
            rules = rules,
            codeStyle = official
        )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE"
        )
        assertThat(generatedEditorConfig.lines()).doesNotContain(
            "$PROPERTY_1_NAME = false"
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

    private class TestRule1 : TestRule("test-rule-one"), UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
            EDITOR_CONFIG_PROPERTY_2,
            EDITOR_CONFIG_PROPERTY_1
        )
    }

    private class TestRule2 : TestRule("test-rule-two"), UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
            EDITOR_CONFIG_PROPERTY_1
        )
    }

    private open class TestRule(ruleId: String) : Rule(ruleId) {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) {
            TODO("Not yet implemented")
        }
    }

    private companion object {
        const val PROPERTY_1_NAME = "insert_final_newline"
        const val PROPERTY_1_DEFAULT_VALUE = true
        const val PROPERTY_1_DEFAULT_VALUE_ANDROID = false
        const val PROPERTY_2_NAME = "kotlin_test_if_else_num"
        const val PROPERTY_2_DEFAULT_VALUE = 10
        const val PROPERTY_2_DEFAULT_VALUE_ANDROID = 11
        const val PROPERTY_3_NAME = "property-3"
        const val PROPERTY_3_VALUE_A = "default-value-a"
        const val PROPERTY_3_VALUE_B = "default-value-b"
        val EDITOR_CONFIG_PROPERTY_1 = UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType(
                PROPERTY_1_NAME,
                "",
                PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                setOf("true", "false")
            ),
            defaultValue = PROPERTY_1_DEFAULT_VALUE,
            defaultAndroidValue = PROPERTY_1_DEFAULT_VALUE_ANDROID
        )
        val EDITOR_CONFIG_PROPERTY_2 = UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType(
                PROPERTY_2_NAME,
                "",
                PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            ),
            defaultValue = PROPERTY_2_DEFAULT_VALUE,
            defaultAndroidValue = PROPERTY_2_DEFAULT_VALUE_ANDROID
        )
        val EDITOR_CONFIG_PROPERTY_3_WITH_DEFAULT_VALUE_A = UsesEditorConfigProperties.EditorConfigProperty(
            type = PropertyType(
                PROPERTY_3_NAME,
                "",
                PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                setOf(PROPERTY_3_VALUE_A, PROPERTY_3_VALUE_B)
            ),
            defaultValue = PROPERTY_3_VALUE_A
        )
        val EDITOR_CONFIG_PROPERTY_3_WITH_DEFAULT_VALUE_B =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType(
                    PROPERTY_3_NAME,
                    "",
                    PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                    setOf(PROPERTY_3_VALUE_A, PROPERTY_3_VALUE_B)
                ),
                defaultValue = PROPERTY_3_VALUE_B
            )
    }
}
