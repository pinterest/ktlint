package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class MaxLineLengthRule : Rule("max-line-length"), Rule.Modifier.RestrictToRootLast {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val android = node.getUserData(KtLint.ANDROID_USER_DATA_KEY)!!
            val maxLineLength = editorConfig.get("max_line_length")?.toIntOrNull()
                ?: if (android) 100 else 0
            if (maxLineLength <= 0) {
                return
            }
            val text = node.text
            val lines = text.split("\n")
            var offset = 0
            for (line in lines) {
                if (line.length > maxLineLength) {
                    val el = node.psi.findElementAt(offset + line.length - 1)!!
                    if (!el.isPartOf(KDoc::class)) {
                        if (!el.isPartOf(PsiComment::class)) {
                            if (!el.isPartOf(KtPackageDirective::class) && !el.isPartOf(KtImportDirective::class)) {
                                emit(offset, "Exceeded max line length ($maxLineLength)", false)
                            }
                        } else {
                            // if comment is the only thing on the line - fine, otherwise emit an error
                            val prevLeaf = el.getPrevSiblingIgnoringWhitespaceAndComments(false)
                            if (prevLeaf != null && prevLeaf.startOffset >= offset) {
                                emit(offset, "Exceeded max line length ($maxLineLength)", false)
                            }
                        }
                    }
                }
                offset += line.length + 1
            }
        }
    }
}
