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
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.IF
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_TEMPLATE_ENTRY_END
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.PARENTHESIZED
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SHORT_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevCodeSibling
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.initKtLintKLogger
import java.util.Deque
import java.util.LinkedList
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.psi.psiUtil.parents

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

    private lateinit var stringTemplateIndenter: StringTemplateIndenter

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
                    fromASTNode = node,
                    nodeIndentLevel = 0,
                    childIndentLevel = 0,
                    unchanged = false,
                ),
            )
        }

        /* Dump entire indentContextStack before each node
        logger.trace {
            indentContextStack
                .joinToString(
                    prefix = "Node ${node.elementType}: ${node.textWithEscapedTabAndNewline()}\n\t",
                    separator = "\n\t",
                ) { "${it.fromASTNode.elementType} - ${it.toASTNode.elementType}: ${it.nodeIndentLevel}, ${it.childIndentLevel}, ${it.unchanged}" }
        }
         */

        if (node.isWhiteSpaceWithNewline()) {
            if (indentContextStack.peekLast()?.unchanged == true) {
                val lastIndentContext = indentContextStack.removeLast()
                indentContextStack.addLast(
                    lastIndentContext.copy(unchanged = false),
                )
            }
        }
        when {
            node.isWhiteSpaceWithNewline() -> {
                visitWhiteSpace(node, autoCorrect, emit)
            }
            node.isWhiteSpaceWithNewline() &&
                node.prevCodeSibling()?.elementType == EQ &&
                node.treeParent?.elementType == FUN -> {
                visitWhiteSpace(node, autoCorrect, emit)
            }
            node.elementType == LONG_STRING_TEMPLATE_ENTRY &&
                node.treeParent.prevSibling { !it.isPartOfComment() }?.isWhiteSpaceWithNewline() == true -> {
                startIndentContextSameAsParent(node)
            }
            node.elementType == CLASS_BODY ||
                node.elementType == STRING_TEMPLATE ||
                node.elementType == LONG_STRING_TEMPLATE_ENTRY ||
                node.elementType == PARENTHESIZED ||
                node.elementType == SUPER_TYPE_ENTRY ||
                node.elementType == SUPER_TYPE_CALL_ENTRY ||
                node.elementType == TYPE_ARGUMENT_LIST ||
                node.elementType == VALUE_ARGUMENT_LIST ||
                node.elementType == VALUE_PARAMETER_LIST ||
                node.elementType == WHEN ->
                startIndentContextSameAsParent(node)
            node.elementType == LBRACE -> {
                val treeParent = node.treeParent
                if (treeParent.elementType == FUNCTION_LITERAL &&
                    treeParent.findChildByType(VALUE_PARAMETER_LIST) != null
                ) {
                    currentIndentLevel().let { currentIndentLevel ->
                        val arrow =
                            treeParent.findChildByType(ARROW)
                                ?: treeParent.lastChildLeafOrSelf()
                        startIndentContext(
                            fromAstNode = node,
                            toAstNode = arrow,
                            nodeIndentLevel = currentIndentLevel + 2,
                            childIndentLevel = currentIndentLevel + 2,
                        )
                    }
                } else {
                    val rbrace = requireNotNull(
                        node.nextSibling { it.elementType == RBRACE }
                    ) { "Can not find matching rbrace" }
                    startIndentContextSameAsParent(
                        fromAstNode = node,
                        toAstNode = rbrace,
                    )
                }
            }
            node.elementType == EQ &&
                node.treeParent?.elementType == FUN -> {
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = node.treeParent.lastChildLeafOrSelf(),
                )
            }
            node.elementType == LPAR && node.nextCodeSibling()?.elementType == CONDITION -> {
                currentIndentLevel()
                    .let { currentIndentLevel ->
                        startIndentContext(
                            fromAstNode = requireNotNull(node.nextLeaf()), // Allow to pickup whitespace before condition
                            toAstNode = requireNotNull(node.nextCodeSibling()).lastChildLeafOrSelf(), // Ignore whitespace after condition but before rpar
                            nodeIndentLevel = currentIndentLevel + 1,
                            childIndentLevel = currentIndentLevel + 1,
                        )
                    }
            }
            node.elementType == CLASS &&
                node.findChildByType(SUPER_TYPE_LIST) != null ->
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = node.findChildByType(SUPER_TYPE_LIST)!!.lastChildLeafOrSelf(),
                )
            node.elementType == BINARY_EXPRESSION -> {
                if (isPartOfBinaryExpressionWrappedInCondition(node)) {
                    startIndentContextSameAsParent(
                        fromAstNode = node.firstChildNode,
                        toAstNode = node.firstChildNode.lastChildLeafOrSelf(),
                    )
                } else if (node.treeParent?.elementType != BINARY_EXPRESSION) {
                    startIndentContextSameAsParent(node)
                }
                Unit
            }
            node.elementType == DOT_QUALIFIED_EXPRESSION -> {
                startIndentContextSameAsParent(node.firstCodeChild())
            }
            node.elementType == SAFE_ACCESS_EXPRESSION -> {
                if (node.treeParent?.firstChildNode?.elementType != SAFE_ACCESS_EXPRESSION) {
                    startIndentContextSameAsParent(node)
                }
            }
            node.elementType == IDENTIFIER &&
                node.treeParent.elementType == PROPERTY &&
                node.treeParent.findChildByType(IF) == null -> {
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = node.treeParent.lastChildLeafOrSelf(),
                )
            }
            node.elementType == LITERAL_STRING_TEMPLATE_ENTRY &&
                node.nextCodeSibling()?.elementType == CLOSING_QUOTE -> {
                    visitWhiteSpaceBeforeClosingQuote(node, autoCorrect, emit)
                }
            node.elementType == WHEN_ENTRY -> {
                val lastCodeLeafBeforeArrow =
                    requireNotNull(
                        node.findChildByType(ARROW)?.prevCodeLeaf()
                    ) { "Can not find last code leaf before arrow in when entry" }
                currentIndentLevel().let { currentIndentLevel ->
                    startIndentContext(
                        fromAstNode = node,
                        toAstNode = lastCodeLeafBeforeArrow,
                        nodeIndentLevel = currentIndentLevel,
                        childIndentLevel = currentIndentLevel,
                    )
                }
            }
            !node.isWhiteSpaceWithNewline() && node.children().none { it.isWhiteSpaceWithNewline() } -> {
                // No direct child node contains a whitespace with new line. So this node can not be a reason to change
                // the indent level
                logger.trace { "Ignore node as it is not and does not contain a whitespace with newline for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}" }
                return
            }
            node.isWhiteSpaceWithNewline() -> {
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
                    fromASTNode = node,
                    nodeIndentLevel = 0,
                    childIndentLevel = 0,
                    unchanged = true,
                ),
            )
        }
    }

    private fun ASTNode.firstCodeChild() =
        children()
            .first { !it.isWhiteSpace() && !it.isPartOfComment() }

    private fun isPartOfBinaryExpressionWrappedInCondition(node: ASTNode) =
        node
            .parents()
            .takeWhile { it.elementType == BINARY_EXPRESSION || it.elementType == CONDITION }
            .lastOrNull()
            ?.elementType == CONDITION

    private fun startIndentContextSameAsParent(
        fromAstNode: ASTNode,
        toAstNode: ASTNode = fromAstNode.lastChildLeafOrSelf(),
    ) {
        currentIndentLevel()
            .let { currentIndentLevel ->
                startIndentContext(
                    fromAstNode,
                    toAstNode,
                    currentIndentLevel,
                    currentIndentLevel + 1,
                )
            }
    }

    private fun currentIndentLevel() =
        indentContextStack
            .peekLast()
            .let { lastIndentContextStack ->
                if (lastIndentContextStack.unchanged) {
                    lastIndentContextStack.nodeIndentLevel
                } else {
                    lastIndentContextStack.childIndentLevel
                }
            }

    private fun startIndentContext(
        fromAstNode: ASTNode,
        toAstNode: ASTNode,
        nodeIndentLevel: Int,
        childIndentLevel: Int,
    ) {
        NewIndentContext(
            fromASTNode = fromAstNode,
            toASTNode = toAstNode,
            nodeIndentLevel = nodeIndentLevel,
            childIndentLevel = childIndentLevel,
            unchanged = true,
        ).let { newIndentContext ->
            indentContextStack
                .addLast(newIndentContext)
                .also {
                    logger.trace {
                        "Create new indent context (same as parent) with level (${newIndentContext.nodeIndentLevel}, ${newIndentContext.childIndentLevel})  for ${fromAstNode.elementType}: ${newIndentContext.nodes}"
                    }
                }
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        while (indentContextStack.peekLast()?.toASTNode == node) {
            logger.trace {
                val indentContext = indentContextStack.peekLast()
                "Remove indent context with level (${indentContext.nodeIndentLevel}, ${indentContext.childIndentLevel}) for ${indentContext.fromASTNode.elementType}: ${indentContext.nodes}"
            }
            indentContextStack
                .removeLast()
                .also {
                    logger.trace {
                        val indentContext = indentContextStack.peekLast()
                        "Last indent context with level (${indentContext.nodeIndentLevel}, ${indentContext.childIndentLevel}) for ${indentContext.fromASTNode.elementType}: ${indentContext.nodes}"
                    }
                }
        }

        when {
            node.elementType == RPAR &&
                node.nextCodeSibling()?.elementType == THEN -> {
                startIndentContextSameAsParent(
                    fromAstNode = node.nextCodeSibling()!!,
                    toAstNode = node
                        .treeParent
                        .findChildByType(ELSE_KEYWORD)
                        ?.prevCodeLeaf()
                        ?: node.treeParent.lastChildLeafOrSelf(),
                )
            }
            node.elementType == ELSE_KEYWORD -> {
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    node.treeParent.lastChildLeafOrSelf(),
                )
            }
            node.elementType == RPAR &&
                node.nextCodeSibling()?.elementType == BODY -> {
                startIndentContextSameAsParent(
                    fromAstNode = node.nextCodeSibling()!!,
                    node.treeParent.lastChildLeafOrSelf(),
                )
            }
            isPartOfBinaryExpressionWrappedInCondition(node) -> {
                val binaryExpression = node.treeParent
                if (indentContextStack.peekLast().fromASTNode == binaryExpression) {
                    // Remove the indent context for the left-hand side of the binary expression
                    indentContextStack.removeLast()
                    // Complex binary expression are nested in such a way that the indent context of the condition wrapper
                    // is not the last node on the stack
                    val conditionIndentContext =
                        indentContextStack
                            .filterNot { it.fromASTNode.elementType == BINARY_EXPRESSION }
                            .last()
                    // Create new indent context for the remainder (operator and right-hand side) of the binary expression
                    startIndentContext(
                        fromAstNode = requireNotNull(node.nextSibling { true }),
                        toAstNode = binaryExpression.lastChildLeafOrSelf(),
                        nodeIndentLevel = conditionIndentContext.nodeIndentLevel,
                        childIndentLevel = conditionIndentContext.childIndentLevel,
                    )
                }
            }
            node.elementType == ARROW -> {
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = node.treeParent.lastChildLeafOrSelf(),
                )
            }
            node.treeParent?.elementType == DOT_QUALIFIED_EXPRESSION && node == node.treeParent?.firstCodeChild() -> {
                val fromAstNode =
                    requireNotNull(
                        node
                            .treeParent
                            ?.firstCodeChild()
                            ?.nextLeaf()
                    ) { "Can not find a leaf after the left hand side in a dot qualified expression" }
                val extraIndent =
                    if (node.hasWhitespaceWithNewLineInBinaryExpression()) {
                        1
                    } else {
                        0
                    }
                currentIndentLevel()
                    .plus(extraIndent)
                    .let { indentLevel ->
                    startIndentContext(
                        fromAstNode = fromAstNode,
                        toAstNode = node.treeParent.lastChildLeafOrSelf(),
                        nodeIndentLevel = indentLevel,
                        childIndentLevel = indentLevel,
                    )
                }
            }
        }
    }

    private fun ASTNode.hasWhitespaceWithNewLineInBinaryExpression(): Boolean {
        var node = this
        do {
            if (node.elementType == DOT_QUALIFIED_EXPRESSION) {
                val hasWhitespaceWithNewLineInLeftHandSide =
                    node
                        .children()
                        .any { it.isWhiteSpaceWithNewline() }
                if (hasWhitespaceWithNewLineInLeftHandSide) {
                    return true
                }
            }
            node = node.treeParent
        } while (node.elementType == DOT_QUALIFIED_EXPRESSION)
        return false
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
            nextLeaf == lastIndexContext.toASTNode &&
                (
                    (
                        nextLeafElementType == RPAR &&
                            nextLeaf.treeParent?.elementType != PARENTHESIZED
                        ) ||
                        nextLeafElementType == RBRACE ||
                        nextLeafElementType == LONG_TEMPLATE_ENTRY_END ||
                        nextLeafElementType == CLOSING_QUOTE
                    )
        val adjustedExpectedIndent =
            if (isClosingNode) {
                lastIndexContext.nodeIndentLevel
            } else {
                lastIndexContext.childIndentLevel
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

    private fun visitWhiteSpaceBeforeClosingQuote(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (!this::stringTemplateIndenter.isInitialized) {
            stringTemplateIndenter = StringTemplateIndenter(indentConfig)
        }
        stringTemplateIndenter.visitClosingQuotes(currentIndentLevel(), node.treeParent, autoCorrect, emit)
    }

    private data class NewIndentContext(
        /**
         * The node on which the indent context starts.
         */
        val fromASTNode: ASTNode,

        /**
         * The node at which the indent context ends. If null, then the context ends at the last child leaf of the node
         * on which the indent context starts.
         */
        val toASTNode: ASTNode = fromASTNode.lastChildLeafOrSelf(),

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
    ) {
        val nodes: String
            get() =
                fromASTNode
                    .prevLeaf() // The 'fromAstNode' itself needs to be returned by '.leaves()' call as well
                    ?.leaves()
                    .orEmpty()
                    .takeWhile {
                        // The 'toAstNode' itself needs to be included as well
                        it != toASTNode.nextLeaf()
                    }
                    .joinToString(separator = "") { it.text }
                    .textWithEscapedTabAndNewline()
    }
}

private fun ASTNode.isKDocIndent() =
    if (text.lastOrNull() == ' ') {
        // The indentation of a KDoc comment contains a space as the last character regardless of the indentation style
        // (tabs or spaces) except for the starting line of the KDoc comment
        nextLeaf()?.elementType == KDOC_LEADING_ASTERISK || nextLeaf()?.elementType == KDOC_END
    } else {
        false
    }

private fun ASTNode.textWithEscapedTabAndNewline() =
    text.textWithEscapedTabAndNewline()

private fun String.textWithEscapedTabAndNewline(): String {
    val (prefix, suffix) = if (this.all { it.isWhitespace() }) {
        Pair("[", "]")
    } else {
        Pair("", "")
    }
    return prefix
        .plus(
            this
                .replace("\t", "\\t")
                .replace("\n", "\\n"),
        ).plus(suffix)
}

private fun ASTNode.processedButNoIndentationChangedNeeded() =
    logger.trace { "No indentation change required for $elementType: ${textWithEscapedTabAndNewline()}" }

private class StringTemplateIndenter(private val indentConfig: IndentConfig) {
    fun visitClosingQuotes(
        expectedIndentLevel: Int,
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(node.elementType == STRING_TEMPLATE)
        node
            .let { it.psi as KtStringTemplateExpression }
            .takeIf { it.isFollowedByTrimIndent() || it.isFollowedByTrimMargin() }
            ?.takeIf { it.isMultiLine() }
            ?.let { _ ->
                if (node.containsMixedIndentationCharacters()) {
                    // It can not be determined with certainty how mixed indentation characters should be interpreted.
                    // The trimIndent function handles tabs and spaces equally (one tabs equals one space) while the user
                    // might expect that the tab size in the indentation is more than one space.
                    emit(
                        node.startOffset,
                        "Indentation of multiline string should not contain both tab(s) and space(s)",
                        false,
                    )
                    return
                }

                val prefixLength = node.getCommonPrefixLength()
                val correctedExpectedIndentLevel = if (node.prevLeaf()?.text == "\n") {
                    // In case the opening quotes are placed at the start of the line, then the closing quotes should
                    // have no indent as well.
                    0
                } else {
                    expectedIndentLevel
                }
                val expectedIndentation = indentConfig.indent.repeat(correctedExpectedIndentLevel)
                val expectedPrefixLength = correctedExpectedIndentLevel * indentConfig.indent.length
                node.children()
                    .forEach {
                        if (it.prevLeaf()?.text == "\n" &&
                            (
                                it.isLiteralStringTemplateEntry() ||
                                    it.isVariableStringTemplateEntry() ||
                                    it.isClosingQuote()
                                )
                        ) {
                            val (actualIndent, actualContent) =
                                if (it.isIndentBeforeClosingQuote()) {
                                    it.text.splitIndentAt(it.text.length)
                                } else if (it.isVariableStringTemplateEntry() && it.isFirstNonBlankElementOnLine()) {
                                    it.getFirstElementOnSameLine().text.splitIndentAt(expectedPrefixLength)
                                } else {
                                    it.text.splitIndentAt(prefixLength)
                                }
                            if (indentConfig.containsUnexpectedIndentChar(actualIndent)) {
                                val offsetFirstWrongIndentChar =
                                    indentConfig.indexOfFirstUnexpectedIndentChar(actualIndent)
                                emit(
                                    it.startOffset + offsetFirstWrongIndentChar,
                                    "Unexpected '${indentConfig.unexpectedIndentCharDescription}' character(s) in margin of multiline string",
                                    true,
                                )
                                if (autoCorrect) {
                                    (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                        expectedIndentation + actualContent,
                                    )
                                }
                            } else if (actualIndent != expectedIndentation && it.isIndentBeforeClosingQuote()) {
                                // It is a deliberate choice not to fix the indents inside the string literal except the line which only contains
                                // the closing quotes.
                                emit(
                                    it.startOffset,
                                    "Unexpected indent of multiline string closing quotes",
                                    true,
                                )
                                if (autoCorrect) {
                                    if (it.firstChildNode == null) {
                                        (it as LeafPsiElement).rawInsertBeforeMe(
                                            LeafPsiElement(REGULAR_STRING_PART, expectedIndentation),
                                        )
                                    } else {
                                        (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                            expectedIndentation + actualContent,
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
    }

    /**
     * Get the length of the indent which is shared by all lines inside the string template except for the indent of
     * the closing quotes.
     */
    private fun ASTNode.getCommonPrefixLength() =
        children()
            .filterNot { it.elementType == OPEN_QUOTE }
            .filterNot { it.elementType == CLOSING_QUOTE }
            .filter { it.prevLeaf()?.text == "\n" }
            .filterNot { it.text == "\n" }
            .let { indents ->
                val indentsExceptBlankIndentBeforeClosingQuote = indents
                    .filterNot { it.isIndentBeforeClosingQuote() }
                if (indentsExceptBlankIndentBeforeClosingQuote.count() > 0) {
                    indentsExceptBlankIndentBeforeClosingQuote
                } else {
                    indents
                }
            }
            .map { it.text.indentLength() }
            .minOrNull()
            ?: 0

    private fun KtStringTemplateExpression.isFollowedByTrimIndent() = isFollowedBy("trimIndent()")

    private fun KtStringTemplateExpression.isFollowedByTrimMargin() = isFollowedBy("trimMargin()")

    private fun KtStringTemplateExpression.isFollowedBy(callExpressionName: String) =
        this.node.nextSibling { it.elementType != DOT }
            .let { it?.elementType == CALL_EXPRESSION && it.text == callExpressionName }

    private fun KtStringTemplateExpression.isMultiLine(): Boolean {
        for (child in node.children()) {
            if (child.elementType == LITERAL_STRING_TEMPLATE_ENTRY) {
                val v = child.text
                if (v == "\n") {
                    return true
                }
            }
        }
        return false
    }

    private fun ASTNode.containsMixedIndentationCharacters(): Boolean {
        assert((this.psi as KtStringTemplateExpression).isMultiLine())
        val nonBlankLines = this
            .text
            .split("\n")
            .filterNot { it.startsWith("\"\"\"") }
            .filterNot { it.endsWith("\"\"\"") }
            .filterNot { it.isBlank() }
        val prefixLength = nonBlankLines.minOfOrNull { it.indentLength() } ?: 0
        val distinctIndentCharacters = nonBlankLines
            .joinToString(separator = "") {
                it.splitIndentAt(prefixLength).first
            }
            .toCharArray()
            .distinct()
            .count()
        return distinctIndentCharacters > 1
    }

    private fun ASTNode.isIndentBeforeClosingQuote() =
        elementType == CLOSING_QUOTE || (text.isBlank() && nextCodeSibling()?.elementType == CLOSING_QUOTE)

    private fun ASTNode.isLiteralStringTemplateEntry() =
        elementType == LITERAL_STRING_TEMPLATE_ENTRY && text != "\n"

    private fun ASTNode.isVariableStringTemplateEntry() =
        elementType == LONG_STRING_TEMPLATE_ENTRY || elementType == SHORT_STRING_TEMPLATE_ENTRY

    private fun ASTNode.isClosingQuote() =
        elementType == CLOSING_QUOTE

    private fun String.indentLength() =
        indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

    /**
     * Splits the string at the given index or at the first non white space character before that index. The returned pair
     * consists of the indentation and the second part contains the remainder. Note that the second part still can start
     * with whitespace characters in case the original strings starts with more white space characters than the requested
     * split index.
     */
    private fun String.splitIndentAt(index: Int): Pair<String, String> {
        assert(index >= 0)
        val firstNonWhitespaceIndex = indexOfFirst { !it.isWhitespace() }.let {
            if (it == -1) {
                this.length
            } else {
                it
            }
        }
        val safeIndex = kotlin.math.min(firstNonWhitespaceIndex, index)
        return Pair(
            first = this.take(safeIndex),
            second = this.substring(safeIndex),
        )
    }

    private fun ASTNode.getFirstElementOnSameLine(): ASTNode {
        val firstLeafOnLine = prevLeaf { it.text == "\n" }
        return if (firstLeafOnLine == null) {
            this
        } else {
            firstLeafOnLine.nextLeaf(includeEmpty = true) ?: this
        }
    }

    private fun ASTNode.isFirstNonBlankElementOnLine(): Boolean {
        var node: ASTNode? = getFirstElementOnSameLine()
        while (node != null && node != this && node.text.isWhitespace()) {
            node = node.nextLeaf()
        }
        return node != this
    }

    private fun String.isWhitespace() =
        none { !it.isWhitespace() }
}
