package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VARARG_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.util.prefixIfNot
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import kotlin.reflect.KClass

public fun ASTNode.nextLeaf(
    includeEmpty: Boolean = false,
    skipSubtree: Boolean = false,
): ASTNode? {
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

public fun ASTNode.nextCodeLeaf(
    includeEmpty: Boolean = false,
    skipSubtree: Boolean = false,
): ASTNode? {
    var n = nextLeaf(includeEmpty, skipSubtree)
    while (n != null && (n.elementType == WHITE_SPACE || n.isPartOfComment())) {
        n = n.nextLeaf(includeEmpty, skipSubtree)
    }
    return n
}

public fun ASTNode.prevCodeSibling(): ASTNode? = prevSibling { it.elementType != WHITE_SPACE && !it.isPartOfComment() }

public inline fun ASTNode.prevSibling(predicate: (ASTNode) -> Boolean = { true }): ASTNode? {
    var n = this.treePrev
    while (n != null) {
        if (predicate(n)) {
            return n
        }
        n = n.treePrev
    }
    return null
}

public fun ASTNode.nextCodeSibling(): ASTNode? = nextSibling { it.elementType != WHITE_SPACE && !it.isPartOfComment() }

public inline fun ASTNode.nextSibling(predicate: (ASTNode) -> Boolean = { true }): ASTNode? {
    var n = this.treeNext
    while (n != null) {
        if (predicate(n)) {
            return n
        }
        n = n.treeNext
    }
    return null
}

/**
 * @param elementType [ElementType].*
 */
public fun ASTNode.parent(
    elementType: IElementType,
    strict: Boolean = true,
): ASTNode? {
    var n: ASTNode? = if (strict) this.treeParent else this
    while (n != null) {
        if (n.elementType == elementType) {
            return n
        }
        n = n.treeParent
    }
    return null
}

public fun ASTNode.parent(
    strict: Boolean = true,
    predicate: (ASTNode) -> Boolean,
): ASTNode? {
    var n: ASTNode? = if (strict) this.treeParent else this
    while (n != null) {
        if (predicate(n)) {
            return n
        }
        n = n.treeParent
    }
    return null
}

/**
 * @param elementType [ElementType].*
 */
public fun ASTNode.isPartOf(elementType: IElementType): Boolean = parent(elementType, strict = false) != null

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
    iElementType == findCompositeParentElementOfType(iElementType)?.elementType

public fun ASTNode.findCompositeParentElementOfType(iElementType: IElementType): ASTNode? =
    parent { it.elementType == iElementType || it !is CompositeElement }

public fun ASTNode.isPartOfString(): Boolean = parent(STRING_TEMPLATE, strict = false) != null

public fun ASTNode?.isWhiteSpace(): Boolean = this != null && elementType == WHITE_SPACE

public fun ASTNode?.isWhiteSpaceWithNewline(): Boolean = this != null && elementType == WHITE_SPACE && textContains('\n')

public fun ASTNode?.isWhiteSpaceWithoutNewline(): Boolean = this != null && elementType == WHITE_SPACE && !textContains('\n')

public fun ASTNode.isRoot(): Boolean = elementType == ElementType.FILE

public fun ASTNode.isLeaf(): Boolean = firstChildNode == null

/**
 * Check if the given [ASTNode] is a code leaf. E.g. it must be a leaf and may not be a whitespace or be part of a
 * comment.
 */
public fun ASTNode.isCodeLeaf(): Boolean = isLeaf() && !isWhiteSpace() && !isPartOfComment()

public fun ASTNode.isPartOfComment(): Boolean = parent(strict = false) { it.psi is PsiComment } != null

public fun ASTNode.children(): Sequence<ASTNode> = generateSequence(firstChildNode) { node -> node.treeNext }

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
        when (val prevSibling = prevSibling()) {
            null -> {
                // Never insert a whitespace element as first child node in a composite node. Instead, upsert just before the composite node
                treeParent.upsertWhitespaceBeforeMe(text)
            }

            is LeafElement -> {
                prevSibling.upsertWhitespaceAfterMe(text)
            }

            else -> {
                // Insert in between two composite nodes
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    treeParent.addChild(psiWhiteSpace.node, this)
                }
            }
        }
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
        when (val nextSibling = nextSibling()) {
            null -> {
                // Never insert a whitespace element as last child node in a composite node. Instead, upsert just after the composite node
                treeParent.upsertWhitespaceAfterMe(text)
            }

            is LeafElement -> {
                nextSibling.upsertWhitespaceBeforeMe(text)
            }

            else -> {
                // Insert in between two composite nodes
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    treeParent.addChild(psiWhiteSpace.node, nextSibling)
                }
            }
        }
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

/**
 * Get the current indentation of the line containing the [ASTNode]. By default, this indentation starts with a newline (\n) character.
 */
public fun ASTNode.indent(includeNewline: Boolean = true): String =
    leaves(forward = false)
        .firstOrNull { it.isWhiteSpaceWithNewline() }
        ?.text
        ?.substringAfterLast('\n')
        .orEmpty() // Fallback if node is not preceded by any newline character
        .applyIf(includeNewline) { prefixIfNot("\n") }

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

private fun String.replaceTabAndNewline(): String = replace("\t", "\\t").replace("\n", "\\n")

/**
 * Verifies that a leaf containing a newline exist in the closed range [from] - [to]. Also, returns true in case any of
 * the boundary nodes [from] or [to] contains a newline.
 */
public fun hasNewLineInClosedRange(
    from: ASTNode,
    to: ASTNode,
): Boolean = leavesInClosedRange(from, to).any { it.textContains('\n') }

/**
 * Verifies that no leaf contains a newline in the closed range [from] - [to]. Also, the boundary nodes [from] and [to]
 * should not contain a newline.
 */
public fun noNewLineInClosedRange(
    from: ASTNode,
    to: ASTNode,
): Boolean = leavesInClosedRange(from, to).none { it.textContains('\n') }

/**
 * Verifies that no leaf contains a newline in the open range [from] - [to]. This means that the boundary nodes are excluded from the range
 * in case they would happen to be a leaf node. In case [from] is a [CompositeElement] than the first leaf node in the sequence is the first
 * leaf node in this [CompositeElement]. In case [to] is a [CompositeElement] than the last node in the sequence is the last leaf node prior
 * to this [CompositeElement].
 */
public fun noNewLineInOpenRange(
    from: ASTNode,
    to: ASTNode,
): Boolean = leavesInOpenRange(from, to).none { it.textContains('\n') }

/**
 * Creates a sequence of leaf nodes in the open range [from] - [to]. This means that the boundary nodes are excluded
 * from the range in case they would happen to be a leaf node. In case [from] is a [CompositeElement] than the first
 * leaf node in the sequence is the first leaf node in this [CompositeElement]. In case [to] is a [CompositeElement]
 * than the last node in the sequence is the last leaf node prior to this [CompositeElement].
 */
public fun leavesInOpenRange(
    from: ASTNode,
    to: ASTNode,
): Sequence<ASTNode> =
    from
        .leaves()
        .takeWhile { it != to && it != to.lastChildLeafOrSelf() }

/**
 * Creates a sequence of leaf nodes in the closed range [from] - [to]. This means that the boundary nodes are included from the range in
 * case they would happen to be a leaf node. In case [from] is a [CompositeElement] than the first leaf node in the sequence is the first
 * leaf node in this [CompositeElement]. In case [to] is a [CompositeElement] than the last node in the sequence is the last leaf node of
 * this [CompositeElement].
 */
public fun leavesInClosedRange(
    from: ASTNode,
    to: ASTNode,
): Sequence<ASTNode> {
    val stopAtLeaf =
        to
            .lastChildLeafOrSelf()
            .nextLeaf()
    return from
        .firstChildLeafOrSelf()
        .leavesIncludingSelf()
        .takeWhile { it != stopAtLeaf }
}

public fun ASTNode.isValOrVarKeyword(): Boolean = elementType == VAL_KEYWORD || elementType == VAR_KEYWORD || elementType == VARARG_KEYWORD

/**
 * Creates a sequences of leaves including the [ASTNode] in case it is a [LeafElement] itself. By default, the leaves are traversed in
 * forward order. Setting [forward] to `false` changes this to traversal in backward direction.
 */
public fun ASTNode.leavesIncludingSelf(forward: Boolean = true): Sequence<ASTNode> {
    val sequence =
        if (isLeaf()) {
            sequenceOf(this)
        } else {
            emptySequence()
        }
    return sequence.plus(leaves(forward))
}

/**
 * Get all leaves on the same line as the given node including the whitespace indentation. Note that the whitespace indentation may start
 * with zero or more newline characters.
 */
public fun ASTNode.leavesOnLine(): Sequence<ASTNode> {
    val lastLeafOnLineOrNull = getLastLeafOnLineOrNull()
    return getFirstLeafOnLineOrSelf()
        .leavesIncludingSelf()
        .takeWhile { lastLeafOnLineOrNull == null || it.prevLeaf() != lastLeafOnLineOrNull }
}

internal fun ASTNode.getFirstLeafOnLineOrSelf() =
    prevLeaf { it.textContains('\n') || it.prevLeaf() == null }
        ?: this

internal fun ASTNode.getLastLeafOnLineOrNull() = nextLeaf { it.textContains('\n') }?.prevLeaf()

/**
 * Get the total length of all leaves on the same line as the given node including the whitespace indentation but excluding all leading
 * newline characters in the whitespace indentation.
 */
public fun ASTNode.lineLengthWithoutNewlinePrefix(): Int = leavesOnLine().lineLengthWithoutNewlinePrefix()

/**
 * Get the total length of all leaves in the sequence including the whitespace indentation but excluding all leading newline characters in
 * the whitespace indentation. The first leaf node in the sequence must be a white space starting with at least one newline.
 */
public fun Sequence<ASTNode>.lineLengthWithoutNewlinePrefix(): Int {
    require(first().text.startsWith('\n') || first().prevLeaf() == null) {
        "First node in sequence must be a whitespace containing a newline"
    }
    return joinToString(separator = "") { it.text }
        // If a line is preceded by a blank line then the ident contains multiple newline chars
        .dropWhile { it == '\n' }
        // In case the last element on the line would contain a newline then only include chars before that newline. Note that this should
        // not occur if the AST is parsed correctly
        .takeWhile { it != '\n' }
        .length
}

public fun ASTNode.afterCodeSibling(afterElementType: IElementType): Boolean = prevCodeSibling()?.elementType == afterElementType

public fun ASTNode.beforeCodeSibling(beforeElementType: IElementType): Boolean = nextCodeSibling()?.elementType == beforeElementType

public fun ASTNode.betweenCodeSiblings(
    afterElementType: IElementType,
    beforeElementType: IElementType,
): Boolean = afterCodeSibling(afterElementType) && beforeCodeSibling(beforeElementType)
