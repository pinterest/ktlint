package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.SPACE
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.TAB
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLASS_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.IF
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_TEMPLATE_ENTRY_END
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.TRY
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.initKtLintKLogger
import java.util.Deque
import java.util.LinkedList
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

public class IndentationRuleNew :
    Rule(
        id = "indent",
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible,
            VisitorModifier.RunAfterRule(
                ruleId = "experimental:function-signature",
                loadOnlyWhenOtherRuleIsLoaded = false,
                runOnlyWhenOtherRuleIsEnabled = false,
            ),
        ),
    ),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            indentSizeProperty,
            indentStyleProperty,
        )
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    private var line = 1
    private var expectedIndent = 0

    private val indentContextStack: Deque<NewIndentContext> = LinkedList()

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        indentConfig = IndentConfig(
            indentStyle = editorConfigProperties.getEditorConfigValue(indentStyleProperty),
            tabWidth = editorConfigProperties.getEditorConfigValue(indentSizeProperty),
        )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (indentContextStack.isNotEmpty()) {
            require(indentContextStack.last().node == node.treeParent) {
                "Last node is not the parent of the current node"
            }
            expectedIndent = indentContextStack.last().childIndentLevel
        }
        if (node.isRoot()) {
            // File should not start with a whitespace
            node
                .nextLeaf()
                ?.takeIf { it.isWhiteSpaceWithoutNewline() }
                ?.let { whitespaceWithoutNewline ->
                    emit(node.startOffset, "Unexpected indentation", true)
                    if (autoCorrect) {
                        whitespaceWithoutNewline.treeParent.removeChild(whitespaceWithoutNewline)
                    }
                }
        }

        when {
            INDENT_ON_ELEMENT_TYPE.contains(node.elementType) ->
                expectedIndent++
            node.elementType == PROPERTY ->
                if (node.findChildByType(PROPERTY_ACCESSOR) != null) {
                    expectedIndent++
                }
            node.elementType == BLOCK && node.treeParent.elementType == PROPERTY_ACCESSOR -> {
                expectedIndent++
                indentContextStack.increaseIndentOfLast()
            }
            node.elementType == CLASS_KEYWORD -> {
                // Only starting from the class keyword, the remaining child elements needs to be indented. The
                // modifiers which precede this keyword should not be indented.
                indentContextStack.increaseIndentOfLast()
            }
            node.elementType == FUN_KEYWORD -> {
                // Only starting from the fun keyword the remaining child elements needs to be indented. The modifiers
                // which precede this keyword should not be indented.
                indentContextStack.increaseIndentOfLast()
            }
            node.elementType == BINARY_EXPRESSION ||
//                node.elementType == DOT_QUALIFIED_EXPRESSION ||
                node.elementType == OPERATION_REFERENCE ->
                if (node.treeParent.elementType != CONDITION &&
                    indentContextStack.last.node.elementType != BINARY_EXPRESSION &&
//                    indentContextStack.last.node.elementType != DOT_QUALIFIED_EXPRESSION &&
                    indentContextStack.last.node.elementType != OPERATION_REFERENCE
                ) {
                    expectedIndent++
                }
            node.elementType == CALL_EXPRESSION &&
                node.textContains('\n') ->
//                node.children().any { !it.isPartOfComment() && it.isWhiteSpaceWithNewline() } ->
//                &&
//                indentContextStack.last.node.lineNumber() != node.lineNumber() ->
                expectedIndent++
            node.elementType == DOT_QUALIFIED_EXPRESSION || node.elementType == CALL_EXPRESSION ->
                if (
                    indentContextStack.last.node.elementType != DOT_QUALIFIED_EXPRESSION &&
                    indentContextStack.last.node.elementType != BINARY_EXPRESSION &&
                    indentContextStack.last.node.elementType != CALL_EXPRESSION
//                    indentContextStack.last.node.lineNumber() != node.lineNumber()
//                    node.parents().none { it == indentContextStack.last.node }
                ) {
                    expectedIndent++
                } else {
                    Unit
                }
            node.elementType == EQ &&
                node.treeParent.elementType == PROPERTY &&
                node.treeParent.findChildByType(PROPERTY_ACCESSOR) == null &&
                node.nextSibling { !it.isPartOfComment() }.isWhiteSpaceWithNewline() -> {
                // Allow:
                // val v =
                //     value
                // but prevent indentation of PROPERTY element a second time when it was already corrected because of
                // the existence of a property accessor, like:
                // val v =
                //     value
                //     private set
                indentContextStack.increaseIndentOfLast()
            }
            node.elementType == FUNCTION_LITERAL -> {
                expectedIndent++
                node
                    .chainedParent(LAMBDA_EXPRESSION, VALUE_ARGUMENT)
                    ?.prevSibling { !it.isPartOfComment() }
                    .takeIf { prevSibling -> prevSibling?.elementType == LPAR || prevSibling.isWhiteSpaceWithoutNewline() }
                    ?.let {
                        // Allow
                        // foo({
                        //     ...
                        // }, {
                        //     ...
                        // })
                        expectedIndent--
                    }
                node
                    .chainedParent(LAMBDA_EXPRESSION, LAMBDA_ARGUMENT)
                    ?.prevSibling { !it.isPartOfComment() }
                    .takeIf { prevSibling -> prevSibling?.elementType == LPAR || prevSibling.isWhiteSpaceWithoutNewline() }
                    ?.let {
                        expectedIndent--
                    }
            }
            node.elementType == WHEN ->
                // when ... {
                //     ...
                // }
                expectedIndent++
            node.elementType == LONG_STRING_TEMPLATE_ENTRY &&
                node.treeParent.prevSibling { !it.isPartOfComment() }.isWhiteSpaceWithNewline() ->
                expectedIndent++
            node.elementType == BLOCK &&
                node.treeParent?.elementType == WHEN_ENTRY ->
                expectedIndent++
            node.isWhiteSpaceWithNewline() &&
                node.prevLeaf()?.elementType == ARROW &&
                node.treeParent?.elementType == WHEN_ENTRY -> {
                // Condition and value inside when entry may be placed on separate lines
                // when ... {
                //     condition ->
                //         value1
                //     else ->
                //         value2
                // }
                expectedIndent++
                visitWhiteSpace(node, autoCorrect, emit)
            }
            node.isWhiteSpaceWithNewline() -> {
                val nextCodeSibling = node.nextCodeSibling()
                when {
                    nextCodeSibling == null -> Unit
                    nextCodeSibling.elementType == OPERATION_REFERENCE &&
                        nextCodeSibling.firstChildNode?.elementType == ELVIS &&
                        indentContextStack.last.node.elementType != BINARY_EXPRESSION -> {
                        // Indent the elvis operator itself
                        expectedIndent++
                        // Indent complex multiline value/expression after the elvis operator
                        indentContextStack.increaseIndentOfLast()
                    }
                }
                visitWhiteSpace(node, autoCorrect, emit)
            }
        }

        // Add node to stack only when it has not been pushed yet
        if (indentContextStack.isEmpty()) {
            indentContextStack.addLast(NewIndentContext(node, 0, expectedIndent))
        } else {
//            val parentIndentContext = indentContextStack.last()
//            require(parentIndentContext.node == node.treeParent) {
//                "Stack may not have been changed"
//            }

            indentContextStack.addLast(NewIndentContext(node, expectedIndent, expectedIndent))
        }
    }

    /**
     * Increase indent for yet unvisited child nodes of the last node on the stack.
     */
    private fun Deque<NewIndentContext>.increaseIndentOfLast() {
        val lastIndentContext = removeLast()
        addLast(lastIndentContext.copy(childIndentLevel = lastIndentContext.childIndentLevel + 1))
    }

    /**
     * Decrease indent for yet unvisited child nodes of the last node on the stack.
     */
    private fun Deque<NewIndentContext>.decreaseIndentOfLast() {
        val lastIndentContext = removeLast()
        addLast(lastIndentContext.copy(childIndentLevel = lastIndentContext.childIndentLevel - 1))
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(indentContextStack.last.node == node) {
            "Last element on stack is unexpected"
        }
        indentContextStack.removeLast()
    }

    override fun afterLastNode() {
        // The expectedIndent should never be negative. If so, it is very likely that ktlint crashes at runtime when
        // autocorrecting is executed while no error occurs with linting only. Often, such errors are not found in unit
        // tests, as the examples are way more simple than realistic code.
        assert(expectedIndent >= 0)
        require(indentContextStack.isEmpty()) {
            "Stack should be empty"
        }
    }

    private fun visitWhiteSpace(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val text = node.text
        val nodeIndent = text.substringAfterLast("\n")
        val nextLeaf = node.nextLeaf()
        val nextLeafElementType = nextLeaf?.elementType
        if (nextLeafElementType == OPEN_QUOTE && nextLeaf.text == "\"\"\"" && nodeIndent.isEmpty()) {
            return // raw strings (""") are allowed at column 0
        }
        val comment = nextLeaf?.parent({ it.psi is PsiComment }, strict = false)
        if (comment != null) {
            if (nodeIndent.isEmpty()) {
                return // comments are allowed at column 0
            }
            if (comment.textContains('\n') && comment.elementType == BLOCK_COMMENT) {
                // FIXME: while we cannot assume any kind of layout inside a block comment,
                // `/*` and `*/` can still be indented
                return
            }
        }
        // adjusting expectedIndent based on what is in front
        val adjustedExpectedIndent = expectedIndent + when {
            nextLeafElementType == RPAR ->
                -1
            nextLeafElementType == RBRACE ->
                -1
            nextLeafElementType == LONG_TEMPLATE_ENTRY_END ->
                -1
            else ->
                0
        }
        // indentation with incorrect characters replaced
        val normalizedNodeIndent =
            when (indentConfig.indentStyle) {
                SPACE -> {
                    if ('\t' in nodeIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected tab character(s)",
                            true,
                        )
                        indentConfig.toNormalizedIndent(nodeIndent)
                    } else {
                        nodeIndent
                    }
                }
                TAB -> {
                    val isKdocIndent = node.isKDocIndent()
                    val indentWithoutKdocIndent =
                        if (node.isKDocIndent()) {
                            nodeIndent.removeSuffix(" ")
                        } else {
                            nodeIndent
                        }
                    if (' ' in indentWithoutKdocIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected space character(s)",
                            true,
                        )
                        indentConfig.toNormalizedIndent(indentWithoutKdocIndent) +
                            // Re-add the kdoc indent when it was present before
                            if (isKdocIndent) {
                                " "
                            } else {
                                ""
                            }
                    } else {
                        nodeIndent
                    }
                }
            }
        val expectedIndentation = indentConfig.indent.repeat(adjustedExpectedIndent) +
            // +1 space before * in `/**\n *\n */`
            if (comment?.elementType == KDOC && nextLeafElementType != KDOC_START) " " else ""
        if (normalizedNodeIndent != expectedIndentation) {
            emit(
                node.startOffset + text.length - nodeIndent.length,
                "Unexpected indentation (${normalizedNodeIndent.length}) (should be ${expectedIndentation.length})",
                true,
            )
            logger.trace {
                "$line: " + (if (!autoCorrect) "would have " else "") + "changed indentation to ${expectedIndentation.length} (from ${normalizedNodeIndent.length})"
            }
        }
        if (autoCorrect) {
            if (nodeIndent != normalizedNodeIndent || normalizedNodeIndent != expectedIndentation) {
                (node as LeafPsiElement).rawReplaceWithText(
                    text.substringBeforeLast("\n") + "\n" + expectedIndentation,
                )
            }
        }
    }

    private companion object {
        val INDENT_ON_ELEMENT_TYPE = arrayOf(
            BODY,
//            CALL_EXPRESSION,
            IF,
            TRY,
            TYPE_ARGUMENT_LIST,
            TYPE_PARAMETER_LIST,
        )
    }

    private data class NewIndentContext(
        val node: ASTNode,
        val nodeIndentLevel: Int,
        val childIndentLevel: Int = nodeIndentLevel,
    )
}

private fun ASTNode.isKDocIndent() =
    if (text.lastOrNull() == ' ') {
        // The indentation of a KDoc comment contains a space as the last character regardless of the indentation style
        // (tabs or spaces) except for the starting line of the KDoc comment
        nextLeaf()?.elementType == KDOC_LEADING_ASTERISK || nextLeaf()?.elementType == KDOC_END
    } else {
        false
    }

private fun ASTNode.chainedParent(vararg parentElementTypes: IElementType): ASTNode? {
    var currentNode = this
    val parentElementTypeIterator = parentElementTypes.iterator()
    while (currentNode != null && parentElementTypeIterator.hasNext()) {
        val parentElementType = parentElementTypeIterator.next()
        if (currentNode.treeParent.elementType == parentElementType) {
            currentNode = currentNode.treeParent
        } else {
            return null
        }
    }
    return currentNode
}
