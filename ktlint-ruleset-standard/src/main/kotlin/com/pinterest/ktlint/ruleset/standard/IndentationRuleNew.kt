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
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CLASS_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUN
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
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.firstChildLeafOrSelf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.noWhiteSpaceWithNewLineInClosedRange
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeSibling
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.ruleset.standard.IndentAdjustment.DECREMENT_FROM_CURRENT
import com.pinterest.ktlint.ruleset.standard.IndentAdjustment.INCREMENT_FROM_CURRENT
import com.pinterest.ktlint.ruleset.standard.IndentAdjustment.INCREMENT_FROM_FIRST
import com.pinterest.ktlint.ruleset.standard.IndentAdjustment.NONE
import com.pinterest.ktlint.ruleset.standard.IndentAdjustment.SAME_AS_PARENT
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
            indentContextStack.addLast(
                NewIndentContext(
                    node = node,
                    nodeIndentLevel = 0,
                    childIndentLevel = 0,
                    unchanged = false,
                ),
            )
        }

        val indentAdjustment = when {
            node.isWhiteSpaceWithNewline() &&
                (
                    node.treeParent?.elementType == BLOCK ||
                        node.treeParent?.elementType == CLASS_BODY ||
                        node.treeParent?.elementType == DOT_QUALIFIED_EXPRESSION ||
                        node.treeParent?.elementType == PROPERTY ||
                        node.treeParent?.elementType == PROPERTY ||
                        node.treeParent?.elementType == FUNCTION_LITERAL ||
                        node.treeParent?.elementType == VALUE_ARGUMENT_LIST ||
                        node.treeParent?.elementType == VALUE_PARAMETER_LIST ||
                        node.treeParent?.elementType == WHEN
                    ) -> {
                indentContextStack.increaseIndentOfLast()
                visitWhiteSpace(node, autoCorrect, emit)
                INCREMENT_FROM_CURRENT
            }
            node.elementType == EQ && node.treeParent?.elementType == FUN ->
                INCREMENT_FROM_CURRENT
            node.isWhiteSpaceWithNewline() &&
                node.prevCodeSibling()?.elementType == EQ &&
                node.treeParent?.elementType == FUN -> {
                indentContextStack.increaseIndentOfLast()
                visitWhiteSpace(node, autoCorrect, emit)
                INCREMENT_FROM_CURRENT
            }
            node.isWhiteSpaceWithNewline() &&
                node.prevCodeSibling()?.elementType == ARROW -> {
                indentContextStack.increaseIndentOfLast()
                visitWhiteSpace(node, autoCorrect, emit)
                INCREMENT_FROM_CURRENT
            }
            node.elementType == BLOCK ||
                node.elementType == CLASS_BODY ||
                node.elementType == PROPERTY ||
                node.elementType == FUN ||
                node.elementType == FUNCTION_LITERAL ||
                node.elementType == VALUE_ARGUMENT_LIST ||
                node.elementType == VALUE_PARAMETER_LIST ||
                node.elementType == WHEN ||
                node.elementType == WHEN_ENTRY ->
                SAME_AS_PARENT
            node.elementType == DOT_QUALIFIED_EXPRESSION ->
                INCREMENT_FROM_FIRST
            //        if (node.children().none() || node.children().none { it.isWhiteSpaceWithNewline() }) {
            !node.isWhiteSpaceWithNewline() && node.children().none { it.isWhiteSpaceWithNewline() } -> {
                // No direct child node contains a whitespace with new line. So this node can not be a reason to change
                // the indent level
                logger.trace { "Ignore node as it is not and does not contain a whitespace with newline for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}" }
                return
            }

            false && node.elementType == FUNCTION_LITERAL -> {
                node.logIndentRaised()
                INCREMENT_FROM_FIRST
            }
            false && INDENT_ON_ELEMENT_TYPE.contains(node.elementType) -> {
                node.logIndentRaised()
                INCREMENT_FROM_FIRST
            }
            false && node.elementType == PROPERTY -> {
                if (node.findChildByType(PROPERTY_ACCESSOR) != null) {
                    node.logIndentRaised()
                    INCREMENT_FROM_FIRST
                } else {
                    NONE
                }
            }
            false && node.elementType == BLOCK && node.treeParent.elementType == PROPERTY_ACCESSOR -> {
                node.logIndentRaised()
                indentContextStack.increaseIndentOfLast()
                INCREMENT_FROM_CURRENT
            }
            false && node.elementType == CLASS_KEYWORD -> {
                // Only starting from the class keyword, the remaining child elements needs to be indented. The
                // modifiers which precede this keyword should not be indented.
//                indentContextStack.increaseIndentOfLast()
                INCREMENT_FROM_CURRENT
            }
            false && node.elementType == FUN_KEYWORD -> {
                // Only starting from the fun keyword the remaining child elements needs to be indented. The modifiers
                // which precede this keyword should not be indented.
                indentContextStack.increaseIndentOfLast()
                INCREMENT_FROM_CURRENT
            }
            false &&
                node.elementType == BINARY_EXPRESSION &&
                node.textContains('\n') -> {
//                node.children().any { !it.isPartOfComment() && it.isWhiteSpaceWithNewline() } ->
//                &&
//                indentContextStack.last.node.lineNumber() != node.lineNumber() ->
                if (indentContextStack.last.node.elementType == PROPERTY &&
                    noWhiteSpaceWithNewLineInClosedRange(
                            indentContextStack.last.node.firstChildLeafOrSelf(),
                            node.firstChildLeafOrSelf(),
                        )
                ) {
                    // A call expression on the right-hand side does not take another indention if the indentation was
                    // already raised for the left-hand side given that it starts on the same line as that left hand
                    // side node.
                    // val foobar =
                    //     foo.doSomeThing {
                    //        bar
                    //     }
                    Unit
                } else {
                    node.logIndentRaised()
                    INCREMENT_FROM_FIRST
                }
            }
            false &&
//            node.elementType == BINARY_EXPRESSION ||
//                node.elementType == DOT_QUALIFIED_EXPRESSION ||
                node.elementType == OPERATION_REFERENCE ->
                if (node.treeParent.elementType != CONDITION &&
                    indentContextStack.last.node.elementType != BINARY_EXPRESSION &&
//                    indentContextStack.last.node.elementType != DOT_QUALIFIED_EXPRESSION &&
                    indentContextStack.last.node.elementType != OPERATION_REFERENCE &&
                    !(
                        node.elementType == BINARY_EXPRESSION &&
                            node == indentContextStack.last.node.lastChildNode &&
                            noWhiteSpaceWithNewLineInClosedRange(
                                    indentContextStack.last.node.firstChildLeafOrSelf(),
                                    node.firstChildLeafOrSelf(),
                                )
                        )
//                    indentContextStack.last.node.elementType != PROPERTY
                ) {
                    node.logIndentRaised()
                    INCREMENT_FROM_FIRST
                } else {
                    Unit
                }
            false && node.elementType == CALL_EXPRESSION &&
                node.textContains('\n') ->
//                node.children().any { !it.isPartOfComment() && it.isWhiteSpaceWithNewline() } ->
//                &&
//                indentContextStack.last.node.lineNumber() != node.lineNumber() ->
                if (indentContextStack.last.node.elementType == DOT_QUALIFIED_EXPRESSION &&
                    noWhiteSpaceWithNewLineInClosedRange(
                            indentContextStack.last.node.firstChildLeafOrSelf(),
                            node.firstChildLeafOrSelf(),
                        )
                ) {
                    // A call expression on the right-hand side does not take another indention if the indentation was
                    // already raised for the left-hand side given that it starts on the same line as that left hand
                    // side node.
                    // val foobar =
                    //     foo.doSomeThing {
                    //        bar
                    //     }
                    Unit
                } else if (isRightHandSideOfBinaryExpressionOnSameLineAsOperatorPrecedingIt(node)) {
                    Unit
                } else {
                    node.logIndentRaised()
                    INCREMENT_FROM_FIRST
                }
            false && node.elementType == DOT_QUALIFIED_EXPRESSION || node.elementType == CALL_EXPRESSION ->
                if (node.treeParent.elementType != node.elementType) {
                    INCREMENT_FROM_FIRST
                } else if (
                    indentContextStack.last.node.elementType != DOT_QUALIFIED_EXPRESSION &&
                    indentContextStack.last.node.elementType != BINARY_EXPRESSION &&
                    indentContextStack.last.node.elementType != CALL_EXPRESSION
//                    indentContextStack.last.node.lineNumber() != node.lineNumber()
//                    node.parents().none { it == indentContextStack.last.node }
                ) {
                    node.logIndentRaised()
                    INCREMENT_FROM_FIRST
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
//                indentContextStack.increaseIndentOfLast()
                INCREMENT_FROM_CURRENT
            }
            node.elementType == EQ &&
                node.treeParent.elementType == FUN &&
                node.nextSibling { !it.isPartOfComment() }.isWhiteSpaceWithNewline() -> {
                // Allow:
                // fun foo() =
                //     value
                INCREMENT_FROM_CURRENT
            }
            false && node.elementType == FUNCTION_LITERAL -> {
                node.logIndentRaised()
                INCREMENT_FROM_FIRST
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
                        node.logIndentLowered()
                        DECREMENT_FROM_CURRENT
                    }
                node
                    .chainedParent(LAMBDA_EXPRESSION, LAMBDA_ARGUMENT)
                    ?.prevSibling { !it.isPartOfComment() }
                    .takeIf { prevSibling -> prevSibling?.elementType == LPAR || prevSibling.isWhiteSpaceWithoutNewline() }
                    ?.let {
                        node.logIndentLowered()
                        DECREMENT_FROM_CURRENT
                    }
            }
            false && node.elementType == WHEN -> {
                // when ... {
                //     ...
                // }
                node.logIndentRaised()
                INCREMENT_FROM_CURRENT
            }
            false && node.elementType == LONG_STRING_TEMPLATE_ENTRY &&
                node.treeParent.prevSibling { !it.isPartOfComment() }.isWhiteSpaceWithNewline() -> {
                node.logIndentRaised()
                INCREMENT_FROM_CURRENT
            }
            false && node.elementType == BLOCK &&
                node.treeParent?.elementType == WHEN_ENTRY -> {
                node.logIndentRaised()
                INCREMENT_FROM_CURRENT
            }
            false && node.isWhiteSpaceWithNewline() &&
                node.prevLeaf()?.elementType == ARROW &&
                node.treeParent?.elementType == WHEN_ENTRY -> {
                // Condition and value inside when entry may be placed on separate lines
                // when ... {
                //     condition ->
                //         value1
                //     else ->
                //         value2
                // }
                node.logIndentRaised()
                visitWhiteSpace(node, autoCorrect, emit)
                INCREMENT_FROM_CURRENT
            }
            false && node.isWhiteSpaceWithNewline() -> {
//                node.logIndentRaised()
                indentContextStack.increaseIndentOfLast()
                visitWhiteSpace(node, autoCorrect, emit)
                INCREMENT_FROM_CURRENT
            }
            node.isWhiteSpaceWithNewline() -> {
                visitWhiteSpace(node, autoCorrect, emit)
            }
            false && node.isWhiteSpaceWithNewline() -> {
                val nextCodeSibling = node.nextCodeSibling()
                when {
                    nextCodeSibling == null -> Unit
                    nextCodeSibling.elementType == OPERATION_REFERENCE &&
                        nextCodeSibling.firstChildNode?.elementType == ELVIS &&
                        indentContextStack.last.node.elementType != BINARY_EXPRESSION -> {
                        // Indent the elvis operator itself
                        node.logIndentRaised()
                        // Indent complex multiline value/expression after the elvis operator
                        indentContextStack.increaseIndentOfLast()
                        INCREMENT_FROM_CURRENT
                    }
                }
                visitWhiteSpace(node, autoCorrect, emit)
            }
            else -> {
                logger.trace { "No processing for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}" }
            }
        }

        // Add node to stack only when it has not been pushed yet
        if (indentContextStack.isEmpty()) {
            indentContextStack.addLast(
                NewIndentContext(
                    node = node,
                    nodeIndentLevel = 0,
                    childIndentLevel = 0,
                    unchanged = true,
                ),
            )
        } else {
//            val parentIndentContext = indentContextStack.last()
//            require(parentIndentContext.node == node.treeParent) {
//                "Stack may not have been changed"
//            }
            val parent = indentContextStack.peekLast()
            when (indentAdjustment) {
                NONE -> Unit
                SAME_AS_PARENT -> {
                    val newIndentContext = NewIndentContext(
                        node = node,
                        nodeIndentLevel = parent.nodeIndentLevel,
                        childIndentLevel = parent.childIndentLevel,
                        unchanged = true,
                    )
                    indentContextStack
                        .addLast(newIndentContext)
                        .also {
                            logger.trace { "Create new indent context (same as parent) with level (${newIndentContext.nodeIndentLevel}, ${newIndentContext.childIndentLevel})  for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}" }
                        }
                }
                INCREMENT_FROM_FIRST -> {
                    indentContextStack
                        .addLast(
                            NewIndentContext(
                                node = node,
                                nodeIndentLevel = parent.nodeIndentLevel + 1,
                                childIndentLevel = parent.nodeIndentLevel + 1,
                                unchanged = true,
                            ),
                        ).also {
                            logger.trace { "Create new indent context with level ${parent.nodeIndentLevel + 1} for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}" }
                        }
                }
                INCREMENT_FROM_CURRENT ->
                    indentContextStack
                        .increaseIndentOfLast()
                DECREMENT_FROM_CURRENT ->
                    TODO()
            }
        }
    }

    private fun isRightHandSideOfBinaryExpressionOnSameLineAsOperatorPrecedingIt(node: ASTNode) =
        indentContextStack
            .last
            .node
            .takeIf { it.elementType == BINARY_EXPRESSION }
            ?.findChildByType(OPERATION_REFERENCE)
            ?.let { operationReference ->
                noWhiteSpaceWithNewLineInClosedRange(
                    operationReference.firstChildLeafOrSelf(),
                    node.firstChildLeafOrSelf(),
                )
            } ?: false

    /**
     * Increase indent for yet unvisited child nodes of the last node on the stack.
     */
    private fun Deque<NewIndentContext>.increaseIndentOfLast() {
        if (peekLast()?.unchanged == true) {
            val lastIndentContext = removeLast()
            val oldIndentLevel = lastIndentContext.childIndentLevel
            val newIndentLevel = oldIndentLevel + 1
            addLast(
                lastIndentContext.copy(
                    childIndentLevel = newIndentLevel,
                    unchanged = false,
                ),
            )
            logger.trace {
                "Adjusted index context from $oldIndentLevel to $newIndentLevel for " +
                    "${lastIndentContext.node.elementType}: ${lastIndentContext.node.textWithEscapedTabAndNewline()}"
            }
        }
    }

    /**
     * Decrease indent for yet unvisited child nodes of the last node on the stack.
     */
//    private fun Deque<NewIndentContext>.decreaseIndentOfLast() {
//        val lastIndentContext = removeLast()
//        addLast(lastIndentContext.copy(childIndentLevel = lastIndentContext.childIndentLevel - 1))
//        lastIndentContext.node.logIndentLowered()
//    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (indentContextStack.isNotEmpty() && indentContextStack.last.node == node) {
            indentContextStack.removeLast()
        }
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
            node.processedButNoIndentationChangedNeeded()
            return // raw strings (""") are allowed at column 0
        }
        val comment = nextLeaf?.parent({ it.psi is PsiComment }, strict = false)
        if (comment != null) {
            if (nodeIndent.isEmpty()) {
                node.processedButNoIndentationChangedNeeded()
                return // comments are allowed at column 0
            }
            if (comment.textContains('\n') && comment.elementType == BLOCK_COMMENT) {
                // FIXME: while we cannot assume any kind of layout inside a block comment,
                // `/*` and `*/` can still be indented
                node.processedButNoIndentationChangedNeeded()
                return
            }
        }

        // adjusting expectedIndent based on what is in front
        val lastIndexContext = indentContextStack.peekLast()
        val isClosingNode =
            nextLeaf == lastIndexContext.node.lastChildNode &&
                (
                    nextLeafElementType == RPAR ||
                        nextLeafElementType == RBRACE ||
                        nextLeafElementType == LONG_TEMPLATE_ENTRY_END
                    )
        val adjustedExpectedIndent =
            lastIndexContext.childIndentLevel +
                if (lastIndexContext.childIndentLevel > lastIndexContext.nodeIndentLevel && isClosingNode) {
                    -1
                } else {
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
                "Line $line: " + (if (!autoCorrect) "would have " else "") + "changed indentation to ${expectedIndentation.length} (from ${normalizedNodeIndent.length}) for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}"
            }
        } else {
            node.processedButNoIndentationChangedNeeded()
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
        /**
         * The node on the indent context is based.
         */
        val node: ASTNode,
        /**
         * Level of indentation of the node itself
         */
        val nodeIndentLevel: Int,
        /**
         *  Level of indentation for child nodes. Note that some child nodes may actually have the same indent level as
         *  the node while later child nodes are indented at a deeper level.
         */
        val childIndentLevel: Int = nodeIndentLevel,
        /**
         * True when the indentation level for child nodes has been raised
         */
        val unchanged: Boolean,
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

private fun ASTNode.textWithEscapedTabAndNewline(): String {
    val (prefix, suffix) = if (this.elementType == WHITE_SPACE) {
        Pair("[", "]")
    } else {
        Pair("", "")
    }
    return prefix
        .plus(
            text
                .replace("\t", "\\t")
                .replace("\n", "\\n"),
        ).plus(suffix)
}

private fun ASTNode.processedButNoIndentationChangedNeeded() =
    logger.trace { "No identation change required for $elementType: ${textWithEscapedTabAndNewline()}" }

private fun ASTNode.logIndentRaised() =
    logger.trace { "Raised indent while processing $elementType: ${textWithEscapedTabAndNewline()}" }

private fun ASTNode.logIndentLowered() =
    logger.trace { "Lowered indent while processing $elementType: ${textWithEscapedTabAndNewline()}" }

private enum class IndentAdjustment {
    NONE,
    INCREMENT_FROM_FIRST,
    INCREMENT_FROM_CURRENT,
    DECREMENT_FROM_CURRENT,

    /**
     * Indent of the node is initially the same as the parent node. Due to the type of element, its indentation might
     * be changed starting from one of its child nodes.
     */
    SAME_AS_PARENT
}
