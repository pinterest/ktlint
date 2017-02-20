package com.gihub.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class CapitalizedClassNamesRule : Rule("capitalized-class-name") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, message: String, canAutoCorrect: Boolean) -> Unit) {

        val type = node.elementType

        if (type == KtStubElementTypes.CLASS) {
            val identifier = node.getChildren(null).first { it.elementType == KtTokens.IDENTIFIER }
            if (identifier.text.toCharArray()[0].isLowerCase()) {
                emit(identifier.startOffset, "Class `${identifier.text}` should be capitalized", false)
            }
        }

    }
}
