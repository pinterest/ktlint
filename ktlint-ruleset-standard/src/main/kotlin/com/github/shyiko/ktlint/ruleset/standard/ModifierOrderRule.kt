package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.ANNOTATION_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.COMPANION_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.CONST_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.DATA_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.ENUM_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.EXTERNAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.FINAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INFIX_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INLINE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INNER_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INTERNAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.LATEINIT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OPERATOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PRIVATE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PROTECTED_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PUBLIC_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.SEALED_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.SUSPEND_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.TAILREC_KEYWORD
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import java.util.Arrays

class ModifierOrderRule : Rule("modifier-order") {

    // subset of KtTokens.MODIFIER_KEYWORDS_ARRAY
    private val order = arrayOf(
        PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD,
        FINAL_KEYWORD, OPEN_KEYWORD, ABSTRACT_KEYWORD,
        SUSPEND_KEYWORD, TAILREC_KEYWORD,
        OVERRIDE_KEYWORD,
        CONST_KEYWORD, LATEINIT_KEYWORD,
        INNER_KEYWORD, EXTERNAL_KEYWORD,
        ENUM_KEYWORD, ANNOTATION_KEYWORD, SEALED_KEYWORD, DATA_KEYWORD,
        COMPANION_KEYWORD,
        INLINE_KEYWORD,
        // NOINLINE_KEYWORD, CROSSINLINE_KEYWORD, OUT_KEYWORD, IN_KEYWORD, VARARG_KEYWORD, REIFIED_KEYWORD
        INFIX_KEYWORD,
        OPERATOR_KEYWORD
        // HEADER_KEYWORD, IMPL_KEYWORD
    )
    private val tokenSet = TokenSet.create(*order)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.psi is KtDeclarationModifierList) {
            val modifierArr = node.getChildren(tokenSet)
            val sorted = modifierArr.copyOf().apply { sortWith(compareBy { order.indexOf(it.elementType) }) }
            if (!Arrays.equals(modifierArr, sorted)) {
                emit(node.startOffset, "Incorrect modifier order (should be \"${
                    sorted.map { it.text }.joinToString(" ")
                }\")", true)
                if (autoCorrect) {
                    modifierArr.forEachIndexed { i, n ->
                        // fixme: find a better way (node type is now potentially out of sync)
                        (n.psi as LeafPsiElement).rawReplaceWithText(sorted[i].text)
                    }
                }
            }
        }
    }
}
