package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.ParseException
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Paths

internal class PreparedCode(
    val rootNode: FileASTNode,
    val positionInTextLocator: (offset: Int) -> LineAndColumn,
    var suppressedRegionLocator: SuppressionLocator
)

internal fun prepareCodeForLinting(
    psiFileFactory: PsiFileFactory,
    params: KtLint.ExperimentalParams
): PreparedCode {
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
        normalizedText
    ) as KtFile

    val errorElement = psiFile.findErrorElement()
    if (errorElement != null) {
        val (line, col) = positionInTextLocator(errorElement.textOffset)
        throw ParseException(line, col, errorElement.errorDescription)
    }

    val rootNode = psiFile.node

    val editorConfigProperties = KtLint.editorConfigLoader.loadPropertiesForFile(
        params.normalizedFilePath,
        params.isStdIn,
        params.editorConfigPath?.let { Paths.get(it) },
        params.rules,
        params.editorConfigOverride,
        params.debug
    )

    if (!params.isStdIn) {
        rootNode.putUserData(KtLint.FILE_PATH_USER_DATA_KEY, params.normalizedFilePath.toString())
    }
    rootNode.putUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY, editorConfigProperties)

    val suppressedRegionLocator = SuppressionLocatorBuilder.buildSuppressedRegionsLocator(rootNode)

    return PreparedCode(
        rootNode,
        positionInTextLocator,
        suppressedRegionLocator
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
