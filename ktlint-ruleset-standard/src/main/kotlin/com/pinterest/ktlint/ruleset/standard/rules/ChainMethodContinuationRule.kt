package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARRAY_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACKET
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine
import com.pinterest.ktlint.rule.engine.core.api.lineLengthWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Methods chained with operator '.' or '?.' should all fit on a single line. Otherwise, each chained method should be on a separate line.
 *
 * The Kotlin Coding Conventions https://kotlinlang.org/docs/coding-conventions.html#wrap-chained-calls are a more lenient as it defines
 * these rules:
 *  - When wrapping chained calls, put the . character or the ?. operator on the next line, with a single indent:
 *  - The first call in the chain should usually have a line break before it, but it's OK to omit it if the code makes more sense that way.
 *
 * As of that the rule is restricted to ktlint_official code style unless explicitly enabled.
 */
@SinceKtlint("1.0", EXPERIMENTAL)
public class ChainMethodContinuationRule :
    StandardRule(
        id = "chain-method-continuation",
        visitorModifiers = setOf(RunAfterRule(ARGUMENT_LIST_WRAPPING_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED)),
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength: Int = MAX_LINE_LENGTH_PROPERTY.defaultValue
    private var forceMultilineWhenChainOperatorCountGreaterOrEqualThanProperty =
        FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
        forceMultilineWhenChainOperatorCountGreaterOrEqualThanProperty =
            editorConfig[FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.elementType in chainOperatorTokenSet }
            .takeIf { it?.treeParent?.elementType in chainOperatorExpressionConverterTokenSet }
            ?.let { chainOperator ->
                // Chained methods which have to be aligned vertically live at different levels in the AST hierarchy. To ease the processing
                // the AST hierarchy for nodes contains a chain operator are restructured in a ChainedExpression.
                ChainedExpression
                    .createFrom(chainOperator)
                    .takeIf { chainedExpression ->
                        // When the first chain operator of the expression is found, all other chains operators of that same expression are
                        // processed as well. So when the chain operator is not the first operator of the expression, it should be
                        // considered as being processed already.
                        chainOperator == chainedExpression.chainOperators.first()
                    }?.takeUnless { it.rootASTNode.treeParent.elementType == IMPORT_DIRECTIVE }
                    ?.takeUnless { it.rootASTNode.treeParent.elementType == PACKAGE_DIRECTIVE }
                    ?.takeUnless { it.rootASTNode.treeParent.elementType == LONG_STRING_TEMPLATE_ENTRY }
                    ?.let { chainedExpression ->
                        fixWhitespaceBeforeChainOperators(chainedExpression, emit, autoCorrect)
                        disallowCommentBetweenDotAndCallExpression(chainedExpression, emit)
                        fixWhiteSpaceAfterChainOperators(chainedExpression, emit, autoCorrect)
                    }
            }
    }

    private fun fixWhitespaceBeforeChainOperators(
        chainedExpression: ChainedExpression,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val wrapBeforeEachChainOperator = chainedExpression.wrapBeforeChainOperator()
        val exceedsMaxLineLength = chainedExpression.exceedsMaxLineLength()
        chainedExpression
            .chainOperators
            .filterNot { it.isJavaClassReferenceExpression() }
            .forEach { chainOperator ->
                when {
                    chainOperator.shouldBeOnSameLineAsClosingElementOfPreviousExpressionInMethodChain() -> {
                        removeWhiteSpaceBeforeChainOperator(chainOperator, emit, autoCorrect)
                    }

                    wrapBeforeEachChainOperator || exceedsMaxLineLength || chainOperator.isPrecededByComment() -> {
                        insertWhiteSpaceBeforeChainOperator(chainOperator, emit, autoCorrect)
                    }
                }
            }
    }

    private fun ASTNode.isJavaClassReferenceExpression() =
        treeParent.elementType == DOT_QUALIFIED_EXPRESSION &&
            prevCodeSibling()?.elementType == CLASS_LITERAL_EXPRESSION &&
            nextCodeSibling()?.elementType == REFERENCE_EXPRESSION &&
            nextCodeSibling()?.firstChildLeafOrSelf()?.text == "java"

    private fun ChainedExpression.wrapBeforeChainOperator() =
        when {
            hasNewlineBetweenFirstAndLastChainOperator -> {
                // Disallow:
                //     listOf(1, 2, 3, 4)
                //        .filter { it > 2 }?.filter { it > 3 }
                //        ?.filter { it > 4 }
                // or
                //     listOf(1, 2, 3, 4)
                //        .filter {
                //            it > 2
                //        }?.filter { it > 3 }?.filter { it > 4 }
                // or
                //     listOf(1, 2, 3, 4).filter {
                //        it > 2
                //     }?.filter { it > 3 }
                true
            }

            isChainedExpressionOnStringTemplate() && !hasNewlineAfterLastChainOperator -> {
                // Allow:
                //     """
                //     some text
                //     """.uppercase().replace("foo bar", "bar foo").trimIndent()
                false
            }

            !hasNewlineBeforeFirstChainOperator && !hasNewlineAfterLastChainOperator -> {
                // Allow:
                //     listOf(1, 2, 3).filter { it > 2 }.filter { it > 3 }
                // or:
                //     listOf(1, 2, 3).filter { it > 2 }.filter {
                //         it > 3
                //     }
                chainOperators.size >= forceMultilineWhenChainOperatorCountGreaterOrEqualThanProperty
            }

            else -> false
        }

    private fun ChainedExpression.isChainedExpressionOnStringTemplate() =
        STRING_TEMPLATE ==
            chainOperators
                .first()
                .prevCodeSibling()
                ?.elementType

    private fun ChainedExpression.exceedsMaxLineLength() =
        with(rootASTNode) {
            if (treeParent.elementType == BINARY_EXPRESSION) {
                // Chained expressions which are enclosed inside a binary expression are skipped for now. It depends on the situation
                // whether wrapping on the binary expression takes precedence on the chained expression or vice versa.
                // This can be illustrated with following examples:
                //    - wrapping on chained expression first makes more sense in this case
                //        val foo = listOf("one", "two", "three").filter { it.length > 3 }
                //      resulting in:
                //        val foo = listOf("one", "two", "three")
                //            .filter { it.length > 3 }
                //    - wrapping on binary expression first makes more sense in this case
                //        if (someOtherExpression || listOf("one", "two", "three").any { it.length > 3 }) { ... }
                //      resulting in:
                //        if (someOtherExpression ||
                //            listOf("one", "two", "three").any { it.length > 3 }
                //        ) { ... }
                false
            } else {
                val stopAtLeaf =
                    chainOperators
                        .last()
                        .startOfLambdaArgumentInCallExpressionOrNull()
                        ?: lastChildLeafOrSelf().nextLeaf()
                leavesOnLine()
                    .takeWhile { it != stopAtLeaf }
                    .lineLengthWithoutNewlinePrefix() > maxLineLength
            }
        }

    private fun ASTNode.startOfLambdaArgumentInCallExpressionOrNull(): ASTNode? {
        require(elementType in chainOperatorTokenSet)
        return nextCodeSibling()
            ?.takeIf { it.elementType == CALL_EXPRESSION }
            ?.findChildByType(LAMBDA_ARGUMENT)
            ?.findChildByType(LAMBDA_EXPRESSION)
            ?.findChildByType(FUNCTION_LITERAL)
            ?.findChildByType(LBRACE)
    }

    private fun ASTNode.isPrecededByComment() = treeParent.children().any { it.isPartOfComment() }

    private fun insertWhiteSpaceBeforeChainOperator(
        chainOperator: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        chainOperator
            .prevLeaf()
            .takeIf { it.isWhiteSpace() || it?.isPartOfComment() == true }
            .let { whiteSpaceOrComment ->
                when {
                    whiteSpaceOrComment?.isPartOfComment() == true -> {
                        // In a chained method containing comments before each method in the chain starts on a newline
                        // Disallow:
                        //     fooBar
                        //         .bar { ... }.foo()
                        emit(chainOperator.startOffset, "Expected newline before '${chainOperator.text}'", true)
                        if (autoCorrect) {
                            chainOperator.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(chainOperator.treeParent))
                        }
                    }

                    whiteSpaceOrComment == null || whiteSpaceOrComment.isWhiteSpaceWithoutNewline() -> {
                        // In a multiline chained method each method in the chain starts on a newline
                        // Disallow:
                        //     fooBar
                        //         .bar { ... }.foo()
                        emit(chainOperator.startOffset, "Expected newline before '${chainOperator.text}'", true)
                        if (autoCorrect) {
                            chainOperator.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(chainOperator.treeParent))
                        }
                    }
                }
            }
    }

    private fun ASTNode.shouldBeOnSameLineAsClosingElementOfPreviousExpressionInMethodChain() =
        prevLeaf { !it.isWhiteSpace() }
            ?.takeIf { it.elementType in groupClosingElementType }
            ?.let { closingElement ->
                closingElement.isPrecededByNewline() ||
                    (closingElement.elementType == CLOSING_QUOTE && closingElement.isPartOfMultilineStringTemplate())
            }
            ?: false

    private fun ASTNode.isPrecededByNewline() =
        prevLeaf()
            ?.isWhiteSpaceWithNewline()
            ?: false

    private fun ASTNode.isPartOfMultilineStringTemplate() =
        treeParent
            .takeIf { it.elementType == STRING_TEMPLATE }
            ?.children()
            ?.any { it.text == "\n" }
            ?: false

    private fun removeWhiteSpaceBeforeChainOperator(
        chainOperator: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        chainOperator
            .prevLeaf()
            .takeIf { it.isWhiteSpace() }
            .let { whiteSpaceOrComment ->
                // Disallow:
                //     bar {
                //         ...
                //     }.
                //     foo()
                // or
                //     """
                //     some text
                //     """
                //         .trimIndent()
                if (whiteSpaceOrComment.isWhiteSpaceWithNewline()) {
                    emit(chainOperator.startOffset, "Unexpected newline before '${chainOperator.text}'", true)
                    if (autoCorrect) {
                        whiteSpaceOrComment?.treeParent?.removeChild(whiteSpaceOrComment)
                    }
                }
            }
    }

    private fun disallowCommentBetweenDotAndCallExpression(
        chainedExpression: ChainedExpression,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        chainedExpression
            .chainOperators
            .forEach { chainOperator ->
                chainOperator
                    .nextSibling { !it.isWhiteSpace() }
                    ?.takeIf { it.isPartOfComment() }
                    ?.let { emit(it.startOffset, "No comment expected at this location in method chain", false) }
            }
    }

    private fun fixWhiteSpaceAfterChainOperators(
        chainedExpression: ChainedExpression,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        chainedExpression
            .chainOperators
            .forEach { chainOperator ->
                chainOperator
                    .nextLeaf()
                    .takeIf { it.isWhiteSpaceWithNewline() }
                    ?.let { whiteSpace ->
                        emit(whiteSpace.startOffset - 1, "Unexpected newline after '${chainOperator.text}'", true)
                        if (autoCorrect) {
                            whiteSpace.treeParent.removeChild(whiteSpace)
                        }
                    }
            }
    }

    private data class ChainedExpression(
        val rootASTNode: ASTNode,
        val chainOperators: List<ASTNode>,
        val hasNewlineBeforeFirstChainOperator: Boolean,
        val hasNewlineBetweenFirstAndLastChainOperator: Boolean,
        val hasNewlineAfterLastChainOperator: Boolean,
    ) {
        companion object {
            private val chainableElementTypes =
                TokenSet.create(
                    ARRAY_ACCESS_EXPRESSION,
                    CALL_EXPRESSION,
                    DOT_QUALIFIED_EXPRESSION,
                    POSTFIX_EXPRESSION,
                    PREFIX_EXPRESSION,
                    SAFE_ACCESS_EXPRESSION,
                )

            fun createFrom(astNode: ASTNode): ChainedExpression {
                require(astNode.elementType in chainOperatorTokenSet)
                var chainParent = requireNotNull(astNode.treeParent)
                while (chainParent.treeParent?.elementType in chainableElementTypes) {
                    chainParent = chainParent.treeParent
                }
                return requireNotNull(
                    chainParent.toChainedExpression(),
                ) { "Failed to create chained expression from ${astNode.treeParent.text}" }
            }

            private fun ASTNode.toChainedExpression(): ChainedExpression? =
                when (elementType) {
                    DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION -> {
                        children()
                            .find { it.elementType in chainOperatorTokenSet }
                            ?.let { chainOperator ->
                                createBaseChainedExpression(chainOperator).let { chainedExpression ->
                                    when (chainedExpression.chainOperators.size) {
                                        1 -> chainedExpression.modifyForFirstOperator(chainOperator)
                                        2 -> chainedExpression.modifyForSecondOperator()
                                        else -> chainedExpression
                                    }
                                }
                            }
                    }

                    CALL_EXPRESSION, ARRAY_ACCESS_EXPRESSION, PREFIX_EXPRESSION, POSTFIX_EXPRESSION -> {
                        children()
                            .mapNotNull { it.toChainedExpression() }
                            .singleOrNull()
                    }

                    else -> null
                }

            private fun ASTNode.createBaseChainedExpression(chainOperator: ASTNode): ChainedExpression {
                val chainBefore = chainOperator.prevCodeSibling()?.toChainedExpression()
                val chainAfter = chainOperator.nextCodeSibling()?.toChainedExpression()
                val newlineAfter =
                    chainAfter.containsNewline() ||
                        chainOperator.nextCodeSibling()!!.textContains('\n') ||
                        chainOperator.nextSibling { it.isWhiteSpaceWithNewline() } != null
                val chainOperators =
                    mutableListOf<ASTNode>()
                        .plus(chainBefore?.chainOperators.orEmpty())
                        .plus(chainOperator)
                        .plus(chainAfter?.chainOperators.orEmpty())
                val newlineBefore =
                    chainBefore?.hasNewlineBetweenFirstAndLastChainOperator ?: false ||
                        chainBefore?.hasNewlineAfterLastChainOperator ?: false ||
                        chainOperator.isPrecededByNewlineSibling()
                return ChainedExpression(
                    rootASTNode = this,
                    chainOperators = chainOperators,
                    hasNewlineBeforeFirstChainOperator = chainBefore?.hasNewlineBeforeFirstChainOperator ?: false,
                    hasNewlineBetweenFirstAndLastChainOperator = newlineBefore,
                    hasNewlineAfterLastChainOperator = newlineAfter,
                )
            }

            private fun ChainedExpression.modifyForFirstOperator(chainOperator: ASTNode): ChainedExpression =
                copy(
                    hasNewlineBeforeFirstChainOperator =
                        chainOperator.prevCodeSibling()!!.textContains('\n') || chainOperator.isPrecededByNewlineSibling(),
                    hasNewlineBetweenFirstAndLastChainOperator = false,
                )

            private fun ChainedExpression.modifyForSecondOperator(): ChainedExpression =
                if (hasNewlineBetweenFirstAndLastChainOperator) {
                    this
                } else {
                    // In case the expression before the first chain operator contains an inner newline, it is to be ignored. But a newline
                    // between the last leaf of that expression and the chain operator has to be taken into account.
                    // Allows:
                    //     """
                    //     some text
                    //     """.uppercase().trimIndent()
                    copy(hasNewlineBetweenFirstAndLastChainOperator = chainOperators.first().isPrecededByNewlineSibling())
                }

            private fun ASTNode.isPrecededByNewlineSibling() = prevSibling { it.isWhiteSpaceWithNewline() } != null

            private fun ChainedExpression?.containsNewline() =
                if (this == null) {
                    false
                } else {
                    hasNewlineBeforeFirstChainOperator || hasNewlineBetweenFirstAndLastChainOperator || hasNewlineAfterLastChainOperator
                }
        }
    }

    public companion object {
        private val chainOperatorExpressionConverterTokenSet = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)
        private val chainOperatorTokenSet = TokenSet.create(DOT, SAFE_ACCESS)
        private val groupClosingElementType = TokenSet.create(CLOSING_QUOTE, RBRACE, RBRACKET, RPAR)

        private const val FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET = 4
        public val FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY: EditorConfigProperty<Int> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_chain_method_rule_force_multiline_when_chain_operator_count_greater_or_equal_than",
                        "Force wrapping of chained methods in case and expression contains at least the specified number of chain " +
                            "operators. By default this parameter is set to 4.",
                        PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                        setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "unset"),
                    ),
                defaultValue = FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET,
                propertyMapper = { property, _ ->
                    if (property?.isUnset == true) {
                        FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET
                    } else {
                        property?.getValueAs<Int>()
                    }
                },
                propertyWriter = { property ->
                    if (property == FORCE_MULTILINE_WHEN_CHAIN_OPERATOR_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET) {
                        "unset"
                    } else {
                        property.toString()
                    }
                },
            )
    }
}

public val CHAIN_METHOD_CONTINUATION_RULE_ID: RuleId = ChainMethodContinuationRule().ruleId
