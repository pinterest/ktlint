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
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_TEMPLATE_ENTRY_END
import com.pinterest.ktlint.core.ast.ElementType.LONG_TEMPLATE_ENTRY_START
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.PARENTHESIZED
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHILE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
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
import org.jetbrains.kotlin.psi.psiUtil.leaves

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
            node.isWhiteSpaceWithNewline() &&
                (
                    node.treeParent?.elementType == ARROW ||
                        node.treeParent?.elementType == BLOCK ||
                        node.treeParent?.elementType == BINARY_EXPRESSION ||
                        node.treeParent?.elementType == CONDITION ||
                        node.treeParent?.elementType == DOT_QUALIFIED_EXPRESSION ||
                        node.treeParent?.elementType == FUNCTION_LITERAL ||
                        node.treeParent?.elementType == LONG_TEMPLATE_ENTRY_START ||
                        node.treeParent?.elementType == PARENTHESIZED ||
                        node.treeParent?.elementType == PROPERTY ||
                        node.treeParent?.elementType == TYPE_ARGUMENT_LIST ||
                        node.treeParent?.elementType == VALUE_ARGUMENT_LIST ||
                        node.treeParent?.elementType == VALUE_PARAMETER_LIST ||
                        node.treeParent?.elementType == WHEN
                    ) -> {
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
            node.elementType == BLOCK ||
                node.elementType == CLASS_BODY ||
                node.elementType == FUNCTION_LITERAL ||
                node.elementType == STRING_TEMPLATE ||
                node.elementType == LONG_STRING_TEMPLATE_ENTRY ||
                node.elementType == PARENTHESIZED ||
                node.elementType == SUPER_TYPE_ENTRY ||
                node.elementType == SUPER_TYPE_CALL_ENTRY ||
                node.elementType == TYPE_ARGUMENT_LIST ||
                node.elementType == VALUE_ARGUMENT_LIST ||
                node.elementType == VALUE_PARAMETER_LIST ||
                node.elementType == WHEN ||
                node.elementType == WHEN_ENTRY ->
                startIndentContextSameAsParent(node)
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
            false && node.elementType == LPAR && node.nextCodeSibling()?.elementType == CONDITION -> {
                val rpar =
                    requireNotNull(
                        node.nextSibling { it.elementType == RPAR },
                    )
                currentIndentLevel()
                    .let { currentIndentLevel ->
                        startIndentContext(
                            node,
                            rpar,
                            currentIndentLevel + 1,
                            currentIndentLevel + 1,
                        )
                    }
            }
            false && node.elementType == WHILE -> {
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = requireNotNull(node.findChildByType(RPAR)),
                )
            }
            node.elementType == CLASS &&
                node.findChildByType(SUPER_TYPE_LIST) != null ->
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = node.findChildByType(SUPER_TYPE_LIST)!!.lastChildLeafOrSelf(),
                )
            node.elementType == BINARY_EXPRESSION -> {
                if (node.treeParent?.elementType != BINARY_EXPRESSION) {
                    startIndentContextSameAsParent(node)
                }
            }
            node.elementType == DOT_QUALIFIED_EXPRESSION -> {
                if (node.treeParent?.firstChildNode?.elementType != DOT_QUALIFIED_EXPRESSION) {
                    startIndentContextSameAsParent(node)
                }
            }
            node.elementType == IDENTIFIER &&
                node.treeParent.elementType == PROPERTY -> {
                startIndentContextSameAsParent(
                    fromAstNode = node,
                    toAstNode = node.treeParent.lastChildLeafOrSelf(),
                )
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
        val newIndentContext = NewIndentContext(
            fromASTNode = fromAstNode,
            toASTNode = toAstNode,
            nodeIndentLevel = nodeIndentLevel,
            childIndentLevel = childIndentLevel,
            unchanged = true,
        )
        indentContextStack
            .addLast(newIndentContext)
            .also {
                logger.trace {
                    val context =
                        fromAstNode
                            .prevLeaf() // The 'fromAstNode' itself needs to be returned by '.leaves()' call as well
                            ?.leaves()
                            .orEmpty()
                            .takeWhile {
                                // The 'toAstNode' itself nees to be included as well
                                it != toAstNode.nextLeaf()
                            }
                            .joinToString(separator = "") { it.text }
                            .textWithEscapedTabAndNewline()
                    "Create new indent context (same as parent) with level (${newIndentContext.nodeIndentLevel}, ${newIndentContext.childIndentLevel})  for ${fromAstNode.elementType}: $context"
                }
            }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
//        while (indentContextStack.isNotEmpty() && indentContextStack.last.toASTNode == node) {
        Unit
        while (indentContextStack.peekLast()?.toASTNode == node) {
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
            nextLeaf == lastIndexContext.toASTNode &&
                (
                    (
                        nextLeafElementType == RPAR &&
                            nextLeaf.treeParent?.elementType != PARENTHESIZED
                        ) ||
                        nextLeafElementType == RBRACE ||
                        nextLeafElementType == LONG_TEMPLATE_ENTRY_END
                    )
        val adjustedExpectedIndent =
            if (isClosingNode) {
                lastIndexContext.nodeIndentLevel
//                indentContextStack.removeLast()
//                indentContextStack.peekLast().childIndentLevel
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
