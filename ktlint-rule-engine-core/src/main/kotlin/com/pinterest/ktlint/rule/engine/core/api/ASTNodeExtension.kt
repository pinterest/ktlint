package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REGULAR_STRING_PART
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

public fun ASTNode.isPartOf(tokenSet: TokenSet): Boolean = parent(strict = false) { tokenSet.contains(it.elementType) } != null

/**
 * @param elementType [ElementType].*
 */
public fun ASTNode.isPartOf(elementType: IElementType): Boolean = parent(elementType, strict = false) != null

@Deprecated(
    "Marked for removal in Ktlint 2.x. Replace with ASTNode.isPartOf(elementType: IElementType) or ASTNode.isPartOf(tokenSet: TokenSet). " +
        "This method might cause performance issues, see https://github.com/pinterest/ktlint/pull/2901",
    replaceWith = ReplaceWith("this.isPartOf(elementTypeOrTokenSet)"),
)
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

@Deprecated(
    "In Ktlint 2.0, it will be replaced with a property accessor. For easy migration replace current function call with " +
        "the temporary property accessor. In 2.0 it can be replaced the final property accessor which will be the same as the " +
        "current function name.",
    replaceWith = ReplaceWith("isPartOfString20"),
)
public fun ASTNode.isPartOfString(): Boolean = isPartOfString20

// TODO: In Ktlint 2.0 replace with accessor without temporary suffix "20"
public val ASTNode.isPartOfString20
    get(): Boolean = parent(STRING_TEMPLATE, strict = false) != null

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
    get(): Boolean = this != null && elementType == WHITE_SPACE && textContains('\n')

public fun ASTNode?.isWhiteSpaceWithoutNewline(): Boolean = this != null && elementType == WHITE_SPACE && !textContains('\n')

public fun ASTNode.isRoot(): Boolean = elementType == ElementType.FILE

public fun ASTNode.isLeaf(): Boolean = firstChildNode == null

/**
 * Check if the given [ASTNode] is a code leaf. E.g. it must be a leaf and may not be a whitespace or be part of a
 * comment.
 */
public fun ASTNode.isCodeLeaf(): Boolean = isLeaf() && !isWhiteSpace20 && !isPartOfComment()

public fun ASTNode.isPartOfComment(): Boolean = isPartOf(TokenSets.COMMENTS)

public fun ASTNode.children(): Sequence<ASTNode> = generateSequence(firstChildNode) { node -> node.treeNext }

public fun ASTNode.recursiveChildren(includeSelf: Boolean = false): Sequence<ASTNode> =
    sequence {
        if (includeSelf) {
            yield(this@recursiveChildren)
        }
        children().forEach { yieldAll(it.recursiveChildren(includeSelf = true)) }
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
        when {
            previous?.elementType == WHITE_SPACE -> {
                previous.replaceWhitespaceWith(text)
            }

            treeParent.firstChildNode == this -> {
                // Never insert a whitespace node as first node in a composite node
                treeParent.upsertWhitespaceBeforeMe(text)
            }

            else -> {
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    (psi as LeafElement).rawInsertBeforeMe(psiWhiteSpace)
                }
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
        when {
            next?.elementType == WHITE_SPACE -> {
                next.replaceWhitespaceWith(text)
            }

            treeParent.lastChildNode == this -> {
                // Never insert a whitespace as last node in a composite node
                treeParent.upsertWhitespaceAfterMe(text)
            }

            else -> {
                PsiWhiteSpaceImpl(text).also { psiWhiteSpace ->
                    (psi as LeafElement).rawInsertAfterMe(psiWhiteSpace)
                }
            }
        }
    } else {
        when (val nextSibling = nextSibling()) {
            null -> {
                // Never insert a whitespace element as last child node in a composite node. Instead, upsert just after the composite node
                treeParent?.upsertWhitespaceAfterMe(text)
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
        .firstOrNull { it.isWhiteSpaceWithNewline20 }
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
@Deprecated(
    message =
        "Marked for removal in Ktlint 2.x. Rules should not modify code in case the EOL comment causes the max_line_length to be exceeded.",
    replaceWith = ReplaceWith("leavesOnLine(excludeEolComment = false)"),
)
public fun ASTNode.leavesOnLine(): Sequence<ASTNode> = leavesOnLine(excludeEolComment = false)

/**
 * Get all leaves on the same line as the given node including the whitespace indentation. Note that the whitespace indentation may start
 * with zero or more newline characters.
 */
public fun ASTNode.leavesOnLine(excludeEolComment: Boolean): Sequence<ASTNode> {
    val lastLeafOnLineOrNull = getLastLeafOnLineOrNull()
    return getFirstLeafOnLineOrSelf()
        .leavesIncludingSelf()
        .applyIf(excludeEolComment) { dropTrailingEolComment() }
        .takeWhile { lastLeafOnLineOrNull == null || it.prevLeaf() != lastLeafOnLineOrNull }
}

/**
 * Take all nodes preceding the whitespace before the EOL comment
 */
private fun Sequence<ASTNode>.dropTrailingEolComment(): Sequence<ASTNode> =
    takeWhile {
        !(it.isWhiteSpaceWithoutNewline() && it.nextLeaf()?.elementType == EOL_COMMENT) &&
            // But if EOL-comment not preceded by whitespace than take all nodes before the EOL comment
            it.elementType != EOL_COMMENT
    }

internal fun ASTNode.getFirstLeafOnLineOrSelf() =
    prevLeaf { (it.textContains('\n') && !it.isPartOfComment()) || it.prevLeaf() == null }
        ?: this

internal fun ASTNode.getLastLeafOnLineOrNull() = nextLeaf { it.textContains('\n') }?.prevLeaf()

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
public fun ASTNode.lineLengthWithoutNewlinePrefix(): Int = leavesOnLine(excludeEolComment = false).lineLengthWithoutNewlinePrefix()

/**
 * Get the total length of all leaves on the same line as the given node including the whitespace indentation but excluding all leading
 * newline characters in the whitespace indentation. Use [excludeEolComment] to exclude the EOL-comment (and preceding whitespace) from this
 * calculation. Note that rules should not modify code in case the EOL comment causes the max_line_length to be exceeded. Instead, let the
 * max-line-length rule report this violation so that the developer can choose whether the comment can be shortened or that it can be placed
 * on a separate line.
 */
public fun ASTNode.lineLength(excludeEolComment: Boolean = false): Int = leavesOnLine(excludeEolComment).lineLengthWithoutNewlinePrefix()

/**
 * Get the total length of all leaves in the sequence including the whitespace indentation but excluding all leading newline characters in
 * the whitespace indentation. The first leaf node in the sequence must be a white space starting with at least one newline.
 */
public fun Sequence<ASTNode>.lineLengthWithoutNewlinePrefix(): Int {
    val first = firstOrNull() ?: return 0
    require(first.textContains('\n') || first.prevLeaf() == null) {
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

public fun ASTNode.afterCodeSibling(afterElementType: IElementType): Boolean = prevCodeSibling()?.elementType == afterElementType

public fun ASTNode.beforeCodeSibling(beforeElementType: IElementType): Boolean = nextCodeSibling()?.elementType == beforeElementType

public fun ASTNode.betweenCodeSiblings(
    afterElementType: IElementType,
    beforeElementType: IElementType,
): Boolean = afterCodeSibling(afterElementType) && beforeCodeSibling(beforeElementType)

public fun ASTNode.hasModifier(iElementType: IElementType): Boolean =
    findChildByType(ElementType.MODIFIER_LIST)
        ?.children()
        .orEmpty()
        .any { it.elementType == iElementType }

public fun ASTNode.replaceWith(node: ASTNode) {
    treeParent.addChild(node, this)
    this.remove()
}

public fun ASTNode.remove() {
    treeParent.removeChild(this)
}

/**
 * Searches the receiver [ASTNode] recursively, returning the first child with type [elementType]. If none are found, returns `null`.
 * If [includeSelf] is `true`, includes the receiver in the search. The receiver would then be the first element searched, so it is
 * guaranteed to be returned if it has type [elementType].
 */
public fun ASTNode.findChildByTypeRecursively(
    elementType: IElementType,
    includeSelf: Boolean,
): ASTNode? = recursiveChildren(includeSelf).firstOrNull { it.elementType == elementType }

/**
 * Returns the end offset of the text of this [ASTNode]
 */
public fun ASTNode.endOffset(): Int = textRange.endOffset

private val elementTypeCache = hashMapOf<IElementType, PsiElement>()

/**
 * Checks if the [AstNode] extends the [KtAnnotated] interface. Using this function to check the interface is more performant than checking
 * whether `psi is KtAnnotated` as the psi does not need to be derived from [ASTNode].
 */
public fun ASTNode.isKtAnnotated(): Boolean = psiType { it is KtAnnotated }

public fun ASTNode.isKtExpression(): Boolean = psiType { it is KtExpression }

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
public fun ASTNode?.isDeclaration(): Boolean = this != null && elementType in KtTokenSets.DECLARATION_TYPES
