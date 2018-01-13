package com.github.shyiko.ktlint.core

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.FileElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class MaxLineLengthConfigTest {
    private lateinit var node: ASTNode
    private val KEY = "max_line_length"

    @BeforeMethod
    fun setUp() {
        node = FileElement(KtStubElementTypes.USER_TYPE, "")
    }

    @Test
    fun shouldUseProvidedValue() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(mapOf(KEY to "120")))
        val config = MaxLineLengthConfig.create(node)
        assertThat(config.lineLength).isEqualTo(120)
    }

    @Test
    fun shouldUseDefaultValue() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, false)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(emptyMap()))
        val config = MaxLineLengthConfig.create(node)
        assertThat(config.lineLength).isEqualTo(0)
    }

    @Test
    fun shouldUseAndroidDefaultValue() {
        node.putUserData(KtLint.ANDROID_USER_DATA_KEY, true)
        node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(emptyMap()))
        val config = MaxLineLengthConfig.create(node)
        assertThat(config.lineLength).isEqualTo(100)
    }

    @Test
    fun shouldBeDisabledWhenNegativeValue() {
        val config = MaxLineLengthConfig(-1)
        assertThat(config.isDisabled()).isTrue()
        assertThat(config.isEnabled()).isFalse()
    }

    @Test
    fun shouldBeDisabledWhenZeroValue() {
        val config = MaxLineLengthConfig(0)
        assertThat(config.isDisabled()).isTrue()
        assertThat(config.isEnabled()).isFalse()
    }

    @Test
    fun shouldBeEnabledWhenPositiveValue() {
        val config = MaxLineLengthConfig(1)
        assertThat(config.isDisabled()).isFalse()
        assertThat(config.isEnabled()).isTrue()
    }
}
