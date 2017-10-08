package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class IndentationRule : Rule("indent") {

    companion object {
        // indentation size recommended by JetBrains
        private const val DEFAULT_INDENT = 4
    }

    private var indent = DEFAULT_INDENT

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val indentSize = editorConfig.get("indent_size")
            indent = indentSize?.toIntOrNull() ?: if (indentSize?.toLowerCase() == "unset") -1 else indent
            return
        }
        if (indent <= 0) {
            return
        }
        if (node is PsiWhiteSpace && !node.isPartOf(PsiComment::class)) {
            val split = node.getText().split("\n")
            if (split.size > 1) {
                var offset = node.startOffset + split.first().length + 1
                val firstParameterColumn = lazy {
                    val firstParameter = PsiTreeUtil.findChildOfType(
                        node.getNonStrictParentOfType(KtParameterList::class.java),
                        KtParameter::class.java
                    )
                    firstParameter?.run {
                        DiagnosticUtils.getLineAndColumnInPsiFile(node.containingFile,
                            TextRange(startOffset, startOffset)).column
                    } ?: 0
                }
                split.tail().forEach {
                    if (it.length % indent != 0) {
                        if (node.isPartOf(KtParameterList::class) && firstParameterColumn.value != 0) {
                            if (firstParameterColumn.value - 1 != it.length) {
                                emit(offset, "Unexpected indentation (${it.length}) (" +
                                    "parameters should be either vertically aligned or indented by the multiple of $indent" +
                                ")", false)
                            }
                        } else {
                            emit(offset, "Unexpected indentation (${it.length}) (it should be multiple of $indent)", false)
                        }
                    }
                    offset += it.length + 1
                }
            }
        }
    }
}
