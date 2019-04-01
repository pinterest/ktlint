package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.github.shyiko.ktlint.core.ast.ElementType.IMPORT_LIST
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * Alphabetical with capital letters before lower case letters (e.g. Z before a).
 * No blank lines between major groups (android, com, junit, net, org, java, javax).
 * Single group regardless of import type.
 */
class ImportOrderingRule : Rule("import-ordering") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == IMPORT_LIST) {
            val children = node.getChildren(null)
            if (children.isNotEmpty()) {
                val imports = children.filter { it.elementType == IMPORT_DIRECTIVE }
                val sortedImports = imports.sortedBy { it.text }
                if (imports != sortedImports || hasTooMuchWhitespace(children)) {
                    emit(node.startOffset, "Imports must be ordered in lexicographic order without any empty lines in-between", true)
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

    private fun hasTooMuchWhitespace(nodes: Array<ASTNode>): Boolean {
        return nodes.any { it is PsiWhiteSpace && (it as PsiWhiteSpace).text != "\n" }
    }
}
