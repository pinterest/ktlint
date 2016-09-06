package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtImportDirective

class NoWildcardImportsRule : Rule("no-wildcard-imports") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is LeafPsiElement && node.textMatches("*") && node.isPartOf(KtImportDirective::class)) {
            emit(node.startOffset, "Wildcard import", false)
        }
    }

}
