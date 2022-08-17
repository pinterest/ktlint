package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NoConsecutiveBlankLinesRule : Rule("no-consecutive-blank-lines") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is PsiWhiteSpace &&
            node.prevSibling != null
        ) {
            val text = node.getText()
            val lfcount = text.count { it == '\n' }
            if (lfcount < 2) {
                return
            }

            val eof = node.nextLeaf() == null
            val prevNode = node.treePrev
            val betweenClassAndPrimaryConstructor = prevNode.elementType == IDENTIFIER &&
                prevNode.treeParent.elementType == CLASS &&
                node.treeNext.elementType == PRIMARY_CONSTRUCTOR

            if (lfcount > 2 || eof || betweenClassAndPrimaryConstructor) {
                val split = text.split("\n")
                emit(node.startOffset + split[0].length + split[1].length + 2, "Needless blank line(s)", true)
                if (autoCorrect) {
                    val newText = buildString {
                        append(split.first())
                        append("\n")
                        if (!eof && !betweenClassAndPrimaryConstructor) append("\n")
                        append(split.last())
                    }
                    (node as LeafPsiElement).rawReplaceWithText(newText)
                }
            }
        }
    }
}
