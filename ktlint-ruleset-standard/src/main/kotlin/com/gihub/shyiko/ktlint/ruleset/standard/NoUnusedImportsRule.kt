package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class NoUnusedImportsRule : Rule("no-unused-imports") {

    private val operatorSet = setOf("unaryPlus", "unaryMinus", "not", "inc", "dec", "plus", "minus", "times", "div",
        "mod", "rangeTo", "contains", "get", "set", "invoke", "plusAssign", "minusAssign", "timesAssign", "divAssign",
        "modAssign", "equals", "compareTo")
    private val ref = mutableSetOf("*")

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            node.visit { node ->
                val type = node.elementType
                if ((type == KtNodeTypes.REFERENCE_EXPRESSION || type == KtNodeTypes.OPERATION_REFERENCE) &&
                    !node.psi.isPartOf(KtImportDirective::class)) {
                    ref.add(node.text.trim('`'))
                }
            }
        } else
        if (node.elementType == KtStubElementTypes.IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val name = importDirective.importPath?.importedName?.asString()
            if (name != null && !ref.contains(name) && !operatorSet.contains(name)) {
                emit(importDirective.startOffset, "Unused import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            }
        }
    }

    private fun ASTNode.visit(cb: (node: ASTNode) -> Unit) {
        cb(this)
        this.getChildren(null).forEach { it.visit(cb) }
    }

}
