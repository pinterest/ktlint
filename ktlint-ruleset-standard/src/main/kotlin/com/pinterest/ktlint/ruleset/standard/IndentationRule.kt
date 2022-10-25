package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.SPACE
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.TAB
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.BINARY_WITH_TYPE
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.CONSTRUCTOR_DELEGATION_CALL
import com.pinterest.ktlint.core.ast.ElementType.CONTEXT_RECEIVER_LIST
import com.pinterest.ktlint.core.ast.ElementType.DELEGATED_SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.DESTRUCTURING_DECLARATION
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FOR
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
import com.pinterest.ktlint.core.ast.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
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
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_CONSTRAINT
import com.pinterest.ktlint.core.ast.ElementType.TYPE_CONSTRAINT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.USER_TYPE
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHERE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.WHILE
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

    private val indentContextStack: Deque<IndentContext> = LinkedList()

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

        when {
            node.isWhiteSpaceWithNewline() -> {
                line++
                if (indentContextStack.peekLast()?.activated == false) {
                    val lastIndentContext = indentContextStack.removeLast()
                    indentContextStack.addLast(
                        lastIndentContext.copy(activated = true),
                    )
                }
                visitNewLineIndentation(node, autoCorrect, emit)
            }

            node.elementType == CLASS_BODY ||
                node.elementType == CONTEXT_RECEIVER_LIST ||
                node.elementType == LONG_STRING_TEMPLATE_ENTRY ||
                node.elementType == SUPER_TYPE_CALL_ENTRY ||
                node.elementType == STRING_TEMPLATE ||
                node.elementType == VALUE_ARGUMENT_LIST ->
                startIndentContext(
                    fromAstNode = node,
                    lastChildIndent = "",
                )

            node.elementType == SECONDARY_CONSTRUCTOR ->
                visitSecondaryConstructor(node)

            node.elementType == PARENTHESIZED &&
                node.treeParent.treeParent.elementType != IF ->
                startIndentContext(node)

            node.elementType == BINARY_WITH_TYPE ||
                node.elementType == SUPER_TYPE_ENTRY ||
                node.elementType == TYPE_ARGUMENT_LIST ||
                node.elementType == TYPE_PARAMETER_LIST ->
                startIndentContext(node)

            node.elementType == DELEGATED_SUPER_TYPE_ENTRY ||
                node.elementType == ANNOTATED_EXPRESSION ||
                node.elementType == TYPE_REFERENCE ->
                startIndentContext(
                    fromAstNode = node,
                    childIndent = "",
                )

            node.elementType == IF ->
                visitIf(node)

            node.elementType == LBRACE ->
                visitLbrace(node)

            node.elementType == VALUE_PARAMETER_LIST &&
                node.treeParent.elementType != FUNCTION_LITERAL ->
                startIndentContext(
                    fromAstNode = node,
                    lastChildIndent = "",
                )

            node.elementType == LPAR &&
                node.nextCodeSibling()?.elementType == CONDITION ->
                visitLparBeforeCondition(node)

            node.elementType == VALUE_PARAMETER ->
                visitValueParameter(node)

            node.elementType == FUN ->
                visitFun(node)

            node.elementType == CLASS ->
                visitClass(node)

            node.elementType == OBJECT_DECLARATION ->
                visitObjectDeclaration(node)

            node.elementType == BINARY_EXPRESSION ->
                visitBinaryExpression(node)

            node.elementType == DOT_QUALIFIED_EXPRESSION ||
                node.elementType == SAFE_ACCESS_EXPRESSION ||
                node.elementType == USER_TYPE -> {
                if (node.treeParent?.elementType != node.elementType) {
                    startIndentContext(node)
                }
            }

            node.elementType == IDENTIFIER &&
                node.treeParent.elementType == PROPERTY ->
                visitIdentifierInProperty(node)

            node.elementType == LITERAL_STRING_TEMPLATE_ENTRY &&
                node.nextCodeSibling()?.elementType == CLOSING_QUOTE ->
                visitWhiteSpaceBeforeClosingQuote(node, autoCorrect, emit)

            node.elementType == WHEN ->
                visitWhen(node)

            node.elementType == WHEN_ENTRY ->
                visitWhenEntry(node)

            node.elementType == WHERE_KEYWORD &&
                node.nextCodeSibling()?.elementType == TYPE_CONSTRAINT_LIST ->
                visitWhereKeywordBeforeTypeConstraintList(node)

            node.elementType == KDOC ->
                visitKdoc(node)

            node.elementType == PROPERTY_ACCESSOR ->
                visitPropertyAccessor(node)

            node.elementType == FOR ||
                node.elementType == WHILE ->
                visitConditionalLoop(node)

            node.elementType == LBRACKET ->
                visitLBracket(node)

            node.elementType == NULLABLE_TYPE ->
                visitNullableType(node)

            node.elementType == DESTRUCTURING_DECLARATION -> visitDestructuringDeclaration(node)

            else -> {
                logger.trace { "No processing for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}" }
            }
        }
    }

    private fun visitSecondaryConstructor(node: ASTNode) {
        node
            .findChildByType(CONSTRUCTOR_DELEGATION_CALL)
            ?.let { constructorDelegationCall ->
                val fromAstNode = node.skipLeadingWhitespaceCommentsAndAnnotations()
                val nextToAstNode = startIndentContext(
                    fromAstNode = constructorDelegationCall,
                ).prevCodeLeaf()

                // Leading annotations and comments should be indented at same level as constructor itself
                if (fromAstNode != node.nextLeaf()) {
                    startIndentContext(
                        fromAstNode = node,
                        toAstNode = nextToAstNode,
                        childIndent = "",
                    )
                }
            }
    }

    private fun visitIf(node: ASTNode) {
        var nextToAstNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(ELSE)
            ?.let { fromAstNode ->
                nextToAstNode = startIndentContext(
                    fromAstNode = fromAstNode,
                    toAstNode = nextToAstNode,
                ).prevCodeLeaf()
            }

        node
            .findChildByType(THEN)
            ?.lastChildLeafOrSelf()
            ?.nextLeaf()
            ?.let { nodeAfterThenBlock ->
                nextToAstNode = startIndentContext(
                    fromAstNode = nodeAfterThenBlock,
                    toAstNode = nextToAstNode,
                    childIndent = "",
                ).prevCodeLeaf()
            }
        node
            .findChildByType(RPAR)
            ?.nextCodeLeaf()
            ?.let { nodeAfterConditionBlock ->
                nextToAstNode = startIndentContext(
                    fromAstNode = nodeAfterConditionBlock,
                    toAstNode = nextToAstNode,
                ).prevCodeLeaf()
            }
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToAstNode,
            lastChildIndent = "", // No indent for the RPAR
        )
    }

    private fun visitLbrace(node: ASTNode) {
        // Outer indent context
        val rbrace = requireNotNull(
            node.nextSibling { it.elementType == RBRACE },
        ) { "Can not find matching rbrace" }
        startIndentContext(
            fromAstNode = node,
            toAstNode = rbrace,
            firstChildIndent = "",
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
                    lastChildIndent = "",
                )
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = arrow.prevCodeLeaf()!!,
                    childIndent = indentConfig.indent.repeat(2),
                )
            }
    }

    private fun visitLparBeforeCondition(node: ASTNode) {
        startIndentContext(
            fromAstNode = requireNotNull(node.nextLeaf()), // Allow to pickup whitespace before condition
            toAstNode = requireNotNull(node.nextCodeSibling()).lastChildLeafOrSelf(), // Ignore whitespace after condition but before rpar
            nodeIndent = currentIndent() + indentConfig.indent,
            childIndent = "",
        )
    }

    private fun visitValueParameter(node: ASTNode) {
        // Inner indent contexts in reversed order
        var nextToAstNode: ASTNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(EQ)
            ?.let { fromAstNode ->
                nextToAstNode = startIndentContext(
                    fromAstNode = fromAstNode,
                    toAstNode = nextToAstNode,
                ).prevCodeLeaf()
            }

        // Leading annotations and comments should be indented at same level as constructor itself
        val fromAstNode = node.skipLeadingWhitespaceCommentsAndAnnotations()
        if (fromAstNode != node.firstChildNode &&
            node.prevSibling { it.isWhiteSpaceWithNewline() } == null &&
            node == node.treeParent.findChildByType(VALUE_PARAMETER)
        ) {
            nextToAstNode = startIndentContext(
                fromAstNode = fromAstNode,
                toAstNode = nextToAstNode,
            ).fromASTNode.prevLeaf { !it.isWhiteSpace() }!!
        } else {
            startIndentContext(
                fromAstNode = node,
                toAstNode = nextToAstNode,
                childIndent = "",
            )
        }
    }

    private fun visitFun(node: ASTNode) {
        // Inner indent contexts in reversed order
        var nextToAstNode: ASTNode = node.lastChildLeafOrSelf()
        val eqOrBlock =
            node.findChildByType(EQ)
                ?: node.findChildByType(BLOCK)
        eqOrBlock?.let {
            nextToAstNode = startIndentContext(
                fromAstNode = eqOrBlock,
                toAstNode = nextToAstNode,
            ).prevCodeLeaf()
        }

        node
            .findChildByType(TYPE_REFERENCE)
            ?.let { typeReference ->
                nextToAstNode = startIndentContext(
                    fromAstNode = typeReference.getPrecedingLeadingCommentsAndWhitespaces(),
                    toAstNode = nextToAstNode,
                ).prevCodeLeaf()
            }

        // Leading annotations and comments should be indented at same level as function itself
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToAstNode,
            childIndent = "",
        )
    }

    private fun visitClass(node: ASTNode) {
        // Inner indent contexts in reversed order
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
                nextToAstNode = startIndentContext(
                    fromAstNode = where.getPrecedingLeadingCommentsAndWhitespaces(),
                    toAstNode = typeConstraintList.lastChildLeafOrSelf(),
                ).prevCodeLeaf()
            }
        node
            .findChildByType(SUPER_TYPE_LIST)
            ?.let { superTypeList ->
                nextToAstNode = startIndentContext(
                    fromAstNode = superTypeList.getPrecedingLeadingCommentsAndWhitespaces(),
                    toAstNode = superTypeList.lastChildLeafOrSelf(),
                ).prevCodeLeaf()
            }

        // Leading annotations and comments should be indented at same level as class itself
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToAstNode,
            childIndent = "",
        )
    }

    private fun visitObjectDeclaration(node: ASTNode) {
        // Inner indent contexts in reversed order
        var nextToAstNode: ASTNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(SUPER_TYPE_LIST)
            ?.let { superTypeList ->
                nextToAstNode = startIndentContext(
                    fromAstNode = superTypeList.getPrecedingLeadingCommentsAndWhitespaces(),
                    toAstNode = superTypeList.lastChildLeafOrSelf(),
                ).prevCodeLeaf()
            }

        // Leading annotations and comments should be indented at same level as class itself
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToAstNode,
            childIndent = "",
        )
    }

    private fun visitBinaryExpression(node: ASTNode) {
        if (isPartOfBinaryExpressionWrappedInCondition(node)) {
            // Complex binary expression are nested in such a way that the indent context of the condition
            // wrapper is not the last node on the stack
            val conditionIndentContext =
                indentContextStack.last { it.fromASTNode.elementType != BINARY_EXPRESSION }
            // Create new indent context for the remainder (operator and right-hand side) of the binary expression
            startIndentContext(
                fromAstNode = node.findChildByType(OPERATION_REFERENCE)!!,
                toAstNode = node.lastChildLeafOrSelf(),
                nodeIndent = conditionIndentContext.nodeIndent,
                childIndent = conditionIndentContext.childIndent,
            )
            startIndentContext(node.firstChildNode)
        } else if (node.treeParent?.elementType != BINARY_EXPRESSION ||
            node.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType == ELVIS
        ) {
            startIndentContext(node)
        }
    }

    private fun visitIdentifierInProperty(node: ASTNode) {
        startIndentContext(
            fromAstNode = node,
            toAstNode = node.treeParent.lastChildLeafOrSelf(),
        )
    }

    private fun visitWhen(node: ASTNode) {
        var nextToASTNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(LPAR)
            ?.let { lpar ->
                nextToASTNode = startIndentContext(
                    fromAstNode = lpar,
                    toAstNode = node.findChildByType(RPAR)!!,
                    lastChildIndent = "",
                ).prevCodeLeaf()
            }
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToASTNode,
        )
    }

    private fun visitWhenEntry(node: ASTNode) {
        node
            .findChildByType(ARROW)
            ?.let { arrow ->
                val outerIndent =
                    if (arrow.nextLeaf()?.elementType == BLOCK) {
                        ""
                    } else {
                        indentConfig.indent
                    }
                startIndentContext(
                    fromAstNode = arrow.nextLeaf()!!,
                    toAstNode = node.lastChildLeafOrSelf(),
                    firstChildIndent = outerIndent,
                    lastChildIndent = outerIndent,
                )
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = arrow,
                    childIndent = "",
                )
            }
    }

    private fun visitWhereKeywordBeforeTypeConstraintList(node: ASTNode) {
        startIndentContext(
            fromAstNode = node,
            toAstNode = node.nextCodeSibling()?.lastChildLeafOrSelf()!!,
            childIndent = TYPE_CONSTRAINT_CONTINUATION_INDENT,
        )
    }

    private fun visitKdoc(node: ASTNode) {
        node
            .findChildByType(KDOC_START)
            ?.nextLeaf()
            ?.let { fromAstNode ->
                startIndentContext(
                    fromAstNode = fromAstNode,
                    toAstNode = node.lastChildLeafOrSelf(),
                    childIndent = KDOC_CONTINUATION_INDENT,
                )
            }
    }

    private fun visitPropertyAccessor(node: ASTNode) {
        var nextToASTNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(EQ)
            ?.let { fromAstNode ->
                nextToASTNode = startIndentContext(
                    fromAstNode = fromAstNode,
                    toAstNode = node.lastChildLeafOrSelf(),
                ).prevCodeLeaf()
            }
        // No indent on preceding annotations and comments
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToASTNode,
            childIndent = "",
        )
    }

    private fun visitConditionalLoop(node: ASTNode) {
        // Inner indent contexts in reversed order
        node
            .findChildByType(BODY)
            ?.takeIf { it.nextCodeLeaf()?.elementType != LBRACE }
            ?.let { rpar ->
                startIndentContext(
                    fromAstNode = rpar,
                    toAstNode = node.lastChildLeafOrSelf(),
                )
            }
        node
            .findChildByType(CONDITION)
            ?.let { fromAstNode ->
                startIndentContext(fromAstNode)
            }
    }

    private fun visitLBracket(node: ASTNode) {
        node
            .treeParent
            .takeUnless {
                // Should be resolved in IntelliJ IDEA default formatter:
                // https://youtrack.jetbrains.com/issue/KTIJ-14859/Too-little-indentation-inside-the-brackets-in-multiple-annotations-with-the-same-target
                it.elementType == ANNOTATION
            }?.findChildByType(RBRACKET)
            ?.let { rbracket ->
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = rbracket,
                    firstChildIndent = "",
                    lastChildIndent = "",
                )
            }
    }

    private fun visitNullableType(node: ASTNode) {
        // Inner indent contexts in reversed order
        var nextToAstNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(RPAR)
            ?.let { fromAstNode ->
                nextToAstNode = startIndentContext(
                    fromAstNode = fromAstNode,
                    toAstNode = nextToAstNode,
                    childIndent = "",
                ).prevCodeLeaf()
            }
        startIndentContext(
            fromAstNode = node,
            toAstNode = nextToAstNode,
        )
    }

    private fun visitDestructuringDeclaration(node: ASTNode) {
        // Inner indent contexts in reversed order
        var nextToAstNode = node.lastChildLeafOrSelf()
        node
            .findChildByType(EQ)
            ?.let { eq ->
                nextToAstNode = startIndentContext(
                    fromAstNode = eq,
                    toAstNode = nextToAstNode,
                ).prevCodeLeaf()
            }

        node
            .findChildByType(RPAR)
            ?.let {
                startIndentContext(
                    fromAstNode = node,
                    toAstNode = nextToAstNode,
                    lastChildIndent = "",
                )
            }
    }

    private fun ASTNode.skipLeadingWhitespaceCommentsAndAnnotations(): ASTNode {
        require(elementType == SECONDARY_CONSTRUCTOR || elementType == VALUE_PARAMETER)
        return findChildByType(MODIFIER_LIST)
            ?.let { modifierList ->
                modifierList
                    .children()
                    .firstOrNull { !it.isWhiteSpace() && !it.isPartOfComment() && it.elementType != ANNOTATION_ENTRY }
                    ?: modifierList.nextCodeSibling()
            }
            ?: children().firstOrNull { !it.isWhiteSpace() && !it.isPartOfComment() }
            ?: this
    }

    private fun ASTNode.getPrecedingLeadingCommentsAndWhitespaces(): ASTNode {
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

    private fun currentIndent() =
        indentContextStack
            .peekLast()
            .indent()

    private fun startIndentContext(
        fromAstNode: ASTNode,
        toAstNode: ASTNode = fromAstNode.lastChildLeafOrSelf(),
        nodeIndent: String = currentIndent(),
        childIndent: String = indentConfig.indent,
        firstChildIndent: String = childIndent,
        lastChildIndent: String = childIndent,
    ): IndentContext =
        IndentContext(
            fromASTNode = fromAstNode,
            toASTNode = toAstNode,
            nodeIndent = nodeIndent,
            firstChildIndent = firstChildIndent,
            childIndent = childIndent,
            lastChildIndent = lastChildIndent,
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
    }

    override fun afterLastNode() {
        require(indentContextStack.isEmpty()) {
            indentContextStack
                .joinToString(prefix = "Stack should be empty:\n\t", separator = "\n\t") { it.toString() }
        }
    }

    private fun visitNewLineIndentation(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.ignoreIndent()) {
            return
        }

        val normalizedNodeIndent = node.normalizedIndent(emit)
        val expectedIndentation = node.expectedIndent()
        val text = node.text
        val nodeIndent = text.substringAfterLast("\n")
        if (nodeIndent != normalizedNodeIndent || normalizedNodeIndent != expectedIndentation) {
            if (normalizedNodeIndent != expectedIndentation) {
                emit(
                    node.startOffset + text.length - nodeIndent.length,
                    "Unexpected indentation (${normalizedNodeIndent.length}) (should be ${expectedIndentation.length})",
                    true,
                )
            } else {
                // Indentation was at correct level but contained invalid indent characters. This violation has already
                // been emitted.
            }
            logger.trace {
                "Line $line: " + (if (!autoCorrect) "would have " else "") + "changed indentation to ${expectedIndentation.length} (from ${normalizedNodeIndent.length}) for ${node.elementType}: ${node.textWithEscapedTabAndNewline()}"
            }
            if (autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText(
                    text.substringBeforeLast("\n") + "\n" + expectedIndentation,
                )
            }
        } else {
            node.processedButNoIndentationChangedNeeded()
        }
    }

    private fun ASTNode.ignoreIndent(): Boolean {
        val nextLeaf = nextLeaf()
        if (text.endsWith("\n") && nextLeaf.isStartOfRawStringLiteral()) {
            processedButNoIndentationChangedNeeded()
            return true // raw strings (""") are allowed at column 0
        }

        nextLeaf
            ?.parent({ it.psi is PsiComment }, strict = false)
            ?.let { comment ->
                if (text.endsWith("\n")) {
                    processedButNoIndentationChangedNeeded()
                    return true // comments are allowed at column 0
                }
                if (comment.textContains('\n') && comment.elementType == BLOCK_COMMENT) {
                    // FIXME: while we cannot assume any kind of layout inside a block comment,
                    // `/*` and `*/` can still be indented
                    processedButNoIndentationChangedNeeded()
                    return true
                }
            }
        return false
    }

    private fun ASTNode?.isStartOfRawStringLiteral(): Boolean =
        this != null && this.elementType == OPEN_QUOTE && this.text == "\"\"\""

    private fun ASTNode.processedButNoIndentationChangedNeeded() =
        logger.trace { "No indentation change required for $elementType: ${textWithEscapedTabAndNewline()}" }

    private fun ASTNode.expectedIndent(): String {
        val lastIndexContext = indentContextStack.peekLast()
        val nextLeaf = nextLeaf()
        val adjustedChildIndent =
            when {
                this == lastIndexContext.fromASTNode.firstChildLeafOrSelf() ||
                    nextLeaf == lastIndexContext.fromASTNode.firstChildLeafOrSelf() ->
                    lastIndexContext.firstChildIndent

                this == lastIndexContext.toASTNode ||
                    nextLeaf == lastIndexContext.toASTNode ->
                    lastIndexContext.lastChildIndent

                else -> lastIndexContext.childIndent
            }
        return lastIndexContext.nodeIndent + adjustedChildIndent
    }

    private fun ASTNode.normalizedIndent(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): String {
        val nodeIndent = text.substringAfterLast("\n")
        return when (indentConfig.indentStyle) {
            SPACE -> {
                if ('\t' in nodeIndent) {
                    emit(
                        startOffset + text.length - nodeIndent.length,
                        "Unexpected tab character(s)",
                        true,
                    )
                    indentConfig.toNormalizedIndent(nodeIndent)
                } else {
                    nodeIndent
                }
            }

            TAB -> {
                val acceptableTrailingSpaces = acceptableTrailingSpaces()
                val nodeIndentWithoutAcceptableTrailingSpaces = nodeIndent.removeSuffix(acceptableTrailingSpaces)
                if (' ' in nodeIndentWithoutAcceptableTrailingSpaces) {
                    emit(
                        startOffset + text.length - nodeIndent.length,
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
        IndentContext(
            fromASTNode = node,
            nodeIndent = "",
            firstChildIndent = "",
            childIndent = "",
            lastChildIndent = "",
            activated = true,
        )

    private companion object {
        const val KDOC_CONTINUATION_INDENT = " "
        const val TYPE_CONSTRAINT_CONTINUATION_INDENT = "      " // Length of keyword "where" plus separating space
    }

    private data class IndentContext(
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
         * True when the indentation level of this context is activated
         */
        val activated: Boolean = false,
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

        fun indent() =
            if (activated) {
                nodeIndent + childIndent
            } else {
                nodeIndent
            }

        fun prevCodeLeaf() =
            fromASTNode.prevCodeLeaf()!!
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
