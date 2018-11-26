package com.github.shyiko.ktlint.ruleset.standard

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import kotlin.reflect.KClass

internal fun PsiElement.isPartOf(clazz: KClass<out PsiElement>) = getNonStrictParentOfType(clazz.java) != null
internal fun PsiElement.isPartOfString() = isPartOf(KtStringTemplateEntry::class)
internal fun PsiElement.isPartOfMultiLineString(): Boolean {
    val stringTemplate = getNonStrictParentOfType(KtStringTemplateExpression::class.java)
    return stringTemplate != null &&
        stringTemplate.firstChild.textMatches("\"\"\"") &&
        stringTemplate.children.any { it.textMatches("\n") }
}
internal fun PsiElement.prevLeaf(): PsiElement? = PsiTreeUtil.prevLeaf(this)
internal fun PsiElement.nextLeaf(): PsiElement? = PsiTreeUtil.nextLeaf(this)
internal fun PsiElement.nextLeafIgnoringWhitespaceAndComments() =
    this.nextLeaf { it.node.elementType != KtTokens.WHITE_SPACE && !it.isPartOf(PsiComment::class) }
internal fun PsiElement.prevLeafIgnoringWhitespaceAndComments() =
    this.prevLeaf { it.node.elementType != KtTokens.WHITE_SPACE && !it.isPartOf(PsiComment::class) }
internal fun ASTNode.visit(cb: (node: ASTNode) -> Unit) {
    cb(this)
    this.getChildren(null).forEach { it.visit(cb) }
}

internal fun <T> List<T>.head() = this.subList(0, this.size - 1)
internal fun <T> List<T>.tail() = this.subList(1, this.size)

internal val PsiElement.column: Int
    get() {
        var leaf = PsiTreeUtil.prevLeaf(this)
        var offsetToTheLeft = 0
        while (leaf != null) {
            if (leaf.node.elementType == KtTokens.WHITE_SPACE && leaf.textContains('\n')) {
                offsetToTheLeft += leaf.textLength - 1 - leaf.text.lastIndexOf('\n')
                break
            }
            offsetToTheLeft += leaf.textLength
            leaf = PsiTreeUtil.prevLeaf(leaf)
        }
        return offsetToTheLeft + 1
    }
internal fun PsiElement.lineIndent(): String {
    var leaf = PsiTreeUtil.prevLeaf(this)
    while (leaf != null) {
        if (leaf.node.elementType == KtTokens.WHITE_SPACE && leaf.textContains('\n')) {
            return leaf.text.substring(leaf.text.lastIndexOf('\n') + 1)
        }
        leaf = PsiTreeUtil.prevLeaf(leaf)
    }
    return ""
}
