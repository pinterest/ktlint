package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.SPACE
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.TAB
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.CONSTRUCTOR_DELEGATION_CALL
import com.pinterest.ktlint.core.ast.ElementType.DELEGATED_SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
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
import com.pinterest.ktlint.core.ast.ElementType.LBRACKET
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PARENTHESIZED
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACKET
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.SHORT_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.TYPE_CONSTRAINT
import com.pinterest.ktlint.core.ast.ElementType.TYPE_CONSTRAINT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHERE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.firstChildLeafOrSelf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeLeaf
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

public class IndentationRule :
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
            indentContextStack.addLast(startNoIndentZone(node))
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

            node.elementType == LONG_STRING_TEMPLATE_ENTRY &&
                node.treeParent.prevSibling { !it.isPartOfComment() }?.isWhiteSpaceWithNewline() == true -> {
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = indentConfig.indent,
                    lastChildIndent = "",
                )
            }

            node.elementType == CLASS_BODY ||
                node.elementType == LONG_STRING_TEMPLATE_ENTRY ||
                node.elementType == SUPER_TYPE_CALL_ENTRY ||
                node.elementType == STRING_TEMPLATE ||
                node.elementType == VALUE_ARGUMENT_LIST ||
                node.elementType == WHEN ->
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = indentConfig.indent,
                    lastChildIndent = "",
                )

            node.elementType == SECONDARY_CONSTRUCTOR -> {
                // Outer indent context
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )

                // Inner indent contexts in reversed order
                node
                    .findChildByType(CONSTRUCTOR_DELEGATION_CALL)
                    ?.lastChildLeafOrSelf()
                    ?.let { toAstNode ->
                        startIndentContext(
                            fromAstNode = node,
                            toAstNode = toAstNode,
                            nodeIndent = currentIndent(),
                            childIndent = indentConfig.indent,
                        )
                    }
            }

            node.elementType == PARENTHESIZED &&
                node.treeParent.treeParent.elementType != IF -> {
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = indentConfig.indent,
                )
            }

            node.elementType == SUPER_TYPE_ENTRY -> {
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = indentConfig.indent,
                )
            }

            node.elementType == DELEGATED_SUPER_TYPE_ENTRY -> {
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )
            }

            node.elementType == IF -> {
                // Outer indent context
                var nextToAstNode = startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                ).toASTNode

                // Inner indent contexts in reversed order
                node
                    .findChildByType(THEN)
                    ?.lastChildLeafOrSelf()
                    ?.nextCodeLeaf()
                    ?.let { nodeAfterThenBlock ->
                        nextToAstNode = startIndentContext(
                            fromAstNode = nodeAfterThenBlock,
                            toAstNode = nextToAstNode,
                            nodeIndent = currentIndent(),
                            firstChildIndent = "", // The "else" keyword should not be indented
                            childIndent = indentConfig.indent,
                        ).fromASTNode.prevCodeLeaf()!!
                    }
                node
                    .findChildByType(RPAR)
                    ?.lastChildLeafOrSelf()
                    ?.nextCodeLeaf()
                    ?.let { nodeAfterConditionBlock ->
                        nextToAstNode = startIndentContext(
                            fromAstNode = nodeAfterConditionBlock,
                            toAstNode = nextToAstNode,
                            nodeIndent = currentIndent(),
                            childIndent = indentConfig.indent,
                        ).fromASTNode.prevCodeLeaf()!!
                    }
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = nextToAstNode,
                    nodeIndent = currentIndent(),
                    childIndent = indentConfig.indent,
                    lastChildIndent = "", // No indent for the RPAR
                )
            }

            node.elementType == LBRACE -> {
                // Outer indent context
                val rbrace = requireNotNull(
                    node.nextSibling { it.elementType == RBRACE },
                ) { "Can not find matching rbrace" }
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = rbrace,
                    nodeIndent = currentIndent(),
                    firstChildIndent = "",
                    childIndent = indentConfig.indent,
                    lastChildIndent = "",
                )

                // Inner indent context in reversed order
                node
                    .treeParent
                    ?.takeIf { it.elementType == FUNCTION_LITERAL }
                    ?.findChildByType(ARROW)
                    ?.let { arrow ->
                        startIndentContext(
                            fromAstNode = arrow,
                            toAstNode = rbrace,
                            nodeIndent = currentIndent(),
                            childIndent = indentConfig.indent,
                            lastChildIndent = "",
                        )
                        startIndentContext(
                            fromAstNode = node,
                            toAstNode = arrow.prevCodeLeaf()!!,
                            nodeIndent = currentIndent(),
                            childIndent = indentConfig.indent.repeat(2),
                        )
                    }
            }

            node.elementType == VALUE_PARAMETER_LIST &&
                node.treeParent.elementType != FUNCTION_LITERAL ->
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = indentConfig.indent,
                    lastChildIndent = "",
                )

            node.elementType == LPAR && node.nextCodeSibling()?.elementType == CONDITION -> {
                startIndentContext(
                    fromAstNode = requireNotNull(node.nextLeaf()), // Allow to pickup whitespace before condition
                    toAstNode = requireNotNull(node.nextCodeSibling()).lastChildLeafOrSelf(), // Ignore whitespace after condition but before rpar
                    nodeIndent = currentIndent() + indentConfig.indent,
                    childIndent = "",
                )
            }

            node.elementType == VALUE_PARAMETER -> {
                // Outer indent context
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )

                // Sub indent contexts in reversed order
                var nextToAstNode: ASTNode = node.lastChildLeafOrSelf()
                node
                    .findChildByType(EQ)
                    ?.let { fromAstNode ->
                        nextToAstNode = startIndentContextSameAsParent(
                            fromAstNode = fromAstNode,
                            toAstNode = nextToAstNode,
                        ).fromASTNode.prevLeaf()!!
                    }

                // Leading annotations and comments should be indented at same level as function itself
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = nextToAstNode,
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )
            }

            node.elementType == FUN -> {
                // Outer indent context
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )

                // Sub indent contexts in reversed order
                var nextToAstNode: ASTNode = node.lastChildLeafOrSelf()
                val eqOrBlock =
                    node.findChildByType(EQ)
                        ?: node.findChildByType(BLOCK)
                eqOrBlock?.let {
                    nextToAstNode = startIndentContextSameAsParent(
                        fromAstNode = eqOrBlock,
                        toAstNode = nextToAstNode,
                    ).fromASTNode.prevLeaf()!!
                }

                node
                    .findChildByType(TYPE_REFERENCE)
                    ?.let { typeReference ->
                        nextToAstNode = startIndentContextSameAsParent(
                            fromAstNode = typeReference.startOfIndentContext(),
                            toAstNode = nextToAstNode,
                        ).fromASTNode.prevLeaf()!!
                    }

                // Leading annotations and comments should be indented at same level as function itself
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = nextToAstNode,
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )
            }

            node.elementType == CLASS -> {
                // Outer indent context
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )

                // Sub indent contexts in reversed order
                var nextToAstNode: ASTNode = node.lastChildLeafOrSelf()
                node
                    .findChildByType(WHERE_KEYWORD)
                    ?.let { where ->
                        val typeConstraintList =
                            requireNotNull(
                                where.nextCodeSibling(),
                            ) { "Can not find code sibling after WHERE in CLASS" }
                        require(
                            typeConstraintList.elementType == TYPE_CONSTRAINT_LIST,
                        ) { "Code sibling after WHERE in CLASS is not a TYPE_CONSTRAINT_LIST" }
                        nextToAstNode = startIndentContextSameAsParent(
                            fromAstNode = where.startOfIndentContext(),
                            toAstNode = typeConstraintList.lastChildLeafOrSelf(),
                        ).fromASTNode.prevCodeLeaf()!!
                    }
                node
                    .findChildByType(SUPER_TYPE_LIST)
                    ?.let { superTypeList ->
                        nextToAstNode = startIndentContextSameAsParent(
                            fromAstNode = superTypeList.startOfIndentContext(),
                            toAstNode = superTypeList.lastChildLeafOrSelf(),
                        ).fromASTNode.prevCodeLeaf()!!
                    }

                // Leading annotations and comments should be indented at same level as class itself
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = nextToAstNode,
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )
            }

            node.elementType == BINARY_EXPRESSION -> {
                if (isPartOfBinaryExpressionWrappedInCondition(node)) {
                    startIndentContextSameAsParent(
                        fromAstNode = node.firstChildNode,
                        toAstNode = node.firstChildNode.lastChildLeafOrSelf(),
                    )
                } else if (node.treeParent?.elementType != BINARY_EXPRESSION ||
                    node.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType == ELVIS
                ) {
                    startIndentContextSameAsParent(node)
                }
            }

            node.elementType == DOT_QUALIFIED_EXPRESSION ||
                node.elementType == SAFE_ACCESS_EXPRESSION -> {
                if (node.treeParent?.elementType != node.elementType) {
                    startIndentContext(
                        fromAstNode = node,
                        toAstNode = node.lastChildLeafOrSelf(),
                        nodeIndent = currentIndent(),
                        childIndent = indentConfig.indent,
                    )
                }
            }

            node.elementType == IDENTIFIER &&
                node.treeParent.elementType == PROPERTY -> {
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
                // Outer indent context
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )

                // Inner indent contexts in reversed order
                val arrow =
                    requireNotNull(
                        node.findChildByType(ARROW),
                    ) { "Can not find arrow in when entry" }
                val firstAndLastIndent =
                    if (arrow.nextLeaf()?.elementType == BLOCK) {
                        ""
                    } else {
                        indentConfig.indent
                    }
                startIndentContext(
                    fromAstNode = arrow.nextLeaf()!!,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    firstChildIndent = firstAndLastIndent,
                    childIndent = indentConfig.indent,
                    lastChildIndent = firstAndLastIndent,
                )
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = arrow,
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )
            }

            node.elementType == WHERE_KEYWORD &&
                node.nextCodeSibling()?.elementType == TYPE_CONSTRAINT_LIST -> {
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.nextCodeSibling()?.lastChildLeafOrSelf()!!,
                    nodeIndent = currentIndent(),
                    childIndent = TYPE_CONSTRAINT_CONTINUATION_INDENT,
                )
            }

            node.elementType == PROPERTY_ACCESSOR -> {
                // Outer indent context
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = node.lastChildLeafOrSelf(),
                    nodeIndent = currentIndent(),
                    childIndent = "",
                )

                // Inner indent contexts in reversed order
                node
                    .findChildByType(EQ)
                    ?.let { fromAstNode ->
                        startIndentContext(
                            fromAstNode = fromAstNode,
                            toAstNode = node.lastChildLeafOrSelf(),
                            nodeIndent = currentIndent(),
                            childIndent = indentConfig.indent,
                        )
                    }
            }

            node.elementType == LBRACKET -> {
                node
                    .treeParent
                    .findChildByType(RBRACKET)
                    ?.let { rbracket ->
                        startIndentContext(
                            fromAstNode = node,
                            toAstNode = rbracket,
                            nodeIndent = currentIndent(),
                            firstChildIndent = "",
                            childIndent = indentConfig.indent,
                            lastChildIndent = "",
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
            indentContextStack.addLast(startNoIndentZone(node))
        }
    }

    private fun ASTNode.startOfIndentContext(): ASTNode {
        var fromAstNode: ASTNode? = this
        while (fromAstNode?.prevLeaf() != null &&
            (fromAstNode.prevLeaf().isWhiteSpace() || fromAstNode.prevLeaf()?.isPartOfComment() == true)
        ) {
            fromAstNode = fromAstNode.prevLeaf()
        }
        return fromAstNode!!
    }

    private fun isPartOfBinaryExpressionWrappedInCondition(node: ASTNode) =
        node
            .parents()
            .takeWhile { it.elementType == BINARY_EXPRESSION || it.elementType == CONDITION }
            .lastOrNull()
            ?.elementType == CONDITION

    private fun startIndentContextSameAsParent(
        fromAstNode: ASTNode,
        toAstNode: ASTNode = fromAstNode.lastChildLeafOrSelf(),
    ) =
        startIndentContext(
            fromAstNode = fromAstNode,
            toAstNode = toAstNode,
            nodeIndent = currentIndent(),
            childIndent = indentConfig.indent,
        )

    private fun currentIndent() =
        indentContextStack
            .peekLast()
            .indent()

    private fun startIndentContext(
        fromAstNode: ASTNode,
        toAstNode: ASTNode,
        nodeIndent: String,
        childIndent: String,
        firstChildIndent: String = childIndent, // TODO: fix order
        lastChildIndent: String = childIndent,
    ): NewIndentContext =
        NewIndentContext(
            fromASTNode = fromAstNode,
            toASTNode = toAstNode,
            nodeIndent = nodeIndent,
            firstChildIndent = firstChildIndent,
            childIndent = childIndent,
            lastChildIndent = lastChildIndent,
            unchanged = true,
        ).also { newIndentContext ->
            logger.trace {
                val nodeIndentLevel = indentConfig.indentLevelFrom(newIndentContext.nodeIndent)
                val childIndentLevel = indentConfig.indentLevelFrom(newIndentContext.childIndent)
                "Create new indent context (same as parent) with level ($nodeIndentLevel, $childIndentLevel) for ${fromAstNode.elementType}: ${newIndentContext.nodes}"
            }
            indentContextStack.addLast(newIndentContext)
        }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        while (indentContextStack.peekLast()?.toASTNode == node) {
            logger.trace {
                val indentContext = indentContextStack.peekLast()
                val nodeIndentLevel = indentConfig.indentLevelFrom(indentContext.nodeIndent)
                val childIndentLevel = indentConfig.indentLevelFrom(indentContext.childIndent)
                "Remove indent context with level ($nodeIndentLevel, $childIndentLevel) for ${indentContext.fromASTNode.elementType}: ${indentContext.nodes}"
            }
            indentContextStack
                .removeLast()
                .also {
                    logger.trace {
                        val indentContext = indentContextStack.peekLast()
                        val nodeIndentLevel = indentConfig.indentLevelFrom(indentContext.nodeIndent)
                        val childIndentLevel = indentConfig.indentLevelFrom(indentContext.childIndent)
                        "Last indent context with level ($nodeIndentLevel, $childIndentLevel) for ${indentContext.fromASTNode.elementType}: ${indentContext.nodes}"
                    }
                }
        }

        when {
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
                        nodeIndent = conditionIndentContext.nodeIndent,
                        childIndent = conditionIndentContext.childIndent,
                    )
                }
            }
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
        val adjustedChildIndent =
            when {
                node == lastIndexContext.fromASTNode.firstChildLeafOrSelf() ||
                    nextLeaf == lastIndexContext.fromASTNode.firstChildLeafOrSelf() ->
                    lastIndexContext.firstChildIndent

                node == lastIndexContext.toASTNode ||
                    nextLeaf == lastIndexContext.toASTNode ->
                    lastIndexContext.lastChildIndent

                else -> lastIndexContext.childIndent
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
                    val acceptableTrailingSpaces = node.acceptableTrailingSpaces()
                    val nodeIndentWithoutAcceptableTrailingSpaces = nodeIndent.removeSuffix(acceptableTrailingSpaces)
                    if (' ' in nodeIndentWithoutAcceptableTrailingSpaces) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected space character(s)",
                            true,
                        )
                        indentConfig
                            .toNormalizedIndent(nodeIndentWithoutAcceptableTrailingSpaces)
                            .plus(acceptableTrailingSpaces)
                    } else {
                        nodeIndent
                    }
                }
            }
        val kdocContinuationIndent =
            if (comment?.elementType == KDOC && nextLeafElementType != KDOC_START) {
                // +1 space before * in `/**\n *\n */`
                KDOC_CONTINUATION_INDENT
            } else {
                ""
            }
        val expectedIndentation = lastIndexContext.nodeIndent + adjustedChildIndent + kdocContinuationIndent
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
        stringTemplateIndenter.visitClosingQuotes(currentIndent(), node.treeParent, autoCorrect, emit)
    }

    private fun ASTNode.acceptableTrailingSpaces(): String {
        require(elementType == WHITE_SPACE)
        val acceptableTrailingSpaces = when (nextLeaf()?.elementType) {
            KDOC_LEADING_ASTERISK, KDOC_END -> {
                // The indentation of a KDoc comment contains a space as the last character regardless of the indentation
                // style (tabs or spaces) except for the starting line of the KDoc comment
                KDOC_CONTINUATION_INDENT
            }

            TYPE_CONSTRAINT -> {
                // 6 spaces (length of "where" keyword plus a separator space) to indent type constraints as below:
                //    where A1 : RecyclerView.Adapter<V1>,
                //               A1 : ComposableAdapter.ViewTypeProvider,
                TYPE_CONSTRAINT_CONTINUATION_INDENT
            }

            else -> ""
        }
        val nodeIndent = text.substringAfterLast("\n")
        return if (nodeIndent.endsWith(acceptableTrailingSpaces)) {
            return acceptableTrailingSpaces
        } else {
            ""
        }
    }

    private fun startNoIndentZone(node: ASTNode) =
        NewIndentContext(
            fromASTNode = node,
            nodeIndent = "",
            firstChildIndent = "",
            childIndent = "",
            lastChildIndent = "",
            unchanged = false,
        )

    private companion object {
        const val KDOC_CONTINUATION_INDENT = " "
        const val TYPE_CONSTRAINT_CONTINUATION_INDENT = "      " // Length of keyword "where" plus separating space
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
         * Cumulative indentation for the node. Normally this should be equal to a multiple of the
         * 'indentConfig.indentStyle' to ensure a consistent indentation style.
         */
        val nodeIndent: String,

        /**
         * Additional indentation for first child node. Normally this should be equal to the 'indentConfig.indent' to
         * ensure a consistent indentation style. In very limited cases when the default indentation is set to 'tab' it
         * is still needed to adjust the last indentation using spaces.
         */
        val firstChildIndent: String,

        /**
         * Additional indentation for child nodes. Normally this should be equal to the 'indentConfig.indent' to ensure
         * a consistent indentation style. In very limited cases when the default indentation is set to 'tab' it is
         * still needed to adjust the last indentation using spaces.
         */
        val childIndent: String,

        /**
         * Additional indentation for last child node. Normally this should be equal to the 'indentConfig.indent' to
         * ensure a consistent indentation style. In very limited cases when the default indentation is set to 'tab' it
         * is still needed to adjust the last indentation using spaces.
         */
        val lastChildIndent: String,

        /**
         * True when the indentation level for child nodes has been raised
         */
        val unchanged: Boolean,
    ) {
//        init {
//            require(fromASTNode.elementType != WHITE_SPACE) {
//                "Indent context should not start at whitespace node as such node might be altered or replaced by " +
//                    "this rule and as a result could result in a corrupted indent context stack"
//            }
//            require(toASTNode.elementType != WHITE_SPACE) {
//                "Indent context should not end at whitespace node as such node might be altered or replaced by " +
//                    "this rule and as a result could result in a corrupted indent context stack"
//            }
//        }

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

        fun indent() =
            if (unchanged) {
                nodeIndent
            } else {
                nodeIndent + childIndent
            }
    }
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
        expectedIndent: String,
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
                val correctedExpectedIndent =
                    if (node.prevLeaf()?.text == "\n") {
                        // In case the opening quotes are placed at the start of the line, then the closing quotes
                        // should have no indent as well.
                        ""
                    } else {
                        expectedIndent
                    }
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
                                    it.getFirstElementOnSameLine().text.splitIndentAt(correctedExpectedIndent.length)
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
                                        correctedExpectedIndent + actualContent,
                                    )
                                }
                            } else if (actualIndent != correctedExpectedIndent && it.isIndentBeforeClosingQuote()) {
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
                                            LeafPsiElement(REGULAR_STRING_PART, correctedExpectedIndent),
                                        )
                                    } else {
                                        (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                            correctedExpectedIndent + actualContent,
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
     * Splits the string at the given index or at the first non-white space character before that index. The returned pair
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
