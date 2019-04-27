package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class ArgumentListWrappingRule : Rule("argument-list-wrapping") {

    private var indentSize = -1
    private var maxLineLength = -1

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val ec = EditorConfig.from(node as FileASTNode)
            indentSize = ec.indentSize
            maxLineLength = ec.maxLineLength
            return
        }
        if (indentSize <= 0) {
            return
        }
        if (node.elementType == KtNodeTypes.VALUE_ARGUMENT_LIST &&
            node.getChildren(TokenSet.create(KtNodeTypes.VALUE_ARGUMENT)).size > 1) {
            // each parameter should be on a separate line if
            // - at least one of the parameters is
            // - maxLineLength exceeded (and separating parameters with \n would actually help)
            // in addition, "(" and ")" must be on separates line if any of the parameters are (otherwise on the same)
            val exceedsMaxLength = maxLineLength > -1 && (node.psi.column - 1 + node.textLength) > maxLineLength
            val putParametersOnSeparateLines = node.textContains('\n') || exceedsMaxLength
            if (putParametersOnSeparateLines) {
                // aiming for
                // ... LPAR
                // <line indent + indentSize> VALUE_PARAMETER...
                // <line indent> RPAR
                val indent = "\n" + node.psi.lineIndent()
                val paramIndent = indent + " ".repeat(indentSize) // single indent as recommended by Jetbrains/Google
                nextChild@ for (child in node.children()) {
                    when (child.elementType) {
                        KtTokens.LPAR -> {
                            val nextLeaf = child.psi.nextLeaf()
                            if (nextLeaf !is PsiWhiteSpace || !nextLeaf.textContains('\n')) {
                                emit(child.startOffset, """Missing newline after "("""", true)
                                if (autoCorrect) {
                                    node.addChild(PsiWhiteSpaceImpl(paramIndent), child.treeNext)
                                }
                            }
                        }
                        KtNodeTypes.VALUE_ARGUMENT,
                        KtTokens.RPAR -> {
                            var paramInnerIndentAdjustment = 0
                            val prevLeaf = child.psi.prevLeaf()
                            val intendedIndent = if (child.elementType == KtNodeTypes.VALUE_ARGUMENT) {
                                paramIndent
                            } else {
                                indent
                            }
                            val issueApplies = child.elementType == KtTokens.RPAR || exceedsMaxLength
                            if (prevLeaf is PsiWhiteSpace) {
                                val spacing = prevLeaf.text
                                val cut = spacing.lastIndexOf("\n")
                                if (cut > -1) {
                                    val childIndent = spacing.substring(cut)
                                    if (childIndent == intendedIndent) {
                                        continue@nextChild
                                    }
                                    emit(
                                        child.startOffset,
                                        "Unexpected indentation" +
                                            " (expected ${intendedIndent.length - 1}, actual ${childIndent.length - 1})",
                                        true
                                    )
                                } else if (issueApplies) {
                                    emit(child.startOffset, errorMessage(child), true)
                                }
                                if (autoCorrect && (cut > -1 || issueApplies)) {
                                    val adjustedIndent = (if (cut > -1) spacing.substring(0, cut) else "") + intendedIndent
                                    paramInnerIndentAdjustment = adjustedIndent.length - prevLeaf.textLength
                                    (prevLeaf as LeafPsiElement).rawReplaceWithText(adjustedIndent)
                                }
                            } else if (issueApplies) {
                                emit(child.startOffset, errorMessage(child), true)
                                if (autoCorrect) {
                                    paramInnerIndentAdjustment = intendedIndent.length - child.psi.column
                                    node.addChild(PsiWhiteSpaceImpl(intendedIndent), child)
                                }
                            }
                            if (paramInnerIndentAdjustment != 0 &&
                                child.elementType == KtNodeTypes.VALUE_ARGUMENT) {
                                child.visit { n ->
                                    if (n.elementType == KtTokens.WHITE_SPACE && n.textContains('\n')) {
                                        val split = n.text.split("\n")
                                        (n.psi as LeafElement).rawReplaceWithText(split.joinToString("\n") {
                                            if (paramInnerIndentAdjustment > 0) {
                                                it + " ".repeat(paramInnerIndentAdjustment)
                                            } else {
                                                it.substring(0, Math.max(it.length + paramInnerIndentAdjustment, 0))
                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun errorMessage(node: ASTNode) =
        when (node.elementType) {
            KtNodeTypes.VALUE_ARGUMENT -> "Arguments exceed maximum line length (can be split)"
            KtTokens.RPAR -> """Missing newline before ")""""
            else -> throw UnsupportedOperationException()
        }
}
