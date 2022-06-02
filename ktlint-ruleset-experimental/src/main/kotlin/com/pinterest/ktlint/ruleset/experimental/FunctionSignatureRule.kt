package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.maxLineLengthProperty
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
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

public class FunctionSignatureRule :
    Rule(
        id = "function-signature",
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
            functionSignatureWrappingMinimumParametersProperty
        )

    private var indent: String? = null
    private var maxLineLength = -1
    private var functionSignatureWrappingMinimumParameters = -1

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            functionSignatureWrappingMinimumParameters = node.getEditorConfigValue(
                functionSignatureWrappingMinimumParametersProperty
            )
            val indentConfig = IndentConfig(
                indentStyle = node.getEditorConfigValue(indentStyleProperty),
                tabWidth = node.getEditorConfigValue(indentSizeProperty)
            )
            if (indentConfig.disabled) {
                return
            }
            indent = indentConfig.indent
            maxLineLength = node.getEditorConfigValue(maxLineLengthProperty)
            return
        }

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

    private fun ASTNode.isStartOfBodyExpression(): Boolean =
        elementType == EQ && prevSibling { it.elementType == VALUE_PARAMETER_LIST } != null

    private fun visitFunctionSignature(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        require(node.elementType == FUN)

        if (isMaxLineLengthSet()) {
            val actualFunctionSignatureLength = node.getFunctionSignatureLength()

            // Calculate the length of the function signature in case it would be rewritten as single line (and without a
            // maximum line length). The white space correction will be calculated via a dry run of the actual fix.
            val singleLineFunctionSignatureLength =
                actualFunctionSignatureLength +
                    // Calculate the white space correction in case the signature would be rewritten to a single line
                    fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = true)
            if (singleLineFunctionSignatureLength > maxLineLength ||
                node.hasMinimumNumberOfParameters()
            ) {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = true, dryRun = false)
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
            if (rewriteToSingleLineFunctionSignature) {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = false)
            } else {
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = true, dryRun = false)
            }
        }
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

        val firstChildOfParameter = firstParameterInList.firstChildNode
        firstChildOfParameter
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
                                (firstChildOfParameter as LeafElement).upsertWhitespaceBeforeMe(expectedParameterIndent)
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
                                firstChildOfParameter!!.startOffset,
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
        val leaves = node.collectLeavesRecursively()

        val lastNodeOfFunctionSignatureWithBodyExpression =
            node
                .findChildByType(EQ)
                ?.nextLeaf(includeEmpty = true)
        leaves
            .childrenBetween(
                startASTNodePredicate = { it == lastNodeOfFunctionSignatureWithBodyExpression },
                endASTNodePredicate = {
                    // collect all remaining nodes
                    false
                }
            ).takeIf { it.isNotEmpty() }
            ?.fixFunctionBodyExpression(node, emit, autoCorrect, maxLengthRemainingForFirstLineOfBodyExpression)

        val lastNodeOfFunctionSignatureWithBlockBody =
            node
                .getLastNodeOfFunctionSignatureWithBlockBody()
                ?.nextLeaf(includeEmpty = true)
        leaves
            .childrenBetween(
                startASTNodePredicate = { it == lastNodeOfFunctionSignatureWithBlockBody },
                endASTNodePredicate = {
                    // collect all remaining nodes
                    false
                }
            ).takeIf { it.isNotEmpty() }
            ?.fixFunctionBodyBlock(emit, autoCorrect)
    }

    private fun List<ASTNode>.fixFunctionBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        maxLengthRemainingForFirstLineOfBodyExpression: Int
    ) {
        val (whiteSpaceBeforeFunctionBodyExpression, functionBodyExpression) =
            this
                .let {
                    val whiteSpaceFirst =
                        it.firstOrNull { first -> first.isWhiteSpace() }
                    if (whiteSpaceFirst == null) {
                        Pair(null, it)
                    } else {
                        Pair(
                            it.firstOrNull { first -> first.isWhiteSpace() },
                            it.drop(1)
                        )
                    }
                }

        functionBodyExpression
            .joinTextToString()
            .split("\n")
            .firstOrNull()
            ?.also { firstLineOfBodyExpression ->
                if (firstLineOfBodyExpression.length + 1 > maxLengthRemainingForFirstLineOfBodyExpression) {
                    if (whiteSpaceBeforeFunctionBodyExpression == null ||
                        !whiteSpaceBeforeFunctionBodyExpression.textContains('\n')
                    ) {
                        emit(
                            functionBodyExpression.first().startOffset,
                            "Newline expected before expression body",
                            true
                        )
                        if (autoCorrect) {
                            val newLineAndIndent = "\n" + node.lineIndent() + indent
                            if (whiteSpaceBeforeFunctionBodyExpression != null) {
                                (whiteSpaceBeforeFunctionBodyExpression as LeafPsiElement).rawReplaceWithText(newLineAndIndent)
                            } else {
                                (functionBodyExpression.first() as LeafPsiElement).upsertWhitespaceBeforeMe(newLineAndIndent)
                            }
                        }
                    }
                } else if (whiteSpaceBeforeFunctionBodyExpression?.textContains('\n') == true) {
                    emit(
                        whiteSpaceBeforeFunctionBodyExpression.startOffset,
                        "First line of body expression fits on same line as function signature",
                        true
                    )
                    if (autoCorrect) {
                        (whiteSpaceBeforeFunctionBodyExpression as LeafPsiElement).rawReplaceWithText(" ")
                    }
                }
            }
    }

    private fun List<ASTNode>.fixFunctionBodyBlock(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean
    ) {
        val (whiteSpaceBeforeFunctionBodyExpression, functionBodyBlock) =
            this
                .firstOrNull()
                ?.takeIf { first -> first.isWhiteSpace() }
                ?.let { Pair(it, this.drop(1)) }
                ?: Pair(null, this)

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
        @Suppress("MemberVisibilityCanBePrivate")
        public const val KTLINT_FUNCTION_SIGNATURE_RULE_FORCE_MULTILINE_WITH_AT_LEAST_PARAMETERS: String =
            "ktlint_function_signature_rule_force_multiline_with_at_least_parameters"

        public val functionSignatureWrappingMinimumParametersProperty: UsesEditorConfigProperties.EditorConfigProperty<Int> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    KTLINT_FUNCTION_SIGNATURE_RULE_FORCE_MULTILINE_WITH_AT_LEAST_PARAMETERS,
                    "Force wrapping the parameters of the function signature in case it contains at least the specified " +
                        "number of parameters even in case the entire function signature would fit on a single line. " +
                        "By default this parameter is not enabled.",
                    PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                    setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "unset")
                ),
                defaultValue = -1
            )
    }
}
