package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.util.containers.Stack
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtTypeConstraintList
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class IndentationRule : Rule("indent"), Rule.Modifier.RestrictToRoot {
    private var indentSize = -1
    private var continuationIndentSize = -1
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val ec = EditorConfig.from(node as FileASTNode)
        indentSize = ec.indentSize
        continuationIndentSize = ec.continuationIndentSize

        if (indentSize <= 1) {
            return
        }

        exploreTree(node, emit, autoCorrect)
    }

    private fun exploreTree(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val scopeStack = Stack<IndentationScope>()
        val stack = Stack<ASTNode>()

        val scopeSet = setOf(
            IndentationScope.ExpressionScope(
                listOf(KtStubElementTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION),
                listOf(KtTokens.DOT, KtTokens.SAFE_ACCESS)
            ),
            IndentationScope.ExpressionScope(
                listOf(KtNodeTypes.BINARY_EXPRESSION),
                listOf(KtNodeTypes.OPERATION_REFERENCE)
            ),
            IndentationScope.InternalNodeScopeWithTriggerToken(
                KtStubElementTypes.PROPERTY,
                KtTokens.EQ
            ),
            IndentationScope.InternalNodeScopeWithTriggerToken(
                KtStubElementTypes.FUNCTION,
                KtTokens.EQ
            ),
            IndentationScope.InternalNodeScopeWithTriggerToken(
                KtNodeTypes.WHEN_ENTRY,
                KtTokens.ARROW
            ),
            IndentationScope.ParenScope(KtTokens.LBRACE, KtTokens.RBRACE),
            IndentationScope.ParenScope(KtTokens.LBRACKET, KtTokens.RBRACKET),
            IndentationScope.ParenScope(KtTokens.LPAR, KtTokens.RPAR),
            IndentationScope.ParenScope(KtTokens.LT, KtTokens.GT),
            IndentationScope.PropertyAccessorScope
        )

        stack.push(node)

        while (!stack.empty()) {
            val current = stack.pop()
            val children = current.children()

            val exitedScopesOnCurrentNode = mutableSetOf<IndentationScope>()

            while (
                !scopeStack.empty() &&
                !exitedScopesOnCurrentNode.contains(scopeStack.peek()) &&
                scopeStack.peek().isLeaving(current)
            ) {
                exitedScopesOnCurrentNode.add(scopeStack.pop())
            }

            scopeSet.forEach {
                if (it.isEntering(current)) {
                    scopeStack.push(it)
                }
            }


            if (children.none()) {
                if (current is PsiWhiteSpace &&
                    current.treeNext !is PsiComment &&
                    current.treeNext.firstChildNode !is PsiComment &&
                    current.treeNext.elementType != KtTokens.WHERE_KEYWORD &&
                    !current.isPartOf(PsiComment::class) &&
                    !current.isPartOf(KtTypeConstraintList::class)
                ) {
                    val continuationCount = scopeStack.count { it.isContinuation }
                    val normalIndentationCount = scopeStack.count { !it.isContinuation }
                    handleNewline(current, emit, autoCorrect, normalIndentationCount, continuationCount)

                    if (current.textContains('\t')) {
                        handleTab(current, emit, autoCorrect)
                    }
                }
            } else {
                children
                    .toList()
                    .reversed()
                    .forEach { stack.push(it) }
            }
        }
    }

    private fun handleNewline(
        node: PsiWhiteSpace,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        normalIndentationCount: Int,
        continuationIndentationCount: Int
    ) {
        val lines = node.text.split("\n")
        if (lines.size > 1) {
            val offset = node.startOffset + lines.first().length + 1

            lines.take(lines.count() - 1).forEach { indent ->
                if (indent != "") {
                    emit(
                        offset,
                        "Unexpected indentation (${indent.length}) (it should be 0)",
                        false
                    )
                }
            }

            val indent = lines.last()

            val totalIndentationSize = normalIndentationCount * indentSize +
                continuationIndentationCount * continuationIndentSize

            if (
            // parameter list wrapping enforced by ParameterListWrappingRule
                !node.isPartOf(KtParameterList::class) &&
                indent.length != totalIndentationSize
            ) {
                emit(
                    offset,
                    "Unexpected indentation (${indent.length}) (it should be $totalIndentationSize)",
                    false
                )
            }
        }
    }

    private fun handleTab(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        emit(
            node.startOffset + node.text.indexOf('\t'),
            "Unexpected Tab character(s)",
            true
        )

        if (autoCorrect) {
            val tabToSpaceString = node.text.replace("\t", " ".repeat(indentSize))
            (node as LeafPsiElement).rawReplaceWithText(tabToSpaceString)
        }
    }
}

sealed class IndentationScope(val isContinuation: Boolean = false) {
    abstract fun isEntering(node: ASTNode): Boolean
    abstract fun isLeaving(node: ASTNode): Boolean

    data class ExpressionScope(
        val elementTypes: List<IElementType>,
        val chainingElements: List<IElementType>
    ) : IndentationScope(isContinuation = true) {
        override fun isEntering(node: ASTNode): Boolean =
            this.elementTypes.contains(node.elementType) &&
                !this.elementTypes.contains(node.treeParent?.elementType)

        override fun isLeaving(node: ASTNode): Boolean {
            val isPrevTreeContainsExpression = this
                .elementTypes
                .any { node.treePrev?.findDeepChildByType(it) != null }

            val isPrevTreeExpression = this.elementTypes.contains(node.treePrev?.elementType)

            val currentAndNextAreNotChainingElement =
                !this.chainingElements.contains(node.elementType) &&
                    !this.chainingElements.contains(node.treeNext?.elementType)

            return (isPrevTreeExpression && currentAndNextAreNotChainingElement) ||
                (!isPrevTreeExpression && isPrevTreeContainsExpression)
        }
    }

    data class InternalNodeScopeWithTriggerToken(
        val elementType: IElementType,
        val trigger: KtToken
    ) : IndentationScope() {
        override fun isEntering(node: ASTNode): Boolean =
            node.elementType == this.elementType &&
                node.children().zip(node.children().drop(1))
                    .any { (a, b) ->
                        a.elementType == this.trigger &&
                            b is PsiWhiteSpace &&
                            b.textContains('\n')
                    }

        override fun isLeaving(node: ASTNode): Boolean =
            node.isSucceeding(this.elementType)
    }

    data class ParenScope(val startToken: KtToken, val endToken: KtToken) : IndentationScope() {
        override fun isEntering(node: ASTNode): Boolean {
            val lookAhead = node
                .psi
                .nextLeafs
                .takeWhile { !(it is PsiWhiteSpace && it.textContains('\n')) }

            val isLastOpenParen =
                listOf(
                    KtTokens.LPAR to KtTokens.RPAR,
                    KtTokens.LBRACKET to KtTokens.RBRACKET,
                    KtTokens.LBRACE to KtTokens.RBRACE
                )
                    .all { (open, close) ->
                        (lookAhead.find { it.node.elementType == open } == null ||
                            (lookAhead.find { it.node.elementType == open } != null &&
                                lookAhead.find { it.node.elementType == close } != null))
                    }

            return node.elementType == this.startToken && isLastOpenParen
        }

        override fun isLeaving(node: ASTNode): Boolean {
            val isNewline: (ASTNode?) -> Boolean = { it is PsiWhiteSpace && it.textContains('\n') }
            val isNewLineThenEndToken = isNewline(node) && node.isPreceding(this.endToken)
            val isEndTokenAfterNonNewLine = !isNewline(node.treePrev) && node.elementType == this.endToken

            return isNewLineThenEndToken || isEndTokenAfterNonNewLine
        }
    }

    object PropertyAccessorScope : IndentationScope() {
        override fun isEntering(node: ASTNode): Boolean {
            val firstPropAccessor = node
                .findOutermostConsecutiveParent(KtStubElementTypes.PROPERTY)
                ?.findChildByType(KtStubElementTypes.PROPERTY_ACCESSOR)

            return firstPropAccessor != null && node.treeNext == firstPropAccessor
        }

        override fun isLeaving(node: ASTNode): Boolean =
            node.isSucceeding(KtStubElementTypes.PROPERTY)
    }
}

private fun ASTNode.isSucceeding(elementType: IElementType) =
    this.treePrev?.elementType == elementType

private fun ASTNode.isPreceding(elementType: IElementType) =
    this.treeNext?.elementType == elementType

private fun ASTNode.findDeepChildByType(elementType: IElementType): ASTNode? =
    findChildByType(elementType) ?: if (!this.children().none()) {
        val targetParent = this.children().find {
            it.findDeepChildByType(elementType) != null
        }
        targetParent?.findDeepChildByType(elementType)
    } else {
        null
    }

private fun ASTNode.findOutermostConsecutiveParent(elementType: IElementType): ASTNode? =
    this
        .parents()
        .asSequence()
        .dropWhile { it.elementType != elementType }
        .takeWhile { it.elementType == elementType }
        .lastOrNull()
