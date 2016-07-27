package com.github.shyiko.ktlint.rule

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtImportDirective

class NoWildcardImportsRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        if (node is LeafPsiElement && node.textMatches("*") && node.isPartOf(KtImportDirective::class)) {
            emit(RuleViolation(node.startOffset, "Wildcard import", correct))
        }
    }

}
