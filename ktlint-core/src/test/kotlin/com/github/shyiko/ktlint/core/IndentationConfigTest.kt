package com.github.shyiko.ktlint.core

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.FileElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class IndentationConfigTest {
    private val regularKey = IndentationConfig.REGULAR_KEY
    private val continuationKey = IndentationConfig.CONTINUATION_KEY
    private val expectedDefaultRegularIndent = 4
    private val expectedDefaultContinuationIndent = 4
    private lateinit var node: ASTNode

    @BeforeMethod
    fun setUp() {
        node = FileElement(KtStubElementTypes.USER_TYPE, "")
    }

    @Test
    fun shouldUseDefaultValues() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(emptyMap()))
        val config = IndentationConfig.create(node)
        assertThat(config).isEqualTo(IndentationConfig(expectedDefaultRegularIndent, expectedDefaultContinuationIndent, false))
    }

    @Test
    fun shouldReturnAndroidSpecificDefaultValues() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, true)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(emptyMap()))
        val config = IndentationConfig.create(node)
        assertThat(config).isEqualTo(IndentationConfig(4, 8, false))
    }

    @Test
    fun shouldReadValuesFromConfig() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY,
            EditorConfig.fromMap(mapOf(regularKey to "1", continuationKey to "2")))
        val config = IndentationConfig.create(node)
        assertThat(config).isEqualTo(IndentationConfig(1, 2, false))
    }

    @Test
    fun shouldBeDisabledWhenRegularUnset() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY,
            EditorConfig.fromMap(mapOf(regularKey to "unset", continuationKey to "2")))
        val config = IndentationConfig.create(node)
        assertThat(config).isEqualTo(IndentationConfig(expectedDefaultRegularIndent, 2, true))
    }

    @Test
    fun shouldBeDisabledWhenContinuationUnset() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY,
            EditorConfig.fromMap(mapOf(regularKey to "1", continuationKey to "unset")))
        val config = IndentationConfig.create(node)
        assertThat(config).isEqualTo(IndentationConfig(1, expectedDefaultContinuationIndent, true))
    }
}
