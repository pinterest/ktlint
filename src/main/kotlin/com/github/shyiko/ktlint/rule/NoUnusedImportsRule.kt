package com.github.shyiko.ktlint.rule

import com.github.shyiko.ktlint.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class NoUnusedImportsRule : Rule {

    private val ref = mutableSetOf("*")

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            node.visit { node ->
                if (node.elementType == KtStubElementTypes.REFERENCE_EXPRESSION) {
                    if (node.psi.getNonStrictParentOfType(KtImportDirective::class.java) == null) {
                        ref.add(node.text)
                    }
                }
            }
        } else
        if (node.elementType == KtStubElementTypes.IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val name = importDirective.importPath?.importedName?.asString()
            if (name != null && !ref.contains(name)) {
                emit(RuleViolation(importDirective.startOffset, "Unused import", correct))
                if (correct) {
                    importDirective.delete()
                }
            }
        }
    }

}
