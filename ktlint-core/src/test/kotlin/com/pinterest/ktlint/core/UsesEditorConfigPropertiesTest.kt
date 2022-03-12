package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.Property
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.DummyHolderElement
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class UsesEditorConfigPropertiesTest {
    class PropertyValueTester : UsesEditorConfigProperties {
        override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>>
            get() = emptyList()

        fun <T> testValue(
            node: ASTNode,
            editorConfigProperty: UsesEditorConfigProperties.EditorConfigProperty<T>
        ): T = node.getEditorConfigValue(editorConfigProperty)
    }

    @Test
    fun `Given that editor config property indent_size is set to an integer value then return that integer value via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            createPropertyWithValue(
                DefaultEditorConfigProperties.indentSizeProperty,
                SOME_INTEGER_VALUE.toString()
            )
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.indentSizeProperty)

        assertThat(actual).isEqualTo(SOME_INTEGER_VALUE)
    }

    @Test
    fun `Given that editor config property indent_size is set to value "unset" then return -1 as value via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            createPropertyWithValue(
                DefaultEditorConfigProperties.indentSizeProperty,
                "unset"
            )
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.indentSizeProperty)

        assertThat(actual).isEqualTo(-1)
    }

    @Test
    fun `Given that editor config property indent_size is not set then return the default tabWidth as value via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            emptyMap()
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.indentSizeProperty)

        assertThat(actual).isEqualTo(IndentConfig.DEFAULT_INDENT_CONFIG.tabWidth)
    }

    @Test
    fun `Given that editor config property max_line_length is set to an integer value then return that integer value via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            createPropertyWithValue(
                DefaultEditorConfigProperties.maxLineLengthProperty,
                SOME_INTEGER_VALUE.toString()
            )
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.maxLineLengthProperty)

        assertThat(actual).isEqualTo(SOME_INTEGER_VALUE)
    }

    @Test
    fun `Given that editor config property max_line_length is set to value "off" then return -1 via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            createPropertyWithValue(
                DefaultEditorConfigProperties.maxLineLengthProperty,
                "off"
            )
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.maxLineLengthProperty)

        assertThat(actual).isEqualTo(-1)
    }

    @Test
    fun `Given that editor config property max_line_length is set to value "unset" for android then return 100 via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(KtLint.ANDROID_USER_DATA_KEY, true)
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            createPropertyWithValue(
                DefaultEditorConfigProperties.maxLineLengthProperty,
                "unset"
            )
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.maxLineLengthProperty)

        assertThat(actual).isEqualTo(100)
    }

    @Test
    fun `Given that editor config property max_line_length is set to value "unset" for non-android then return -1 via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            createPropertyWithValue(
                DefaultEditorConfigProperties.maxLineLengthProperty,
                "unset"
            )
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.maxLineLengthProperty)

        assertThat(actual).isEqualTo(-1)
    }

    @Test
    fun `Given that editor config property max_line_length is not set for android then return 100 via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(KtLint.ANDROID_USER_DATA_KEY, true)
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            emptyMap()
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.maxLineLengthProperty)

        assertThat(actual).isEqualTo(100)
    }

    @Test
    fun `Given that editor config property max_line_length is not set for non-android then return -1 via the getEditorConfigValue of the node`() {
        val testAstNode: ASTNode = DummyHolderElement("some-text")
        testAstNode.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        testAstNode.putUserData(
            KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY,
            emptyMap()
        )
        val actual = PropertyValueTester().testValue(testAstNode, DefaultEditorConfigProperties.maxLineLengthProperty)

        assertThat(actual).isEqualTo(-1)
    }

    private fun createPropertyWithValue(
        editorConfigProperty: UsesEditorConfigProperties.EditorConfigProperty<Int>,
        value: String
    ) = mapOf(
        editorConfigProperty.type.name to Property.builder()
            .name(editorConfigProperty.type.name)
            .type(editorConfigProperty.type)
            .value(value)
            .build()
    )

    private companion object {
        const val SOME_INTEGER_VALUE = 123
    }
}
