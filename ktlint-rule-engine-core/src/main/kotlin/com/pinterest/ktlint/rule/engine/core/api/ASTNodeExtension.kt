package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VARARG_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.util.prefixIfNot
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * In Ktlint 2.0 all ASTNode extensions functions will be replaced with property access where applicable. For now (ktlint 1.x) those
 * property accessors are prefixed with `_` to avoid name clashes with the functions. You are encouraged to replace the deprecated
 * functions with the accessors prefixed with `_` with use of the IntelliJ IDEA functionality. In Ktlint 2.0 the functions will be removed,
 * and the prefix `_` will be removed from the accessors.
 */

@Deprecated(
    "Marked for removal in KtLint 2.0. Replace calls to 'nextLeaf()', 'nextLeaf(false)', and 'nextLeaf(false, false)' with " +
        "property accessor 'nextLeaf'. For any situation, use 'nextLeaf((ASTNode) -> Boolean)'.",
)
public fun ASTNode.nextLeaf(
    includeEmpty: Boolean = false,
    skipSubtree: Boolean = false,
): ASTNode? {
    var n = if (skipSubtree) lastChildLeafOrSelf20.nextLeafAny else this.nextLeafAny
    if (!includeEmpty) {
        while (n != null && n.textLength == 0) {
            n = n.nextLeafAny
        }
    }
    return n
}

public val ASTNode.nextLeaf
    get(): ASTNode? {
        var node = this.nextLeafAny
        while (node != null && node.textLength == 0) {
            node = node.nextLeafAny
        }
        return node
    }

public fun ASTNode.nextLeaf(p: (ASTNode) -> Boolean): ASTNode? {
    var n = this.nextLeafAny
    while (n != null && !p(n)) {
        n = n.nextLeafAny
    }
    return n
}

private val ASTNode.nextLeafAny
    get(): ASTNode? {
        if (firstChildNode == null) {
            return nextLeafStrict
        }
        var node = this
        while (node.firstChildNode != null) {
            node = node.firstChildNode
        }
        return node
    }

private val ASTNode.nextLeafStrict
    get(): ASTNode? = nextSibling20?.firstChildLeafOrSelf20 ?: parent?.nextLeafStrict

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("firstChildLeafOrSelf20"),
)
public fun ASTNode.firstChildLeafOrSelf(): ASTNode = firstChildLeafOrSelf20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.firstChildLeafOrSelf20
    get(): ASTNode {
        var node = this
        while (node.firstChildNode != null) {
            node = node.firstChildNode
        }
        return node
    }

@Deprecated(
    "Marked for removal in KtLint 2.0. Replace calls to 'prevLeaf()', and 'prevLeaf(false)' with property accessor 'prevLeaf'. For any " +
        "situation, use 'prevLeaf((ASTNode) -> Boolean)'.",
    replaceWith = ReplaceWith("prevLeaf"),
)
public fun ASTNode.prevLeaf(includeEmpty: Boolean = false): ASTNode? {
    var n = this.prevLeafAny
    if (!includeEmpty) {
        while (n != null && n.textLength == 0) {
            n = n.prevLeafAny
        }
    }
    return n
}

public val ASTNode.prevLeaf
    get(): ASTNode? {
        var node = this.prevLeafAny
        while (node != null && node.textLength == 0) {
            node = node.prevLeafAny
        }
        return node
    }

public fun ASTNode.prevLeaf(predicate: (ASTNode) -> Boolean): ASTNode? {
    var node = this.prevLeafAny
    while (node != null && !predicate(node)) {
        node = node.prevLeafAny
    }
    return node
}

private val ASTNode.prevLeafAny
    get(): ASTNode? = prevSibling20?.lastChildLeafOrSelf20 ?: parent?.prevLeafAny

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("lastChildLeafOrSelf20"),
)
public fun ASTNode.lastChildLeafOrSelf(): ASTNode = lastChildLeafOrSelf20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.lastChildLeafOrSelf20
    get(): ASTNode {
        var node = this
        while (node.lastChildNode != null) {
            node = node.lastChildNode
        }
        return node
    }

public val ASTNode.isCode
    get() = !isWhiteSpace20 && !isPartOfComment20

@Deprecated(
    "Marked for removal in KtLint 2.0. Replace calls to 'prevCodeLeaf()', and 'prevCodeLeaf(false)' with property accessor " +
        "'prevCodeLeaf'. For any other situation, use 'prevCodeLeaf((ASTNode) -> Boolean)'.",
)
public fun ASTNode.prevCodeLeaf(includeEmpty: Boolean = false): ASTNode? {
    var n = prevLeaf(includeEmpty)
    while (n != null && !n.isCode) {
        n = n.prevLeaf(includeEmpty)
    }
    return n
}

public val ASTNode.prevCodeLeaf
    get(): ASTNode? {
        var node = prevLeaf
        while (node != null && !node.isCode) {
            node = node.prevLeaf
        }
        return node
    }

@Deprecated(
    "Marked for removal in KtLint 2.0. Replace calls to 'nextCodeLeaf()', 'nextCodeLeaf(false)', and 'nextCodeLeaf(false, false)' with " +
        "property accessor 'nextCodeLeaf'. For any other situation, use 'nextCodeLeaf((ASTNode) -> Boolean)'.",
)
public fun ASTNode.nextCodeLeaf(
    includeEmpty: Boolean = false,
    skipSubtree: Boolean = false,
): ASTNode? {
    var n = nextLeaf
    while (n != null && !n.isCode) {
        n = n.nextLeaf
    }
    return n
}

public val ASTNode.nextCodeLeaf
    get(): ASTNode? {
        var node = nextLeaf
        while (node != null && !node.isCode) {
            node = node.nextLeaf
        }
        return node
    }

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("prevCodeSibling20"),
)
public fun ASTNode.prevCodeSibling(): ASTNode? = prevCodeSibling20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.prevCodeSibling20
    get(): ASTNode? = prevSibling { it.isCode }

public inline fun ASTNode.prevSibling(predicate: (ASTNode) -> Boolean = { true }): ASTNode? {
    var node = this.treePrev
    while (node != null) {
        if (predicate(node)) {
            return node
        }
        node = node.treePrev
    }
    return null
}

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("prevSibling20"),
)
public fun ASTNode.prevSibling(): ASTNode? = prevSibling20

public inline val ASTNode.prevSibling20
    get(): ASTNode? = treePrev

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("nextCodeSibling20"),
)
// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public fun ASTNode.nextCodeSibling(): ASTNode? = nextCodeSibling20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.nextCodeSibling20
    get(): ASTNode? = nextSibling { it.isCode }

public inline fun ASTNode.nextSibling(predicate: (ASTNode) -> Boolean): ASTNode? {
    var node = this.treeNext
    while (node != null) {
        if (predicate(node)) {
            return node
        }
        node = node.treeNext
    }
    return null
}

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("nextSibling20"),
)
public fun ASTNode.nextSibling(): ASTNode? = nextSibling20

public inline val ASTNode.nextSibling20
    get(): ASTNode? = treeNext

public inline val ASTNode.parent
    get(): ASTNode? = treeParent

/**
 * @param elementType [ElementType].*
 */
@Deprecated(
    "Marked for removal in Ktlint 2.0",
    replaceWith = ReplaceWith("parent(elementType)"),
)
public fun ASTNode.parent(
    elementType: IElementType,
    strict: Boolean = true,
): ASTNode? {
    var n: ASTNode? = if (strict) this.parent else this
    while (n != null) {
        if (n.elementType == elementType) {
            return n
        }
        n = n.parent
    }
    return null
}

@Deprecated(
    message = "Marked for removal in Ktlint 2.0",
    replaceWith = ReplaceWith("findParentByType(elementType)"),
)
public fun ASTNode.parent(elementType: IElementType): ASTNode? = findParentByType(elementType)

public fun ASTNode.findParentByType(elementType: IElementType): ASTNode? {
    var node: ASTNode? = parent
    while (node != null) {
        if (node.elementType == elementType) {
            return node
        }
        node = node.parent
    }
    return null
}

@Deprecated("Marked for removal in Ktlint 2.0")
public fun ASTNode.parent(
    strict: Boolean = true,
    predicate: (ASTNode) -> Boolean,
): ASTNode? {
    var n: ASTNode? = if (strict) this.parent else this
    while (n != null) {
        if (predicate(n)) {
            return n
        }
        n = n.parent
    }
    return null
}

public fun ASTNode.parent(predicate: (ASTNode) -> Boolean): ASTNode? {
    var node: ASTNode? = this.parent
    while (node != null && !predicate(node)) {
        node = node.parent
    }
    return node
}

public fun ASTNode.isPartOf(tokenSet: TokenSet): Boolean = elementType in tokenSet || parent { it.elementType in tokenSet } != null

/**
 * @param elementType [ElementType].*
 */
public fun ASTNode.isPartOf(elementType: IElementType): Boolean = this.elementType == elementType || findParentByType(elementType) != null

@Deprecated(
    "Marked for removal in Ktlint 2.x. Replace with ASTNode.isPartOf(elementType: IElementType) or ASTNode.isPartOf(tokenSet: TokenSet). " +
        "This method might cause performance issues, see https://github.com/pinterest/ktlint/pull/2901",
    replaceWith = ReplaceWith("this.isPartOf(elementTypeOrTokenSet)"),
)
public fun ASTNode.isPartOf(klass: KClass<out PsiElement>): Boolean {
    var node: ASTNode? = this
    while (node != null) {
        if (klass.java.isInstance(node.psi)) {
            return true
        }
        node = node.parent
    }
    return false
}

@Deprecated("Marked for removal in KtLint 2.0")
public fun ASTNode.isPartOfCompositeElementOfType(iElementType: IElementType): Boolean =
    iElementType == findCompositeParentElementOfType(iElementType)?.elementType

@Deprecated("Marked for removal in KtLint 2.0")
public fun ASTNode.findCompositeParentElementOfType(iElementType: IElementType): ASTNode? =
    parent { it.elementType == iElementType || it !is CompositeElement }

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isPartOfString20"),
)
public fun ASTNode.isPartOfString(): Boolean = isPartOfString20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.isPartOfString20
    get(): Boolean = findParentByType(STRING_TEMPLATE) != null

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isWhiteSpace20"),
)
@OptIn(ExperimentalContracts::class)
public fun ASTNode?.isWhiteSpace(): Boolean {
    contract {
        returns(true) implies (this@isWhiteSpace != null)
    }
    return isWhiteSpace20
}

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode?.isWhiteSpace20
    get() = this != null && elementType == WHITE_SPACE

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isWhiteSpaceWithNewline20"),
)
public fun ASTNode?.isWhiteSpaceWithNewline(): Boolean = isWhiteSpaceWithNewline20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode?.isWhiteSpaceWithNewline20
    get(): Boolean = this != null && isWhiteSpace20 && textContains('\n')

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isWhiteSpaceWithoutNewline20"),
)
public fun ASTNode?.isWhiteSpaceWithoutNewline(): Boolean = isWhiteSpaceWithoutNewline20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode?.isWhiteSpaceWithoutNewline20
    get(): Boolean = this != null && isWhiteSpace20 && !textContains('\n')

public val ASTNode?.isWhiteSpaceWithoutNewlineOrNull
    get(): Boolean = this == null || isWhiteSpaceWithoutNewline20

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isRoot20"),
)
public fun ASTNode.isRoot(): Boolean = isRoot20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.isRoot20
    get(): Boolean = elementType == FILE

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isLeaf20"),
)
public fun ASTNode.isLeaf(): Boolean = isLeaf20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.isLeaf20
    get(): Boolean = firstChildNode == null

/**
 * Check if the given [ASTNode] is a code leaf. E.g. it must be a leaf and may not be a whitespace or be part of a
 * comment.
 */
@Deprecated("Marked for removal in Ktlint 2.0. Use `isLeaf20 && isCode` instead")
public fun ASTNode.isCodeLeaf(): Boolean = isLeaf20 && isCode

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isPartOfComment20"),
)
public fun ASTNode.isPartOfComment(): Boolean = isPartOfComment20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.isPartOfComment20
    get(): Boolean = isPartOf(TokenSets.COMMENTS)

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("children20"),
)
public fun ASTNode.children(): Sequence<ASTNode> = children20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.children20
    get(): Sequence<ASTNode> = generateSequence(firstChildNode) { node -> node.nextSibling20 }

@Deprecated("Marked for removal in Ktlint 2.0")
public fun ASTNode.recursiveChildren(includeSelf: Boolean = false): Sequence<ASTNode> = recursiveChildrenInternal(includeSelf)

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("recursiveChildren20"),
)
public fun ASTNode.recursiveChildren(): Sequence<ASTNode> = recursiveChildrenInternal(false)

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.recursiveChildren20
    get(): Sequence<ASTNode> = recursiveChildrenInternal(false)

private fun ASTNode.recursiveChildrenInternal(includeSelf: Boolean = false): Sequence<ASTNode> =
    sequence {
        if (includeSelf) {
            yield(this@recursiveChildrenInternal)
        }
        this@recursiveChildrenInternal.children20.forEach { yieldAll(it.recursiveChildrenInternal(includeSelf = true)) }
    }

/**
 * Updates or inserts a new whitespace element with [text] before the given node. If the node itself is a whitespace
 * then its contents is replaced with [text]. If the node is a (nested) composite element, the whitespace element is
 * added after the previous leaf node.
 */
public fun ASTNode.upsertWhitespaceBeforeMe(text: String) {
    if (isLeaf20) {
        if (isWhiteSpace20) {
            return replaceTextWith(text)
        }
        val previous = prevSibling20 ?: this.prevLeaf
        when {
            previous != null && previous.isWhiteSpace20 -> {
                previous.replaceTextWith(text)
            }

            parent?.firstChildNode == this -> {
                // Never insert a whitespace node as first node in a composite node
                parent?.upsertWhitespaceBeforeMe(text)
            }

            else -> {
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    (psi as LeafElement).rawInsertBeforeMe(psiWhiteSpace)
                }
            }
        }
    } else {
        when (val prevSibling = prevSibling20) {
            null -> {
                // Never insert a whitespace element as first child node in a composite node. Instead, upsert just before the composite node
                parent?.upsertWhitespaceBeforeMe(text)
            }

            is LeafElement -> {
                prevSibling.upsertWhitespaceAfterMe(text)
            }

            else -> {
                // Insert in between two composite nodes
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    parent?.addChild(psiWhiteSpace.node, this)
                }
            }
        }
    }
}

public fun ASTNode.replaceTextWith(text: String) {
    require(this is LeafElement)
    takeIf { it.text != text }
        ?.rawReplaceWithText(text)
}

/**
 * Updates or inserts a new whitespace element with [text] after the given node. If the node itself is a whitespace
 * then its contents is replaced with [text]. If the node is a (nested) composite element, the whitespace element is
 * added after the last child leaf.
 */
public fun ASTNode.upsertWhitespaceAfterMe(text: String) {
    if (isLeaf20) {
        if (isWhiteSpace20) {
            return replaceTextWith(text)
        }
        val next = nextSibling20 ?: nextLeaf
        when {
            next != null && next.isWhiteSpace20 -> {
                next.replaceTextWith(text)
            }

            parent?.lastChildNode == this -> {
                // Never insert whitespace as last node in a composite node
                parent?.upsertWhitespaceAfterMe(text)
            }

            else -> {
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    (psi as LeafElement).rawInsertAfterMe(psiWhiteSpace)
                }
            }
        }
    } else {
        when (val nextSibling = nextSibling20) {
            null -> {
                // Never insert a whitespace element as last child node in a composite node. Instead, upsert just after the composite node
                parent?.upsertWhitespaceAfterMe(text)
            }

            is LeafElement -> {
                nextSibling.upsertWhitespaceBeforeMe(text)
            }

            else -> {
                // Insert in between two composite nodes
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    parent?.addChild(psiWhiteSpace.node, nextSibling)
                }
            }
        }
    }
}

public val ASTNode.column: Int
    get() {
        var leaf = prevLeaf
        var offsetToTheLeft = 0
        while (leaf != null) {
            if (leaf.isWhiteSpaceWithNewline20) {
                offsetToTheLeft += leaf.textLength - 1 - leaf.text.lastIndexOf('\n')
                break
            }
            offsetToTheLeft += leaf.textLength
            leaf = leaf.prevLeaf
        }
        return offsetToTheLeft + 1
    }

/**
 * Get the current indentation of the line containing the [ASTNode]. By default, this indentation starts with a newline (\n) character.
 */
@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "one of the temporary property accessor. Calls to `indent()` or `indent(true)` should be replaced with `indent20()`. Calls " +
        "to `indent(false)` should be replaced with `indentWithoutNewlinePrefix()` In 2.0 the temporary accessors will be replaced " +
        "the final property accessors.",
    replaceWith = ReplaceWith("indent20 or indentWithoutNewlinePrefix"),
)
public fun ASTNode.indent(includeNewline: Boolean = true): String =
    if (includeNewline) {
        indent20
    } else {
        indentWithoutNewlinePrefix
    }

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.indent20
    get(): String = indentInternal().prefixIfNot("\n")

public val ASTNode.indentWithoutNewlinePrefix
    get(): String = indentInternal().removePrefix("\n")

/**
 * Get the current indentation of the line containing the [ASTNode]
 */
private fun ASTNode.indentInternal(): String =
    leaves(forward = false)
        .firstOrNull { it.isWhiteSpaceWithNewline20 }
        ?.text
        ?.substringAfterLast('\n')
        .orEmpty() // Fallback if node is not preceded by any newline character

/**
 *  Print content of a node and the element type of the node, its parent and its direct children. Utility is meant to
 *  be used during development only. Please do not remove.
 */
@Suppress("unused")
@Deprecated("Marked for removal in Ktlint 2.0. Use PsiViewer plugin instead")
public fun ASTNode.logStructure(): ASTNode =
    also {
        println("Processing ${text.replaceTabAndNewline()} : Type $elementType with parent ${parent?.elementType} ")
        children20
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
        .takeWhile { it != to && it != to.lastChildLeafOrSelf20 }

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
            .lastChildLeafOrSelf20
            .nextLeaf
    return from
        .firstChildLeafOrSelf20
        .leavesForwardsIncludingSelf
        .takeWhile { it != stopAtLeaf }
}

@Deprecated("Marked for removal in Ktlint 2.0")
public fun ASTNode.isValOrVarKeyword(): Boolean = elementType == VAL_KEYWORD || elementType == VAR_KEYWORD || elementType == VARARG_KEYWORD

/**
 * Creates a sequences of leaves including the [ASTNode] in case it is a [LeafElement] itself. By default, the leaves are traversed in
 * forward order. Setting [forward] to `false` changes this to traversal in backward direction.
 */
@Deprecated("Marked for removal in Ktlint 2.0. Use `leavesForwardsIncludingSelf` or `leavesBackwardsIncludingSelf` instead")
public fun ASTNode.leavesIncludingSelf(forward: Boolean = true): Sequence<ASTNode> {
    val sequence =
        if (isLeaf20) {
            sequenceOf(this)
        } else {
            emptySequence()
        }
    return sequence.plus(leaves(forward))
}

/**
 * Creates a forward sequence of leaves including the [ASTNode] in case it is a [LeafElement] itself. Otherwise, an empty sequence is
 * returned.
 */
public val ASTNode.leavesForwardsIncludingSelf
    get(): Sequence<ASTNode> = sequenceOfLeafOrEmpty.plus(leaves(forward = true))

/**
 * Creates a backward sequence of leaves including the [ASTNode] in case it is a [LeafElement] itself. Otherwise, an empty sequence is
 *  * returned.
 */
public val ASTNode.leavesBackwardsIncludingSelf
    get(): Sequence<ASTNode> = sequenceOfLeafOrEmpty.plus(leaves(forward = false))

private val ASTNode.sequenceOfLeafOrEmpty
    get(): Sequence<ASTNode> {
        val sequence =
            if (isLeaf20) {
                sequenceOf(this)
            } else {
                emptySequence()
            }
        return sequence
    }

/**
 * Get all leaves on the same line as the given node including the whitespace indentation. Note that the whitespace indentation may start
 * with zero or more newline characters.
 */
@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("leavesOnLine20"),
)
public fun ASTNode.leavesOnLine(): Sequence<ASTNode> = leavesOnLine20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.leavesOnLine20
    get(): Sequence<ASTNode> {
        val takeAll = lastLeafOnLineOrNull == null
        return firstLeafOnLineOrSelf
            .leavesForwardsIncludingSelf
            .takeWhile { takeAll || it.prevLeaf != lastLeafOnLineOrNull }
    }

/**
 * Get all leaves on the same line as the given node including the whitespace indentation. Note that the whitespace indentation may start
 * with zero or more newline characters.
 */
@Deprecated(
    "Marked for removal in Ktlint 2.0. Use 'leavesOnLine20.dropTrailingEolComment()' to get all leaves on the line without the " +
        "trailing EOL comment",
)
public fun ASTNode.leavesOnLine(excludeEolComment: Boolean): Sequence<ASTNode> =
    leavesOnLine20.applyIf(excludeEolComment) { dropTrailingEolComment() }

/**
 * Take all nodes preceding the whitespace before the EOL comment
 */
public fun Sequence<ASTNode>.dropTrailingEolComment(): Sequence<ASTNode> =
    takeWhile {
        !(it.isWhiteSpaceWithoutNewline20 && it.nextLeaf?.elementType == EOL_COMMENT) &&
            // But if EOL-comment not preceded by whitespace than take all nodes before the EOL comment
            it.elementType != EOL_COMMENT
    }

internal val ASTNode.firstLeafOnLineOrSelf
    get() =
        prevLeaf { (it.textContains('\n') && !it.isPartOfComment20) || it.prevLeaf == null }
            ?: this

internal val ASTNode.lastLeafOnLineOrNull
    get() = nextLeaf { it.textContains('\n') }?.prevLeaf

/**
 * Get the total length of all leaves on the same line as the given node including the whitespace indentation but excluding all leading
 * newline characters in the whitespace indentation. Note that EOL-comments are included in the line length calculation. This can lead to
 * rules modifying the code before the EOL-comment in case the EOL-comment causes the max_line_length to be exceeded. It should be
 * preferred to prevent this by excluding the EOL-comment from the calculation (use [lineLength]), and let max-line-length rule report the
 * violation.
 */
@Deprecated(
    message =
        "Marked for removal in Ktlint 2.x. Rules should not modify code in case the EOL comment causes the max_line_length to be exceeded.",
    replaceWith = ReplaceWith("lineLength(excludeEolComment = false)"),
)
public fun ASTNode.lineLengthWithoutNewlinePrefix(): Int = leavesOnLine(excludeEolComment = false).lineLength

/**
 * Get the total length of all leaves on the same line as the given node including the whitespace indentation but excluding all leading
 * newline characters in the whitespace indentation. Use [excludeEolComment] to exclude the EOL-comment (and preceding whitespace) from this
 * calculation. Note that rules should not modify code in case the EOL comment causes the max_line_length to be exceeded. Instead, let the
 * max-line-length rule report this violation so that the developer can choose whether the comment can be shortened or that it can be placed
 * on a separate line.
 */
@Deprecated(
    "Marked for removal in Ktlint 2.0. Use `leavesOnLine20.dropTrailingEolComment().lineLength` instead to get the length of the line " +
        "without the trailing EOL comment.`",
)
public fun ASTNode.lineLength(excludeEolComment: Boolean = false): Int = leavesOnLine(excludeEolComment).lineLength

/**
 * Get the total length of all leaves in the sequence including the whitespace indentation but excluding all leading newline characters in
 * the whitespace indentation. The first leaf node in the sequence must be a white space starting with at least one newline.
 */
@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("lineLength"),
)
public fun Sequence<ASTNode>.lineLengthWithoutNewlinePrefix(): Int = lineLength

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val Sequence<ASTNode>.lineLength
    get(): Int {
        val first = firstOrNull() ?: return 0
        require(first.textContains('\n') || first.prevLeaf == null) {
            "First node in non-empty sequence must be a whitespace containing a newline"
        }
        return joinToString(separator = "") { it.text }
            // If a line is preceded by a blank line then the ident contains multiple newline chars
            .dropWhile { it == '\n' }
            // In case the last element on the line would contain a newline then only include chars before that newline. Note that this should
            // not occur if the AST is parsed correctly
            .takeWhile { it != '\n' }
            .length
    }

public fun ASTNode.afterCodeSibling(afterElementType: IElementType): Boolean =
    prevSibling { it.isCode && it.elementType == afterElementType } != null

public fun ASTNode.beforeCodeSibling(beforeElementType: IElementType): Boolean =
    nextSibling { it.isCode && it.elementType == beforeElementType } != null

public fun ASTNode.betweenCodeSiblings(
    afterElementType: IElementType,
    beforeElementType: IElementType,
): Boolean = afterCodeSibling(afterElementType) && beforeCodeSibling(beforeElementType)

public fun ASTNode.hasModifier(iElementType: IElementType): Boolean =
    findChildByType(MODIFIER_LIST)
        ?.children20
        .orEmpty()
        .any { it.elementType == iElementType }

public fun ASTNode.replaceWith(node: ASTNode) {
    parent?.addChild(node, this)
    this.remove()
}

public fun ASTNode.remove() {
    parent?.removeChild(this)
}

/**
 * Searches the receiver [ASTNode] recursively, returning the first child with type [elementType]. If none are found, returns `null`.
 * If [includeSelf] is `true`, includes the receiver in the search. The receiver would then be the first element searched, so it is
 * guaranteed to be returned if it has type [elementType].
 */
@Deprecated("Marked for removal in KtLint 2.0")
public fun ASTNode.findChildByTypeRecursively(
    elementType: IElementType,
    includeSelf: Boolean,
): ASTNode? = recursiveChildren(includeSelf).firstOrNull { it.elementType == elementType }

/**
 * Searches the receiver [ASTNode] recursively, returning the first child with type [elementType]. If none are found, returns `null`.
 * If [includeSelf] is `true`, includes the receiver in the search. The receiver would then be the first element searched, so it is
 * guaranteed to be returned if it has type [elementType].
 */
public fun ASTNode.findChildByTypeRecursively(elementType: IElementType): ASTNode? =
    recursiveChildren20.firstOrNull { it.elementType == elementType }

/**
 * Returns the end offset of the text of this [ASTNode]
 */
@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("endOffset20"),
)
public fun ASTNode.endOffset(): Int = endOffset20

/**
 * Returns the end offset of the text of this [ASTNode]
 */
public val ASTNode.endOffset20 // TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
    get(): Int = textRange.endOffset

private val elementTypeCache = hashMapOf<IElementType, PsiElement>()

/**
 * Checks if the [AstNode] extends the [KtAnnotated] interface. Using this function to check the interface is more performant than checking
 * whether `psi is KtAnnotated` as the psi does not need to be derived from [ASTNode].
 */
@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isKtAnnotated20"),
)
// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public fun ASTNode.isKtAnnotated(): Boolean = isKtAnnotated20

/**
 * Checks if the [AstNode] extends the [KtAnnotated] interface. Using this function to check the interface is more performant than checking
 * whether `psi is KtAnnotated` as the psi does not need to be derived from [ASTNode].
 */
public val ASTNode.isKtAnnotated20
    get(): Boolean = psiType { it is KtAnnotated }

@Deprecated("Marked for removal in KtLint 2.0")
public fun ASTNode.isKtExpression(): Boolean = psiType { it is KtExpression }

@Deprecated("Marked for removal in KtLint 2.0")
public fun ASTNode.isKtLoopExpression(): Boolean = psiType { it is KtLoopExpression }

private inline fun ASTNode.psiType(predicate: (psiElement: PsiElement) -> Boolean): Boolean = predicate(dummyPsiElement())

/**
 * Checks if the [AstNode] extends the [T] interface which implements [KtElement]. Call this function like:
 * ```
 * astNode.isPsiType<KtAnnotated>()
 * ```
 * Using this function to check the [PsiElement] type of the [ASTNode] is more performant than checking whether `astNode.psi is KtAnnotated`
 * as the psi does not need to be derived from [ASTNode].
 */
public inline fun <reified T : KtElement> ASTNode.isPsiType(): Boolean = this.dummyPsiElement() is T

/**
 * FOR INTERNAL USE ONLY. The returned element is a stub version of a [PsiElement] of the same type as the given [ASTNode]. The returned
 * result may only be used to validate the type of the [PsiElement].
 */
public fun ASTNode.dummyPsiElement(): PsiElement =
    elementTypeCache
        .getOrPut(elementType) {
            // Create a dummy Psi element based on the current node, so that we can store the Psi Type for this ElementType.
            // Creating this cache entry once per elementType is cheaper than accessing the psi for every node.
            when (elementType) {
                is KtFileElementType -> createDummyKtFile()
                is KtKeywordToken -> this as PsiElement
                is KtNodeType -> (elementType as KtNodeType).createPsi(this)
                is KtStubElementType<*, *> -> (elementType as KtStubElementType<*, *>).createPsiFromAst(this)
                is KtToken -> this as PsiElement
                else -> throw NotImplementedError("Cannot create dummy psi for $elementType (${elementType::class})")
            }
        }

private fun createDummyKtFile(): KtFile = KtlintKotlinCompiler.createPsiFileFromText("File.kt", "") as KtFile

/**
 * Returns true if the receiver is not null, and it represents a declaration
 */
@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isDeclaration20"),
)
public fun ASTNode?.isDeclaration(): Boolean = isDeclaration20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode?.isDeclaration20
    get(): Boolean = this != null && elementType in KtTokenSets.DECLARATION_TYPES
