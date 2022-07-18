package com.pinterest.ktlint.core.ast

import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import kotlin.reflect.KClass
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.psiUtil.siblings

fun ASTNode.nextLeaf(includeEmpty: Boolean = false, skipSubtree: Boolean = false): ASTNode? {
    var n = if (skipSubtree) this.lastChildLeafOrSelf().nextLeafAny() else this.nextLeafAny()
    if (!includeEmpty) {
        while (n != null && n.textLength == 0) {
            n = n.nextLeafAny()
        }
    }
    return n
}

fun ASTNode.nextLeaf(p: (ASTNode) -> Boolean): ASTNode? {
    var n = this.nextLeafAny()
    while (n != null && !p(n)) {
        n = n.nextLeafAny()
    }
    return n
}

private fun ASTNode.nextLeafAny(): ASTNode? {
    var n = this
    if (n.firstChildNode != null) {
        do {
            n = n.firstChildNode
        } while (n.firstChildNode != null)
        return n
    }
    return n.nextLeafStrict()
}

private fun ASTNode.nextLeafStrict(): ASTNode? {
    val nextSibling: ASTNode? = treeNext
    if (nextSibling != null) {
        return nextSibling.firstChildLeafOrSelf()
    }
    return treeParent?.nextLeafStrict()
}

fun ASTNode.firstChildLeafOrSelf(): ASTNode {
    var n = this
    if (n.firstChildNode != null) {
        do {
            n = n.firstChildNode
        } while (n.firstChildNode != null)
        return n
    }
    return n
}

fun ASTNode.prevLeaf(includeEmpty: Boolean = false): ASTNode? {
    var n = this.prevLeafAny()
    if (!includeEmpty) {
        while (n != null && n.textLength == 0) {
            n = n.prevLeafAny()
        }
    }
    return n
}

fun ASTNode.prevLeaf(p: (ASTNode) -> Boolean): ASTNode? {
    var n = this.prevLeafAny()
    while (n != null && !p(n)) {
        n = n.prevLeafAny()
    }
    return n
}

private fun ASTNode.prevLeafAny(): ASTNode? {
    val prevSibling = treePrev
    if (prevSibling != null) {
        return treePrev.lastChildLeafOrSelf()
    }
    return treeParent?.prevLeafAny()
}

fun ASTNode.lastChildLeafOrSelf(): ASTNode {
    var n = this
    if (n.lastChildNode != null) {
        do {
            n = n.lastChildNode
        } while (n.lastChildNode != null)
        return n
    }
    return n
}

fun ASTNode.prevCodeLeaf(includeEmpty: Boolean = false): ASTNode? {
    var n = prevLeaf(includeEmpty)
    while (n != null && (n.elementType == WHITE_SPACE || n.isPartOfComment())) {
        n = n.prevLeaf(includeEmpty)
    }
    return n
}

fun ASTNode.nextCodeLeaf(includeEmpty: Boolean = false, skipSubtree: Boolean = false): ASTNode? {
    var n = nextLeaf(includeEmpty, skipSubtree)
    while (n != null && (n.elementType == WHITE_SPACE || n.isPartOfComment())) {
        n = n.nextLeaf(includeEmpty, skipSubtree)
    }
    return n
}

fun ASTNode.prevCodeSibling(): ASTNode? =
    prevSibling { it.elementType != WHITE_SPACE && !it.isPartOfComment() }

inline fun ASTNode.prevSibling(p: (ASTNode) -> Boolean): ASTNode? {
    var n = this.treePrev
    while (n != null) {
        if (p(n)) {
            return n
        }
        n = n.treePrev
    }
    return null
}

fun ASTNode.nextCodeSibling(): ASTNode? =
    nextSibling { it.elementType != WHITE_SPACE && !it.isPartOfComment() }

inline fun ASTNode.nextSibling(p: (ASTNode) -> Boolean): ASTNode? {
    var n = this.treeNext
    while (n != null) {
        if (p(n)) {
            return n
        }
        n = n.treeNext
    }
    return null
}

/**
 * @param elementType [ElementType].*
 */
fun ASTNode.parent(elementType: IElementType, strict: Boolean = true): ASTNode? {
    var n: ASTNode? = if (strict) this.treeParent else this
    while (n != null) {
        if (n.elementType == elementType) {
            return n
        }
        n = n.treeParent
    }
    return null
}

fun ASTNode.parent(p: (ASTNode) -> Boolean, strict: Boolean = true): ASTNode? {
    var n: ASTNode? = if (strict) this.treeParent else this
    while (n != null) {
        if (p(n)) {
            return n
        }
        n = n.treeParent
    }
    return null
}

/**
 * @param elementType [ElementType].*
 */
fun ASTNode.isPartOf(elementType: IElementType) =
    parent(elementType, strict = false) != null

fun ASTNode.isPartOf(klass: KClass<out PsiElement>): Boolean {
    var n: ASTNode? = this
    while (n != null) {
        if (klass.java.isInstance(n.psi)) {
            return true
        }
        n = n.treeParent
    }
    return false
}

public fun ASTNode.isPartOfCompositeElementOfType(iElementType: IElementType) =
    parent(findCompositeElementOfType(iElementType))?.elementType == iElementType

public fun findCompositeElementOfType(iElementType: IElementType): (ASTNode) -> Boolean =
    { it.elementType == iElementType || it !is CompositeElement }

fun ASTNode.isPartOfString() =
    parent(STRING_TEMPLATE, strict = false) != null

fun ASTNode?.isWhiteSpace() =
    this != null && elementType == WHITE_SPACE

fun ASTNode?.isWhiteSpaceWithNewline() =
    this != null && elementType == WHITE_SPACE && textContains('\n')

fun ASTNode?.isWhiteSpaceWithoutNewline() =
    this != null && elementType == WHITE_SPACE && !textContains('\n')

fun ASTNode.isRoot() = elementType == ElementType.FILE
fun ASTNode.isLeaf() = firstChildNode == null

fun ASTNode.isPartOfComment() = parent({ it.psi is PsiComment }, strict = false) != null

fun ASTNode.children() =
    generateSequence(firstChildNode) { node -> node.treeNext }

fun LeafElement.upsertWhitespaceBeforeMe(text: String): LeafElement {
    val s = treePrev
    return if (s != null && s.elementType == WHITE_SPACE) {
        (s.psi as LeafElement).rawReplaceWithText(text)
    } else {
        PsiWhiteSpaceImpl(text).also { w ->
            (psi as LeafElement).rawInsertBeforeMe(w)
        }
    }
}

fun LeafElement.upsertWhitespaceAfterMe(text: String): LeafElement {
    val s = treeNext
    return if (s != null && s.elementType == WHITE_SPACE) {
        (s.psi as LeafElement).rawReplaceWithText(text)
    } else {
        PsiWhiteSpaceImpl(text).also { w ->
            (psi as LeafElement).rawInsertAfterMe(w)
        }
    }
}

@Deprecated(message = "Marked for removal in Ktlint 0.48. See Ktlint 0.47.0 changelog for more information.")
fun ASTNode.visit(enter: (node: ASTNode) -> Unit) {
    enter(this)
    this.getChildren(null).forEach { it.visit(enter) }
}

@Deprecated(message = "Marked for removal in Ktlint 0.48. See Ktlint 0.47.0 changelog for more information.")
fun ASTNode.visit(enter: (node: ASTNode) -> Unit, exit: (node: ASTNode) -> Unit) {
    enter(this)
    this.getChildren(null).forEach { it.visit(enter, exit) }
    exit(this)
}

fun ASTNode.lineNumber(): Int? =
    this.psi.containingFile?.viewProvider?.document?.getLineNumber(this.startOffset)?.let { it + 1 }

val ASTNode.column: Int
    get() {
        var leaf = this.prevLeaf()
        var offsetToTheLeft = 0
        while (leaf != null) {
            if ((leaf.elementType == WHITE_SPACE || leaf.elementType == REGULAR_STRING_PART) && leaf.textContains('\n')) {
                offsetToTheLeft += leaf.textLength - 1 - leaf.text.lastIndexOf('\n')
                break
            }
            offsetToTheLeft += leaf.textLength
            leaf = leaf.prevLeaf()
        }
        return offsetToTheLeft + 1
    }

fun ASTNode.lineIndent(): String {
    var leaf = this.prevLeaf()
    while (leaf != null) {
        if (leaf.elementType == WHITE_SPACE && leaf.textContains('\n')) {
            return leaf.text.substring(leaf.text.lastIndexOf('\n') + 1)
        }
        leaf = leaf.prevLeaf()
    }
    return ""
}

/**
 *  Print content of a node and the element type of the node, its parent and its direct children. Utility is meant to
 *  be used during development only. Please do not remove.
 */
@Suppress("unused")
fun ASTNode.logStructure(): ASTNode =
    also {
        println("Processing ${text.replaceTabAndNewline()} : Type $elementType with parent ${treeParent?.elementType} ")
        children()
            .toList()
            .map {
                println("  ${it.text.replaceTabAndNewline()} : Type ${it.elementType}")
            }
    }

private fun String.replaceTabAndNewline(): String =
    replace("\t", "\\t").replace("\n", "\\n")

public fun containsLineBreakInRange(from: PsiElement, to: PsiElement): Boolean =
    from.siblings(forward = true, withItself = true)
        .takeWhile { !it.isEquivalentTo(to) }
        .any { it.textContains('\n') }
