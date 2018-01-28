package com.github.shyiko.ktlint.ruleset.standard

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import kotlin.reflect.KClass

internal fun PsiElement.isPartOf(clazz: KClass<out PsiElement>) = getNonStrictParentOfType(clazz.java) != null
internal fun PsiElement.isPartOfString() = isPartOf(KtStringTemplateEntry::class)

internal fun <T> List<T>.head() = this.subList(0, this.size - 1)
internal fun <T> List<T>.tail() = this.subList(1, this.size)

internal fun ASTNode.calculatePreviousIndent(): Int {
    val parentNode = this.treeParent?.psi
    var prevIndent = 0
    var prevSibling = parentNode
    var prevSpaceIsFound = false
    while (prevSibling != null && !prevSpaceIsFound) {
        val nextNode = prevSibling.nextSibling?.node?.elementType
        if (prevSibling is PsiWhiteSpace
            && nextNode != KtStubElementTypes.TYPE_REFERENCE
            && nextNode != KtStubElementTypes.SUPER_TYPE_LIST
            && nextNode != KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL) {
            val prevLines = prevSibling.text.split('\n')
            if (prevLines.size > 1) {
                prevIndent = prevLines.last().length
                prevSpaceIsFound = true
            }
        }
        prevSibling = if (prevSpaceIsFound) {
            null
        } else {
            if (prevSibling.prevSibling != null) {
                prevSibling.prevSibling
            } else {
                prevSibling.parent
            }
        }
    }
    return prevIndent
}
