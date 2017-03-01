package com.gihub.shyiko.ktlint.ruleset.standard

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

class IndentationRule : Rule("indent") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
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
                    if (it.length % 4 != 0) {
                        if (node.isPartOf(KtParameterList::class) && firstParameterColumn.value != 0) {
                            if (firstParameterColumn.value - 1 != it.length) {
                                emit(offset, "Unexpected indentation (${it.length}) (" +
                                    "parameters should be either vertically aligned or indented by the multiple of 4" +
                                ")", false)
                            }
                        } else {
                            emit(offset, "Unexpected indentation (${it.length}) (it should be multiple of 4)", false)
                        }
                    }
                    offset += it.length + 1
                }
            }
        }
    }
}
