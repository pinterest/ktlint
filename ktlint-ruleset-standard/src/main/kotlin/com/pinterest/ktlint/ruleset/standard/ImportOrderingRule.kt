package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * Alphabetical with capital letters before lower case letters (e.g. Z before a).
 * No blank lines between major groups (android, com, junit, net, org, java, javax).
 * Single group regardless of import type.
 *
 * https://developer.android.com/kotlin/style-guide#import_statements
 */
class ImportOrderingRule : Rule("import-ordering") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == ElementType.IMPORT_LIST) {
            val children = node.getChildren(null)
            if (children.isNotEmpty()) {
                val imports = children.filter { it.elementType == ElementType.IMPORT_DIRECTIVE }
                val hasComments = children.find { it.elementType == ElementType.BLOCK_COMMENT || it.elementType == ElementType.EOL_COMMENT } != null
                val sortedImports = imports.sortedBy { it.text }.distinctBy { it.text }
                val canAutoCorrect = !hasComments
                if (imports != sortedImports || hasTooMuchWhitespace(children)) {
                    val additionalMessage = if (!canAutoCorrect) {
                        " -- no autocorrection due to comments in the import list"
                    } else {
                        ""
                    }
                    emit(node.startOffset, "Imports must be ordered in lexicographic order without any empty lines in-between$additionalMessage", canAutoCorrect)
                    if (autoCorrect && canAutoCorrect) {
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
