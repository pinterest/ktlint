package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class ImportOrderingRule : Rule("import-ordering") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.IMPORT_LIST) {
            val children = node.getChildren(null)
            if (children.isNotEmpty()) {
                val imports = children.filter { it.elementType == KtStubElementTypes.IMPORT_DIRECTIVE }
                val sortedImports = imports.sortedBy { it.text }
                if (imports != sortedImports) {
                    emit(node.startOffset, "Imports must be ordered in lexicographic order", true)
                    if (autoCorrect) {
                        node.removeRange(node.firstChildNode, node.lastChildNode.treeNext)
                        sortedImports.forEachIndexed { i, astNode ->
                            if (i > 0) {
                                node.addChild(PsiWhiteSpaceImpl("\n"), null)
                            }
                            node.addChild(astNode, null)
                        }
                    }
                }
            }
        }
    }
}
