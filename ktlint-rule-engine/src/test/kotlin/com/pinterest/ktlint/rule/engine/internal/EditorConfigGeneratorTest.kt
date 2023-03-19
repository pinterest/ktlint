package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import com.pinterest.ktlint.test.KtlintTestFileSystem
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class EditorConfigGeneratorTest {
    private val ruleProviders =
        setOf(
            RuleProvider { TestRule() },
        )
    private val rules =
        ruleProviders
            .map { it.createNewRuleInstance() }
            .toSet()
    private val ktlintTestFileSystem = KtlintTestFileSystem()
    private val editorConfigGenerator =
        EditorConfigGenerator(
            fileSystem = ktlintTestFileSystem.fileSystem,
            EditorConfigLoaderEc4j(ruleProviders.propertyTypes()),
        )

    @AfterEach
    fun tearDown() {
        ktlintTestFileSystem.close()
    }

    @Test
    fun `Should use default rule value if property is missing`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                root = true
                """.trimIndent(),
            )
        }

        val generatedEditorConfig =
            editorConfigGenerator.generateEditorconfig(
                rules = rules,
                codeStyle = CodeStyleValue.intellij_idea,
                filePath = ktlintTestFileSystem.resolve("test.kt"),
            )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE",
        )
    }

    @Test
    fun `Should use default Android code style value if value is missing and android code style is active`() {
        val generatedEditorConfig =
            editorConfigGenerator.generateEditorconfig(
                rules = rules,
                codeStyle = CodeStyleValue.android_studio,
                filePath = ktlintTestFileSystem.resolve("test.kt"),
            )

        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE_ANDROID",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE_ANDROID",
        ).doesNotContain(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE",
        )
    }

    @Test
    fun `Given distinct rules that use the same property with the same default value then is should be written only once to the editorconfig file`() {
        val generatedEditorConfig =
            editorConfigGenerator.generateEditorconfig(
                rules =
                    setOf(
                        object : Rule(
                            ruleId = RuleId("test:rule-one"),
                            about = About(),
                            usesEditorConfigProperties =
                                setOf(
                                    EDITOR_CONFIG_PROPERTY_2,
                                    EDITOR_CONFIG_PROPERTY_1,
                                ),
                        ) {},
                        object : Rule(
                            ruleId = RuleId("test:rule-two"),
                            about = About(),
                            usesEditorConfigProperties = setOf(EDITOR_CONFIG_PROPERTY_1),
                        ) {},
                    ),
                codeStyle = CodeStyleValue.intellij_idea,
                filePath = ktlintTestFileSystem.resolve("test.kt"),
            )

        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE",
        )
    }

    @Test
    fun `Should not modify existing editorconfig property`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                root = true

                [*.{kt,kts}]
                indent_size = 4
                $PROPERTY_1_NAME = false
                """.trimIndent(),
            )
        }

        val generatedEditorConfig =
            editorConfigGenerator.generateEditorconfig(
                rules = rules,
                codeStyle = CodeStyleValue.intellij_idea,
                filePath = ktlintTestFileSystem.resolve("test.kt"),
            )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_1_NAME = false",
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE",
        )
    }

    @Test
    fun `Should not modify the value of a property defined in the root editorconfig file`() {
        val rootEditorConfigPropertyValue1 = false
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                root = true

                [*.{kt,kts}]
                indent_size = 4
                $PROPERTY_1_NAME = $rootEditorConfigPropertyValue1
                """.trimIndent(),
            )
        }

        val generatedEditorConfig =
            editorConfigGenerator.generateEditorconfig(
                rules = rules,
                codeStyle = CodeStyleValue.intellij_idea,
                filePath = ktlintTestFileSystem.resolve("test.kt"),
            )

        assertThat(generatedEditorConfig.lines()).doesNotContainAnyElementsOf(listOf("root = true"))
        assertThat(generatedEditorConfig.lines()).contains(
            "$PROPERTY_2_NAME = $PROPERTY_2_DEFAULT_VALUE",
        )
        assertThat(generatedEditorConfig.lines())
            .doesNotContain("$PROPERTY_1_NAME = $PROPERTY_1_DEFAULT_VALUE")
            .contains("$PROPERTY_1_NAME = $rootEditorConfigPropertyValue1")
    }

    private class TestRule : Rule(
        ruleId = RuleId("test:test-rule"),
        about = About(),
        usesEditorConfigProperties =
            setOf(
                EDITOR_CONFIG_PROPERTY_2,
                EDITOR_CONFIG_PROPERTY_1,
            ),
    )

    private companion object {
        //language=
        const val PROPERTY_1_NAME = "property-1"
        const val PROPERTY_1_DEFAULT_VALUE = true
        const val PROPERTY_1_DEFAULT_VALUE_ANDROID = false
        const val PROPERTY_2_NAME = "property-2"
        const val PROPERTY_2_DEFAULT_VALUE = 10
        const val PROPERTY_2_DEFAULT_VALUE_ANDROID = 11
        val EDITOR_CONFIG_PROPERTY_1 =
            EditorConfigProperty(
                name = PROPERTY_1_NAME,
                type =
                    PropertyType(
                        PROPERTY_1_NAME,
                        "",
                        PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        setOf("true", "false"),
                    ),
                defaultValue = PROPERTY_1_DEFAULT_VALUE,
                androidStudioCodeStyleDefaultValue = PROPERTY_1_DEFAULT_VALUE_ANDROID,
            )
        val EDITOR_CONFIG_PROPERTY_2 =
            EditorConfigProperty(
                name = PROPERTY_2_NAME,
                type =
                    PropertyType(
                        PROPERTY_2_NAME,
                        "",
                        PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                        setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                    ),
                defaultValue = PROPERTY_2_DEFAULT_VALUE,
                androidStudioCodeStyleDefaultValue = PROPERTY_2_DEFAULT_VALUE_ANDROID,
            )
    }
}
