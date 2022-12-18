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
import org.jetbrains.kotlin.psi.psiUtil.leaves

public fun ASTNode.nextLeaf(includeEmpty: Boolean = false, skipSubtree: Boolean = false): ASTNode? {
    var n = if (skipSubtree) this.lastChildLeafOrSelf().nextLeafAny() else this.nextLeafAny()
    if (!includeEmpty) {
        while (n != null && n.textLength == 0) {
            n = n.nextLeafAny()
        }
    }
    return n
}

public fun ASTNode.nextLeaf(p: (ASTNode) -> Boolean): ASTNode? {
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

public fun ASTNode.firstChildLeafOrSelf(): ASTNode {
    var n = this
    if (n.firstChildNode != null) {
        do {
            n = n.firstChildNode
        } while (n.firstChildNode != null)
        return n
    }
    return n
}

public fun ASTNode.prevLeaf(includeEmpty: Boolean = false): ASTNode? {
    var n = this.prevLeafAny()
    if (!includeEmpty) {
        while (n != null && n.textLength == 0) {
            n = n.prevLeafAny()
        }
    }
    return n
}

public fun ASTNode.prevLeaf(p: (ASTNode) -> Boolean): ASTNode? {
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

public fun ASTNode.lastChildLeafOrSelf(): ASTNode {
    var n = this
    if (n.lastChildNode != null) {
        do {
            n = n.lastChildNode
        } while (n.lastChildNode != null)
        return n
    }
    return n
}

public fun ASTNode.prevCodeLeaf(includeEmpty: Boolean = false): ASTNode? {
    var n = prevLeaf(includeEmpty)
    while (n != null && (n.elementType == WHITE_SPACE || n.isPartOfComment())) {
        n = n.prevLeaf(includeEmpty)
    }
    return n
}

public fun ASTNode.nextCodeLeaf(includeEmpty: Boolean = false, skipSubtree: Boolean = false): ASTNode? {
    var n = nextLeaf(includeEmpty, skipSubtree)
    while (n != null && (n.elementType == WHITE_SPACE || n.isPartOfComment())) {
        n = n.nextLeaf(includeEmpty, skipSubtree)
    }
    return n
}

public fun ASTNode.prevCodeSibling(): ASTNode? =
    prevSibling { it.elementType != WHITE_SPACE && !it.isPartOfComment() }

public inline fun ASTNode.prevSibling(p: (ASTNode) -> Boolean): ASTNode? {
    var n = this.treePrev
    while (n != null) {
        if (p(n)) {
            return n
        }
        n = n.treePrev
    }
    return null
}

public fun ASTNode.nextCodeSibling(): ASTNode? =
    nextSibling { it.elementType != WHITE_SPACE && !it.isPartOfComment() }

public inline fun ASTNode.nextSibling(p: (ASTNode) -> Boolean): ASTNode? {
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
public fun ASTNode.parent(elementType: IElementType, strict: Boolean = true): ASTNode? {
    var n: ASTNode? = if (strict) this.treeParent else this
    while (n != null) {
        if (n.elementType == elementType) {
            return n
        }
        n = n.treeParent
    }
    return null
}

// TODO in ktlint 0.49 deprecate and replace with "ASTNode.parent(strict: Boolean = true, p: (ASTNode) -> Boolean): ASTNode?"
public fun ASTNode.parent(p: (ASTNode) -> Boolean, strict: Boolean = true): ASTNode? {
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
public fun ASTNode.isPartOf(elementType: IElementType): Boolean =
    parent(elementType, strict = false) != null

public fun ASTNode.isPartOf(klass: KClass<out PsiElement>): Boolean {
    var n: ASTNode? = this
    while (n != null) {
        if (klass.java.isInstance(n.psi)) {
            return true
        }
        n = n.treeParent
    }
    return false
}

public fun ASTNode.isPartOfCompositeElementOfType(iElementType: IElementType): Boolean =
    parent(findCompositeElementOfType(iElementType))?.elementType == iElementType

public fun findCompositeElementOfType(iElementType: IElementType): (ASTNode) -> Boolean =
    { it.elementType == iElementType || it !is CompositeElement }

public fun ASTNode.isPartOfString(): Boolean =
    parent(STRING_TEMPLATE, strict = false) != null

public fun ASTNode?.isWhiteSpace(): Boolean =
    this != null && elementType == WHITE_SPACE

public fun ASTNode?.isWhiteSpaceWithNewline(): Boolean =
    this != null && elementType == WHITE_SPACE && textContains('\n')

public fun ASTNode?.isWhiteSpaceWithoutNewline(): Boolean =
    this != null && elementType == WHITE_SPACE && !textContains('\n')

public fun ASTNode.isRoot(): Boolean = elementType == ElementType.FILE
public fun ASTNode.isLeaf(): Boolean = firstChildNode == null

/**
 * Check if the given [ASTNode] is a code leaf. E.g. it must be a leaf and may not be a whitespace or be part of a
 * comment.
 */
public fun ASTNode.isCodeLeaf(): Boolean =
    isLeaf() && !isWhiteSpace() && !isPartOfComment()

public fun ASTNode.isPartOfComment(): Boolean =
    parent({ it.psi is PsiComment }, strict = false) != null

public fun ASTNode.children(): Sequence<ASTNode> =
    generateSequence(firstChildNode) { node -> node.treeNext }

@Deprecated(message = """Marked for removal in KtLint 0.49. See KDOC""")
/**
 * Marked for removal in KtLint 0.49.
 *
 * Use [ASTNode.upsertWhitespaceBeforeMe] which operates on the [ASTNode] instead of the [LeafElement]. The new method
 * handles more edge case and as of that a lot of code can be simplified.
 *
 * *Code using [LeafElement.upsertWhitespaceBeforeMe]*
 * ```
 * if (elementType == WHITE_SPACE) {
 *     (this as LeafPsiElement).rawReplaceWithText("\n${blockCommentNode.lineIndent()}")
 * } else {
 *     (this as LeafPsiElement).upsertWhitespaceBeforeMe("\n${blockCommentNode.lineIndent()}")
 * }
 *  ```
 * *Code using [ASTNode.upsertWhitespaceBeforeMe]*
 * ```
 * this.upsertWhitespaceBeforeMe(text)
 *  ```
 */
public fun LeafElement.upsertWhitespaceBeforeMe(text: String): LeafElement {
    val s = treePrev
    return if (s != null && s.elementType == WHITE_SPACE) {
        (s.psi as LeafElement).rawReplaceWithText(text)
    } else {
        PsiWhiteSpaceImpl(text).also { w ->
            (psi as LeafElement).rawInsertBeforeMe(w)
        }
    }
}

@Deprecated(
    message =
    "Marked for removal in KtLint 0.49. The new insertOrReplaceWhitespaceAfterMe is more versatile as it " +
        "operates on an AstNode instead of a LeafElement. In a lot of cases the code can be simplified as it is " +
        "no longer needed to check whether the current node is already a whitespace or a leaf element before " +
        "calling this method or the rawReplaceWithText.",
    ReplaceWith("insertOrReplaceWhitespaceBeforeMe"),
)
public fun LeafElement.upsertWhitespaceAfterMe(text: String): LeafElement {
    val s = treeNext
    return if (s != null && s.elementType == WHITE_SPACE) {
        (s.psi as LeafElement).rawReplaceWithText(text)
    } else {
        PsiWhiteSpaceImpl(text).also { w ->
            (psi as LeafElement).rawInsertAfterMe(w)
        }
    }
}

/**
 * Updates or inserts a new whitespace element with [text] before the given node. If the node itself is a whitespace
 * then its contents is replaced with [text]. If the node is a (nested) composite element, the whitespace element is
 * added after the previous leaf node.
 */
public fun ASTNode.upsertWhitespaceBeforeMe(text: String) {
    if (this is LeafElement) {
        if (this.elementType == WHITE_SPACE) {
            return replaceWhitespaceWith(text)
        }
        val previous = treePrev ?: prevLeaf()
        if (previous != null && previous.elementType == WHITE_SPACE) {
            previous.replaceWhitespaceWith(text)
        } else {
            PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                (psi as LeafElement).rawInsertBeforeMe(psiWhiteSpace)
            }
        }
    } else {
        val prevLeaf =
            requireNotNull(prevLeaf()) {
                "Can not upsert a whitespace if the first node is a non-leaf node"
            }
        prevLeaf.upsertWhitespaceAfterMe(text)
    }
}

private fun ASTNode.replaceWhitespaceWith(text: String) {
    require(this.elementType == WHITE_SPACE)
    if (this.text != text) {
        (this.psi as LeafElement).rawReplaceWithText(text)
    }
}

/**
 * Updates or inserts a new whitespace element with [text] after the given node. If the node itself is a whitespace
 * then its contents is replaced with [text]. If the node is a (nested) composite element, the whitespace element is
 * added after the last child leaf.
 */
public fun ASTNode.upsertWhitespaceAfterMe(text: String) {
    if (this is LeafElement) {
        if (this.elementType == WHITE_SPACE) {
            return replaceWhitespaceWith(text)
        }
        val next = treeNext ?: nextLeaf()
        if (next != null && next.elementType == WHITE_SPACE) {
            next.replaceWhitespaceWith(text)
        } else {
            PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                (psi as LeafElement).rawInsertAfterMe(psiWhiteSpace)
            }
        }
    } else {
        lastChildLeafOrSelf().upsertWhitespaceAfterMe(text)
    }
}

public val ASTNode.column: Int
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

public fun ASTNode.lineIndent(): String {
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
public fun ASTNode.logStructure(): ASTNode =
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

/**
 * Verifies that a whitespace leaf containing a newline exist in the closed range [from] - [to]. Also, returns true in
 * case any of the boundary nodes [from] or [to] is a whitespace leaf containing a newline.
 */
@Deprecated(
    message = "Marked for removal in KtLint 0.49",
    replaceWith = ReplaceWith("hasNewLineInClosedRange(from, to)"),
)
public fun hasWhiteSpaceWithNewLineInClosedRange(from: ASTNode, to: ASTNode): Boolean =
    from.isWhiteSpaceWithNewline() ||
        leavesInOpenRange(from, to).any { it.isWhiteSpaceWithNewline() } ||
        to.isWhiteSpaceWithNewline()

/**
 * Verifies that a leaf containing a newline exist in the closed range [from] - [to]. Also, returns true in case any of
 * the boundary nodes [from] or [to] contains a newline.
 */
public fun hasNewLineInClosedRange(from: ASTNode, to: ASTNode): Boolean =
    from.isWhiteSpaceWithNewline() ||
        leavesInOpenRange(from, to).any { it.textContains('\n') } ||
        to.isWhiteSpaceWithNewline()

/**
 * Verifies that no whitespace leaf contains a newline in the closed range [from] - [to]. Also, the boundary nodes
 * [from] and [to] should not be a whitespace leaf containing a newline.
 */
@Deprecated(
    message = "Marked for removal in KtLint 0.49",
    replaceWith = ReplaceWith("noNewLineInClosedRange(from, to)"),
)
public fun noWhiteSpaceWithNewLineInClosedRange(from: ASTNode, to: ASTNode): Boolean =
    !from.isWhiteSpaceWithNewline() &&
        leavesInOpenRange(from, to).none { it.isWhiteSpaceWithNewline() } &&
        !to.isWhiteSpaceWithNewline()

/**
 * Verifies that no leaf contains a newline in the closed range [from] - [to]. Also, the boundary nodes [from] and [to]
 * should not contain a newline.
 */
public fun noNewLineInClosedRange(from: ASTNode, to: ASTNode): Boolean =
    !from.isWhiteSpaceWithNewline() &&
        leavesInOpenRange(from, to).none { it.textContains('\n') } &&
        !to.isWhiteSpaceWithNewline()

/**
 * Creates a sequence of leaf nodes in the open range [from] - [to]. This means that the boundary nodes are excluded
 * from the range in case they would happen to be a leaf node. In case [from] is a [CompositeElement] than the first
 * leaf node in the sequence is the first leaf node in this [CompositeElement]. In case [to] is a [CompositeElement]
 * than the last node in the sequence is the last leaf node prior to this [CompositeElement].
 */
public fun leavesInOpenRange(from: ASTNode, to: ASTNode): Sequence<ASTNode> =
    from
        .leaves()
        .takeWhile { it != to && it != to.firstChildNode }
