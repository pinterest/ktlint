package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.experimental.FunctionSignatureRule.FunctionBodyExpressionWrapping.always
import com.pinterest.ktlint.ruleset.experimental.FunctionSignatureRule.FunctionBodyExpressionWrapping.default
import com.pinterest.ktlint.ruleset.experimental.FunctionSignatureRule.FunctionBodyExpressionWrapping.multiline
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

public class FunctionSignatureRule :
    Rule(
        id = "$experimentalRulesetId:function-signature",
        visitorModifiers = setOf(
            // Run after wrapping and spacing rules
            VisitorModifier.RunAsLateAsPossible
        )
    ),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            indentSizeProperty,
            indentStyleProperty,
            maxLineLengthProperty,
            forceMultilineWhenParameterCountGreaterOrEqualThanProperty,
            functionBodyExpressionWrappingProperty
        )

    private var indent: String? = null
    private var maxLineLength = -1
    private var functionSignatureWrappingMinimumParameters = -1
    private var functionBodyExpressionWrapping = default

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        with(editorConfigProperties) {
            functionSignatureWrappingMinimumParameters = getEditorConfigValue(forceMultilineWhenParameterCountGreaterOrEqualThanProperty)
            functionBodyExpressionWrapping = getEditorConfigValue(functionBodyExpressionWrappingProperty)
            val indentConfig = IndentConfig(
                indentStyle = getEditorConfigValue(indentStyleProperty),
                tabWidth = getEditorConfigValue(indentSizeProperty)
            )
            indent = indentConfig.indent
            maxLineLength = getEditorConfigValue(maxLineLengthProperty)
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == FUN) {
            node
                .functionSignatureNodes()
                .any { it.elementType == EOL_COMMENT || it.elementType == BLOCK_COMMENT }
                .ifTrue {
                    // Rewriting function signatures in a consistent manner is hard or sometimes even impossible. For
                    // example a multiline signature which could fit on one line can not be rewritten in case it
                    // contains an EOL comment. Rewriting a single line signature which exceeds the max line length to a
                    // multiline signature is hard when it contains block comments. For now, it does not seem worth the
                    // effort to attempt it.
                    return
                }

            visitFunctionSignature(node, emit, autoCorrect)
        }
    }

    private fun ASTNode.functionSignatureNodes(): List<ASTNode> {
        // Find the signature including the element that has to be placed on the same line as the function signature
        //     fun foo(bar: String) {
        // or
        //     fun foo(bar: String) =
        val firstCodeChild = getFirstCodeChild()
        val startOfBodyBlock =
            this
                .findChildByType(BLOCK)
                ?.firstChildNode
        val startOfBodyExpression = this.findChildByType(EQ)
        return collectLeavesRecursively()
            .childrenBetween(
                startASTNodePredicate = { it == firstCodeChild },
                endASTNodePredicate = { it == startOfBodyBlock || it == startOfBodyExpression }
            )
    }

    private fun ASTNode.getFirstCodeChild(): ASTNode? {
        val funNode = if (elementType == FUN_KEYWORD) {
            this.treeParent
        } else {
            this
        }
        funNode
            ?.findChildByType(MODIFIER_LIST)
            ?.let { modifierList ->
                val iterator = modifierList.children().iterator()
                var currentNode: ASTNode
                while (iterator.hasNext()) {
                    currentNode = iterator.next()
                    if (currentNode.elementType != ANNOTATION_ENTRY && currentNode.elementType != WHITE_SPACE) {
                        return currentNode
                    }
                }
                return modifierList.nextCodeSibling()
            }
        return funNode.nextCodeLeaf()
    }

    private fun visitFunctionSignature(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        require(node.elementType == FUN)

        val forceMultilineSignature =
            node.hasMinimumNumberOfParameters() ||
                node.containsParameterPrecededByAnnotationOnSeparateLine()
        if (isMaxLineLengthSet()) {
            val singleLineFunctionSignatureLength = calculateFunctionSignatureLengthAsSingleLineSignature(node, emit, autoCorrect)
            if (forceMultilineSignature ||
                singleLineFunctionSignatureLength > maxLineLength ||
                node.hasMinimumNumberOfParameters()
            ) {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = true, dryRun = false)
                // Due to rewriting the function signature, the remaining length on the last line of the multiline
                // signature needs to be recalculated
                val lengthOfLastLine = recalculateRemainLengthForFirstLineOfBodyExpression(node)
                fixFunctionBody(node, emit, autoCorrect, maxLineLength - lengthOfLastLine)
            } else {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = false)
                fixFunctionBody(node, emit, autoCorrect, maxLineLength - singleLineFunctionSignatureLength)
            }
        } else {
            // When max line length is not set then keep it as single line function signature only when the original
            // signature already was a single line signature. Otherwise, rewrite the entire signature as a multiline
            // signature.
            val rewriteToSingleLineFunctionSignature = node
                .functionSignatureNodes()
                .none { it.textContains('\n') }
            if (!forceMultilineSignature && rewriteToSingleLineFunctionSignature) {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = false)
            } else {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = true, dryRun = false)
            }
        }
    }

    private fun recalculateRemainLengthForFirstLineOfBodyExpression(node: ASTNode): Int {
        val closingParenthesis =
            node
                .findChildByType(VALUE_PARAMETER_LIST)
                ?.findChildByType(RPAR)
        val tailNodesOfFunctionSignature = node
            .functionSignatureNodes()
            .childrenBetween(
                startASTNodePredicate = { it == closingParenthesis },
                endASTNodePredicate = { false }
            )

        return node.lineIndent().length +
            tailNodesOfFunctionSignature.sumOf { it.text.length }
    }

    private fun ASTNode.containsParameterPrecededByAnnotationOnSeparateLine(): Boolean =
        findChildByType(VALUE_PARAMETER_LIST)
            ?.children()
            .orEmpty()
            .filter { it.elementType == VALUE_PARAMETER }
            .mapNotNull {
                // If the value parameter contains a modifier then this list is followed by a white space
                it.findChildByType(MODIFIER_LIST)?.nextSibling { true }
            }.any { it.textContains('\n') }

    private fun calculateFunctionSignatureLengthAsSingleLineSignature(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ): Int {
        val actualFunctionSignatureLength = node.getFunctionSignatureLength()

        // Calculate the length of the function signature in case it would be rewritten as single line (and without a
        // maximum line length). The white space correction will be calculated via a dry run of the actual fix.
        return actualFunctionSignatureLength +
            // Calculate the white space correction in case the signature would be rewritten to a single line
            fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = true)
    }

    private fun ASTNode.getFunctionSignatureLength() = lineIndent().length + getFunctionSignatureNodesLength()

    private fun ASTNode.getFunctionSignatureNodesLength() = functionSignatureNodes()
        .joinTextToString()
        .length

    private fun fixWhiteSpacesInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))
        val firstParameterInList =
            valueParameterList
                .children()
                .firstOrNull { it.elementType == VALUE_PARAMETER }

        whiteSpaceCorrection += if (firstParameterInList == null) {
            // handle empty parameter list
            fixWhiteSpacesInEmptyValueParameterList(node, emit, autoCorrect, dryRun)
        } else {
            fixWhiteSpacesBeforeFirstParameterInValueParameterList(node, emit, autoCorrect, multiline, dryRun) +
                fixWhiteSpacesBetweenParametersInValueParameterList(node, emit, autoCorrect, multiline, dryRun) +
                fixWhiteSpaceBeforeClosingParenthesis(node, emit, autoCorrect, multiline, dryRun)
        }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesInEmptyValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        dryRun: Boolean
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))

        valueParameterList
            .children()
            .filter { it.elementType != LPAR && it.elementType != RPAR }
            .also { elementsInValueParameterList ->
                // Functions with comments in the value parameter list are excluded from processing before. So an "empty" value
                // parameter list should only contain a single whitespace element
                require(elementsInValueParameterList.count() <= 1)
            }
            .firstOrNull()
            ?.let { whiteSpace ->
                if (!dryRun) {
                    emit(
                        whiteSpace.startOffset,
                        "No whitespace expected in empty parameter list",
                        true
                    )
                }
                if (autoCorrect && !dryRun) {
                    whiteSpace.treeParent.removeChild(whiteSpace)
                } else {
                    whiteSpaceCorrection -= whiteSpace.textLength
                }
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesBeforeFirstParameterInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))
        val firstParameterInList =
            valueParameterList
                .children()
                .first { it.elementType == VALUE_PARAMETER }

        val firstParameter = firstParameterInList.firstChildNode
        firstParameter
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceBeforeIdentifier ->
                if (multiline) {
                    val expectedParameterIndent = "\n" + node.lineIndent() + indent
                    if (whiteSpaceBeforeIdentifier == null ||
                        whiteSpaceBeforeIdentifier.text != expectedParameterIndent
                    ) {
                        if (!dryRun) {
                            emit(
                                firstParameterInList.startOffset,
                                "Newline expected after opening parenthesis",
                                true
                            )
                        }
                        if (autoCorrect && !dryRun) {
                            if (whiteSpaceBeforeIdentifier == null) {
                                (valueParameterList.firstChildNode as LeafElement).upsertWhitespaceAfterMe(expectedParameterIndent)
                            } else {
                                (whiteSpaceBeforeIdentifier as LeafElement).rawReplaceWithText(
                                    expectedParameterIndent
                                )
                            }
                        } else {
                            whiteSpaceCorrection += expectedParameterIndent.length - (whiteSpaceBeforeIdentifier?.textLength ?: 0)
                        }
                    }
                } else {
                    if (whiteSpaceBeforeIdentifier != null) {
                        if (!dryRun) {
                            emit(
                                firstParameter!!.startOffset,
                                "No whitespace expected between opening parenthesis and first parameter name",
                                true
                            )
                        }
                        if (autoCorrect && !dryRun) {
                            whiteSpaceBeforeIdentifier.treeParent.removeChild(whiteSpaceBeforeIdentifier)
                        } else {
                            whiteSpaceCorrection -= whiteSpaceBeforeIdentifier.textLength
                        }
                    }
                }
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesBetweenParametersInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))
        val firstParameterInList =
            valueParameterList
                .children()
                .first { it.elementType == VALUE_PARAMETER }

        valueParameterList
            .children()
            .filter { it.elementType == VALUE_PARAMETER }
            .filter { it != firstParameterInList }
            .forEach { valueParameter ->
                val firstChildNodeInValueParameter = valueParameter.firstChildNode
                firstChildNodeInValueParameter
                    ?.prevLeaf()
                    ?.takeIf { it.elementType == WHITE_SPACE }
                    .let { whiteSpaceBeforeIdentifier ->
                        if (multiline) {
                            val expectedParameterIndent = "\n" + node.lineIndent() + indent
                            if (whiteSpaceBeforeIdentifier == null ||
                                whiteSpaceBeforeIdentifier.text != expectedParameterIndent
                            ) {
                                if (!dryRun) {
                                    emit(
                                        valueParameter.startOffset,
                                        "Parameter should start on a newline",
                                        true
                                    )
                                }
                                if (autoCorrect && !dryRun) {
                                    if (whiteSpaceBeforeIdentifier == null) {
                                        (firstChildNodeInValueParameter as LeafElement).upsertWhitespaceBeforeMe(expectedParameterIndent)
                                    } else {
                                        (whiteSpaceBeforeIdentifier as LeafElement).rawReplaceWithText(
                                            expectedParameterIndent
                                        )
                                    }
                                } else {
                                    whiteSpaceCorrection += expectedParameterIndent.length - (whiteSpaceBeforeIdentifier?.textLength ?: 0)
                                }
                            }
                        } else {
                            if (whiteSpaceBeforeIdentifier == null || whiteSpaceBeforeIdentifier.text != " ") {
                                if (!dryRun) {
                                    emit(
                                        firstChildNodeInValueParameter!!.startOffset,
                                        "Single whitespace expected before parameter",
                                        true
                                    )
                                }
                                if (autoCorrect && !dryRun) {
                                    if (whiteSpaceBeforeIdentifier == null) {
                                        (firstChildNodeInValueParameter as LeafElement).upsertWhitespaceBeforeMe(" ")
                                    } else {
                                        (whiteSpaceBeforeIdentifier as LeafElement).rawReplaceWithText(" ")
                                    }
                                } else {
                                    whiteSpaceCorrection += 1 - (whiteSpaceBeforeIdentifier?.textLength ?: 0)
                                }
                            }
                        }
                    }
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpaceBeforeClosingParenthesis(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean
    ): Int {
        var whiteSpaceCorrection = 0

        val newlineAndIndent = "\n" + node.lineIndent()
        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))

        val closingParenthesis = valueParameterList.findChildByType(RPAR)
        closingParenthesis
            ?.prevSibling { true }
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceBeforeClosingParenthesis ->
                if (multiline) {
                    if (whiteSpaceBeforeClosingParenthesis == null ||
                        whiteSpaceBeforeClosingParenthesis.text != newlineAndIndent
                    ) {
                        if (!dryRun) {
                            emit(
                                closingParenthesis!!.startOffset,
                                "Newline expected before closing parenthesis",
                                true
                            )
                        }
                        if (autoCorrect && !dryRun) {
                            (closingParenthesis as LeafElement).upsertWhitespaceBeforeMe(newlineAndIndent)
                        } else {
                            whiteSpaceCorrection += newlineAndIndent.length - (whiteSpaceBeforeClosingParenthesis?.textLength ?: 0)
                        }
                    }
                } else {
                    if (whiteSpaceBeforeClosingParenthesis != null &&
                        whiteSpaceBeforeClosingParenthesis.nextLeaf()?.elementType == RPAR
                    ) {
                        if (!dryRun) {
                            emit(
                                whiteSpaceBeforeClosingParenthesis.startOffset,
                                "No whitespace expected between last parameter and closing parenthesis",
                                true
                            )
                        }
                        if (autoCorrect && !dryRun) {
                            whiteSpaceBeforeClosingParenthesis.treeParent.removeChild(whiteSpaceBeforeClosingParenthesis)
                        } else {
                            whiteSpaceCorrection -= whiteSpaceBeforeClosingParenthesis.textLength
                        }
                    }
                }
            }
        return whiteSpaceCorrection
    }

    private fun fixFunctionBody(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        maxLengthRemainingForFirstLineOfBodyExpression: Int
    ) {
        if (node.findChildByType(EQ) == null) {
            fixFunctionBodyBlock(node, emit, autoCorrect)
        } else {
            fixFunctionBodyExpression(node, emit, autoCorrect, maxLengthRemainingForFirstLineOfBodyExpression)
        }
    }

    private fun fixFunctionBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        maxLengthRemainingForFirstLineOfBodyExpression: Int
    ) {
        val lastNodeOfFunctionSignatureWithBodyExpression =
            node
                .findChildByType(EQ)
                ?.nextLeaf(includeEmpty = true)
                ?: return
        val bodyNodes = node.getFunctionBody(lastNodeOfFunctionSignatureWithBodyExpression)
        val whiteSpaceBeforeFunctionBodyExpression = bodyNodes.getStartingWhitespaceOrNull()
        val functionBodyExpressionNodes = bodyNodes.dropWhile { it.isWhiteSpace() }

        val functionBodyExpressionLines = functionBodyExpressionNodes
            .joinTextToString()
            .split("\n")
        functionBodyExpressionLines
            .firstOrNull()
            ?.also { firstLineOfBodyExpression ->
                if (whiteSpaceBeforeFunctionBodyExpression.isWhiteSpaceWithNewline()) {
                    if (functionBodyExpressionWrapping == default ||
                        (functionBodyExpressionWrapping == multiline && functionBodyExpressionLines.size == 1) ||
                        node.isMultilineFunctionSignatureWithoutExplicitReturnType(lastNodeOfFunctionSignatureWithBodyExpression)
                    ) {
                        emit(
                            whiteSpaceBeforeFunctionBodyExpression!!.startOffset,
                            "First line of body expression fits on same line as function signature",
                            true
                        )
                        if (autoCorrect) {
                            (whiteSpaceBeforeFunctionBodyExpression as LeafPsiElement).rawReplaceWithText(" ")
                        }
                    }
                } else if (whiteSpaceBeforeFunctionBodyExpression == null ||
                    !whiteSpaceBeforeFunctionBodyExpression.textContains('\n')
                ) {
                    if (node.isMultilineFunctionSignatureWithoutExplicitReturnType(lastNodeOfFunctionSignatureWithBodyExpression) &&
                        firstLineOfBodyExpression.length + 1 <= maxLengthRemainingForFirstLineOfBodyExpression
                    ) {
                        if (whiteSpaceBeforeFunctionBodyExpression == null ||
                            whiteSpaceBeforeFunctionBodyExpression.text != " "
                        ) {
                            emit(
                                functionBodyExpressionNodes.first().startOffset,
                                "Single whitespace expected before expression body",
                                true
                            )
                            if (autoCorrect) {
                                if (whiteSpaceBeforeFunctionBodyExpression != null) {
                                    (whiteSpaceBeforeFunctionBodyExpression as LeafPsiElement).rawReplaceWithText(" ")
                                } else {
                                    (functionBodyExpressionNodes.first() as LeafPsiElement).upsertWhitespaceBeforeMe(" ")
                                }
                            }
                        }
                    } else if (firstLineOfBodyExpression.length + 1 > maxLengthRemainingForFirstLineOfBodyExpression ||
                        (functionBodyExpressionWrapping == multiline && functionBodyExpressionLines.size > 1) ||
                        functionBodyExpressionWrapping == always
                    ) {
                        emit(
                            functionBodyExpressionNodes.first().startOffset,
                            "Newline expected before expression body",
                            true
                        )
                        if (autoCorrect) {
                            val newLineAndIndent = "\n" + node.lineIndent() + indent
                            if (whiteSpaceBeforeFunctionBodyExpression != null) {
                                (whiteSpaceBeforeFunctionBodyExpression as LeafPsiElement).rawReplaceWithText(newLineAndIndent)
                            } else {
                                (functionBodyExpressionNodes.first() as LeafPsiElement).upsertWhitespaceBeforeMe(newLineAndIndent)
                            }
                        }
                    }
                }
            }
    }

    private fun ASTNode.isMultilineFunctionSignatureWithoutExplicitReturnType(
        lastNodeOfFunctionSignatureWithBodyExpression: ASTNode?
    ) = functionSignatureNodes()
        .childrenBetween(
            startASTNodePredicate = { true },
            endASTNodePredicate = { it == lastNodeOfFunctionSignatureWithBodyExpression }
        ).joinToString(separator = "") { it.text }
        .split("\n")
        .lastOrNull()
        ?.matches(INDENT_WITH_CLOSING_PARENTHESIS)
        ?: false

    private fun fixFunctionBodyBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val lastNodeOfFunctionSignatureWithBlockBody =
            node
                .getLastNodeOfFunctionSignatureWithBlockBody()
                ?.nextLeaf(includeEmpty = true)
                ?: return

        val bodyNodes = node.getFunctionBody(lastNodeOfFunctionSignatureWithBlockBody)
        val whiteSpaceBeforeFunctionBodyExpression = bodyNodes.getStartingWhitespaceOrNull()
        val functionBodyBlock = bodyNodes.dropWhile { it.isWhiteSpace() }

        functionBodyBlock
            .joinTextToString()
            .split("\n")
            .firstOrNull()
            ?.also { firstLineOfBodyBlock ->
                if (whiteSpaceBeforeFunctionBodyExpression == null) {
                    emit(functionBodyBlock.first().startOffset, "Expected a single space before body block", true)
                    if (autoCorrect) {
                        (functionBodyBlock.first().prevLeaf(true) as LeafPsiElement).upsertWhitespaceAfterMe(" ")
                    }
                } else if (whiteSpaceBeforeFunctionBodyExpression.text != " ") {
                    emit(whiteSpaceBeforeFunctionBodyExpression.startOffset, "Expected a single space", true)
                    if (autoCorrect) {
                        (whiteSpaceBeforeFunctionBodyExpression as LeafPsiElement).rawReplaceWithText(" ")
                    }
                }
            }
    }

    private fun ASTNode.getFunctionBody(splitNode: ASTNode?): List<ASTNode> =
        this
            .collectLeavesRecursively()
            .childrenBetween(
                startASTNodePredicate = { it == splitNode },
                endASTNodePredicate = {
                    // collect all remaining nodes
                    false
                }
            )

    private fun List<ASTNode>.getStartingWhitespaceOrNull() =
        this
            .firstOrNull()
            ?.takeIf { first -> first.isWhiteSpace() }

    private fun List<ASTNode>.getBody() =
        this.dropWhile { it.isWhiteSpace() }

    private fun isMaxLineLengthSet() = maxLineLength > -1

    private fun List<ASTNode>.collectLeavesRecursively(): List<ASTNode> = flatMap { it.collectLeavesRecursively() }

    private fun ASTNode.collectLeavesRecursively(): List<ASTNode> = if (psi is LeafElement) {
        listOf(this)
    } else {
        children()
            .flatMap { it.collectLeavesRecursively() }
            .toList()
    }

    private fun List<ASTNode>.childrenBetween(
        startASTNodePredicate: (ASTNode) -> Boolean = { _ -> true },
        endASTNodePredicate: (ASTNode) -> Boolean = { _ -> false }
    ): List<ASTNode> {
        val iterator = iterator()
        var currentNode: ASTNode
        val childrenBetween: MutableList<ASTNode> = mutableListOf()

        while (iterator.hasNext()) {
            currentNode = iterator.next()
            if (startASTNodePredicate(currentNode)) {
                childrenBetween.add(currentNode)
                break
            }
        }

        while (iterator.hasNext()) {
            currentNode = iterator.next()
            childrenBetween.add(currentNode)
            if (endASTNodePredicate(currentNode)) {
                break
            }
        }

        return childrenBetween
    }

    private fun List<ASTNode>.joinTextToString(block: (ASTNode) -> String = { it.text }): String =
        collectLeavesRecursively().joinToString(separator = "") { block(it) }

    private fun ASTNode.hasMinimumNumberOfParameters(): Boolean =
        functionSignatureWrappingMinimumParameters > 0 && countParameters() >= functionSignatureWrappingMinimumParameters

    private fun ASTNode.countParameters(): Int {
        val valueParameterList = requireNotNull(findChildByType(VALUE_PARAMETER_LIST))
        return valueParameterList
            .children()
            .count { it.elementType == VALUE_PARAMETER }
    }

    private fun ASTNode.getLastNodeOfFunctionSignatureWithBlockBody(): ASTNode? =
        this
            .findChildByType(BLOCK)
            ?.firstChildNode
            ?.prevCodeLeaf()

    public companion object {
        public val forceMultilineWhenParameterCountGreaterOrEqualThanProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than",
                    "Force wrapping the parameters of the function signature in case it contains at least the specified " +
                        "number of parameters even in case the entire function signature would fit on a single line. " +
                        "By default this parameter is not enabled.",
                    PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                    setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "unset")
                ),
                defaultValue = -1
            )

        public val functionBodyExpressionWrappingProperty: UsesEditorConfigProperties.EditorConfigProperty<FunctionBodyExpressionWrapping> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    "ktlint_function_signature_body_expression_wrapping",
                    "Determines how to wrap the body of function in case it is an expression. Use 'default' " +
                        "to wrap the body expression only when the first line of the expression does not fit on the same " +
                        "line as the function signature. Use 'multiline' to force wrapping of body expressions that " +
                        "consists of multiple line. Use 'always' to force wrapping of body expression always.",
                    EnumValueParser(FunctionBodyExpressionWrapping::class.java),
                    FunctionBodyExpressionWrapping.values().map { it.name }.toSet()
                ),
                defaultValue = default
            )

        private val INDENT_WITH_CLOSING_PARENTHESIS = Regex("\\s*\\) =")
    }

    /**
     * Code style to be used while linting and formatting. Note that the [EnumValueParser] requires values to be lowercase.
     */
    @Suppress("EnumEntryName", "ktlint:enum-entry-name-case")
    public enum class FunctionBodyExpressionWrapping {
        /**
         * Keep the first line of the body expression on the same line as the function signature if max line length is
         * not exceeded.
         */
        default,

        /**
         * Force the body expression to start on a separate line in case it is a multiline expression. A single line
         * body expression is wrapped only when it does not fit on the same line as the function signature.
         */
        multiline,

        /**
         * Always force the body expression to start on a separate line.
         */
        always;
    }
}
