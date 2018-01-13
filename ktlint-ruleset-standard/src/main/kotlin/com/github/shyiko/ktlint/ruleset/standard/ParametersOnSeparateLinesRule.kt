package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.IndentationConfig
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class ParametersOnSeparateLinesRule : Rule(RULE_ID) {
    private var indentConfig = IndentationConfig(-1, -1, true)
    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            indentConfig = IndentationConfig.create(node)
            return
        }
        val previousIndent: Int by lazy { node.calculatePreviousIndent() }
        val parentParameterList = node.treeParent
        if (parentParameterList?.elementType == KtStubElementTypes.VALUE_PARAMETER_LIST
            //do not enforce lambda parameters
            && parentParameterList.treeParent?.elementType != KtNodeTypes.FUNCTION_LITERAL) {
            val parentFile = TreeUtil.findParent(node, KtStubElementTypes.FILE)
            if (shouldWrap(parentFile, parentParameterList)) {
                if (node.elementType == KtStubElementTypes.VALUE_PARAMETER) {
                    if (node.treePrev !is PsiWhiteSpace) {
                        emit(node.startOffset, MISSED_NEW_LINE_ERROR, true)
                        if (autoCorrect) {
                            (parentParameterList as CompositeElement).addLeaf(
                                KtTokens.WHITE_SPACE,
                                "\n" + " ".repeat(previousIndent + indentConfig.regular),
                                node)
                        }
                    } else {
                        val prevText = node.treePrev.text
                        if (!prevText.startsWith("\n")) {
                            emit(node.startOffset, MISSED_NEW_LINE_ERROR, true)
                            if (autoCorrect) {
                                (node.treePrev as LeafPsiElement)
                                    .rawReplaceWithText("\n" + " ".repeat(previousIndent + indentConfig.regular))
                            }
                        } else if (prevText.length - previousIndent - 1 != indentConfig.regular) {
                            emit(node.startOffset,
                                "Unexpected indentation for parameter ${prevText.length - previousIndent - 1} (should be ${indentConfig.regular})",
                                true)
                            if (autoCorrect) {
                                (node.treePrev as LeafPsiElement)
                                    .rawReplaceWithText("\n" + " ".repeat(previousIndent + indentConfig.regular))
                            }
                        }
                    }
                }
                if (node.elementType == KtTokens.RPAR) {
                    if (node.treePrev !is PsiWhiteSpace) {
                        emit(node.startOffset, PARENTHESES_NEW_LINE_ERROR, true)
                        if (autoCorrect) {
                            (parentParameterList as CompositeElement).addLeaf(
                                KtTokens.WHITE_SPACE,
                                "\n" + " ".repeat(previousIndent),
                                node)
                        }
                    } else {
                        val prevText = node.treePrev.text
                        if (!prevText.startsWith("\n")) {
                            emit(node.startOffset, MISSED_NEW_LINE_ERROR, true)
                            if (autoCorrect) {
                                (node.treePrev as LeafPsiElement)
                                    .rawReplaceWithText("\n" + " ".repeat(previousIndent))
                            }
                        } else if (prevText.length - previousIndent - 1 != 0) {
                            emit(node.startOffset,
                                "Unexpected indentation for parameter ${prevText.length - previousIndent - 1} (should be ${indentConfig.regular})",
                                true)
                            if (autoCorrect) {
                                (node.treePrev as LeafPsiElement)
                                    .rawReplaceWithText("\n" + " ".repeat(previousIndent))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shouldWrap(parentFile: ASTNode?, parentParameterList: ASTNode): Boolean {
        return parentFile != null && parentParameterList.textRange.substring(parentFile.text).contains("\n")
    }


    companion object {
        const val RULE_ID = "parameters-on-separate-lines"
        private const val MISSED_NEW_LINE_ERROR = "Parameter should be on separate line with indentation"
        private const val PARENTHESES_NEW_LINE_ERROR = "Parentheses should be on new line"
    }
}

