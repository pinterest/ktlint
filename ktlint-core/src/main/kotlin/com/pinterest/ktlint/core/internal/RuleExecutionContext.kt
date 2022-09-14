package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.ParseException
import com.pinterest.ktlint.core.api.EditorConfigProperties
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile

private val kotlinPsiFileFactoryProvider = KotlinPsiFileFactoryProvider()

internal class RuleExecutionContext(
    val rootNode: FileASTNode,
    val editorConfigProperties: EditorConfigProperties,
    val positionInTextLocator: (offset: Int) -> LineAndColumn,
) {
    lateinit var suppressionLocator: SuppressionLocator
        private set

    init {
        rebuildSuppressionLocator()
    }

    fun rebuildSuppressionLocator() {
        suppressionLocator = SuppressionLocatorBuilder.buildSuppressedRegionsLocator(rootNode)
    }
}

internal fun createRuleExecutionContext(params: KtLint.ExperimentalParams): RuleExecutionContext {
    val psiFileFactory = kotlinPsiFileFactoryProvider.getKotlinPsiFileFactory(params.isInvokedFromCli)
    val normalizedText = normalizeText(params.text)
    val positionInTextLocator = buildPositionInTextLocator(normalizedText)

    val psiFileName = if (params.script) {
        "file.kts"
    } else {
        "file.kt"
    }
    val psiFile = psiFileFactory.createFileFromText(
        psiFileName,
        KotlinLanguage.INSTANCE,
        normalizedText,
    ) as KtFile

    val errorElement = psiFile.findErrorElement()
    if (errorElement != null) {
        val (line, col) = positionInTextLocator(errorElement.textOffset)
        throw ParseException(line, col, errorElement.errorDescription)
    }

    val rootNode = psiFile.node

    val editorConfigProperties = KtLint.editorConfigLoader.load(
        filePath = params.normalizedFilePath,
        rules = params.getRules(),
        editorConfigDefaults = params.editorConfigDefaults,
        editorConfigOverride = params.editorConfigOverride,
    )

    if (!params.isStdIn) {
        rootNode.putUserData(KtLint.FILE_PATH_USER_DATA_KEY, params.normalizedFilePath.toString())
    }

    // Keep for backwards compatibility in Ktlint 0.47.0 until ASTNode.getEditorConfigValue in UsesEditorConfigProperties
    // is removed
    rootNode.putUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY, editorConfigProperties)

    return RuleExecutionContext(
        rootNode,
        editorConfigProperties,
        positionInTextLocator,
    )
}

private fun normalizeText(text: String): String {
    return text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replaceFirst(KtLint.UTF8_BOM, "")
}

private fun PsiElement.findErrorElement(): PsiErrorElement? {
    if (this is PsiErrorElement) {
        return this
    }
    this.children.forEach { child ->
        val errorElement = child.findErrorElement()
        if (errorElement != null) {
            return errorElement
        }
    }
    return null
}
