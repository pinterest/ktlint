package com.github.shyiko.ktlint.ruleset.standard

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import kotlin.reflect.KClass

internal fun PsiElement.isPartOf(clazz: KClass<out PsiElement>) = getNonStrictParentOfType(clazz.java) != null
internal fun PsiElement.isPartOfString() = isPartOf(KtStringTemplateEntry::class)
internal fun PsiElement.prevLeaf(): PsiElement? = PsiTreeUtil.prevLeaf(this)
internal fun PsiElement.nextLeaf(): PsiElement? = PsiTreeUtil.nextLeaf(this)
internal fun ASTNode.visit(cb: (node: ASTNode) -> Unit) {
    cb(this)
    this.getChildren(null).forEach { it.visit(cb) }
}

internal fun <T> List<T>.head() = this.subList(0, this.size - 1)
internal fun <T> List<T>.tail() = this.subList(1, this.size)
