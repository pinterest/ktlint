package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY_OFF
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule.FunctionBodyExpressionWrapping.always
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule.FunctionBodyExpressionWrapping.default
import com.pinterest.ktlint.ruleset.standard.rules.FunctionSignatureRule.FunctionBodyExpressionWrapping.multiline
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.PropertyValueParser.EnumValueParser
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

@SinceKtlint("0.46", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class FunctionSignatureRule :
    StandardRule(
        id = "function-signature",
        visitorModifiers =
            setOf(
                // Disallow comments at unexpected locations in the type parameter list
                //     fun </* some comment */ T> Foo<T>.foo() {}
                VisitorModifier.RunAfterRule(TYPE_PARAMETER_COMMENT_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                // Disallow comments at unexpected locations in the type argument list
                //     fun Foo<out /* some comment */ Any>.foo() {}
                VisitorModifier.RunAfterRule(TYPE_ARGUMENT_COMMENT_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                // Disallow comments at unexpected locations in the value parameter list
                //     fun foo(
                //        bar /* some comment */: Bar
                //     )
                VisitorModifier.RunAfterRule(VALUE_PARAMETER_COMMENT_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                // Run after wrapping and spacing rules
                VisitorModifier.RunAsLateAsPossible,
            ),
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
                FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY,
                FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY,
            ),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue
    private var functionSignatureWrappingMinimumParameters =
        FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY.defaultValue
    private var functionBodyExpressionWrapping = FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        functionSignatureWrappingMinimumParameters = editorConfig[FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY]
        functionBodyExpressionWrapping = editorConfig[FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY]
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
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

            visitFunctionSignature(node, emit)
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
                endASTNodePredicate = { it == startOfBodyBlock || it == startOfBodyExpression },
            )
    }

    private fun ASTNode.getFirstCodeChild(): ASTNode? {
        val funNode =
            if (elementType == FUN_KEYWORD) {
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
                    if (currentNode.elementType != ANNOTATION &&
                        currentNode.elementType != ANNOTATION_ENTRY &&
                        currentNode.elementType != WHITE_SPACE
                    ) {
                        return currentNode
                    }
                }
                return modifierList.nextCodeSibling()
            }
        return funNode.nextCodeLeaf()
    }

    private fun visitFunctionSignature(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == FUN)

        val forceMultilineSignature =
            node.hasMinimumNumberOfParameters() ||
                node.containsMultilineParameter() ||
                (codeStyle == ktlint_official && node.containsAnnotatedParameter())
        if (isMaxLineLengthSet()) {
            val singleLineFunctionSignatureLength = calculateFunctionSignatureLengthAsSingleLineSignature(node, emit)
            // Function signatures not having parameters, should not be reformatted automatically. It would result in function signatures
            // like below, which are not acceptable:
            //     fun aVeryLongFunctionName(
            //     ) = "some-value"
            //
            //     fun aVeryLongFunctionName(
            //     ): SomeVeryLongTypeName =
            //         SomeVeryLongTypeName(...)
            // Leave it up to the max-line-length rule to detect those violations so that the developer can handle it manually.
            val rewriteFunctionSignatureWithParameters = node.countParameters() > 0 && singleLineFunctionSignatureLength > maxLineLength
            if (forceMultilineSignature || rewriteFunctionSignatureWithParameters) {
                fixWhiteSpacesInValueParameterList(node, emit, multiline = true, dryRun = false)
                if (node.findChildByType(EQ) == null) {
                    fixWhitespaceBeforeFunctionBodyBlock(node, emit, dryRun = false)
                } else {
                    // Due to rewriting the function signature, the remaining length on the last line of the multiline signature needs to be
                    // recalculated
                    val lengthOfLastLine = recalculateRemainingLengthForFirstLineOfBodyExpression(node)
                    fixFunctionBodyExpression(node, emit, maxLineLength - lengthOfLastLine)
                }
            } else {
                fixWhiteSpacesInValueParameterList(node, emit, multiline = false, dryRun = false)
                if (node.findChildByType(EQ) == null) {
                    fixWhitespaceBeforeFunctionBodyBlock(node, emit, dryRun = false)
                } else {
                    fixFunctionBodyExpression(node, emit, maxLineLength - singleLineFunctionSignatureLength)
                }
            }
        } else {
            // When max line length is not set then keep it as single line function signature only when the original
            // signature already was a single line signature. Otherwise, rewrite the entire signature as a multiline
            // signature.
            val rewriteToSingleLineFunctionSignature =
                node
                    .functionSignatureNodes()
                    .none { it.textContains('\n') }
            if (!forceMultilineSignature && rewriteToSingleLineFunctionSignature) {
                fixWhiteSpacesInValueParameterList(node, emit, multiline = false, dryRun = false)
            } else {
                fixWhiteSpacesInValueParameterList(node, emit, multiline = true, dryRun = false)
            }
        }
    }

    private fun recalculateRemainingLengthForFirstLineOfBodyExpression(node: ASTNode): Int {
        val closingParenthesis =
            node
                .findChildByType(VALUE_PARAMETER_LIST)
                ?.findChildByType(RPAR)
        val tailNodesOfFunctionSignature =
            node
                .functionSignatureNodes()
                .childrenBetween(
                    startASTNodePredicate = { it == closingParenthesis },
                    endASTNodePredicate = { false },
                )

        return node.indent(false).length +
            tailNodesOfFunctionSignature.sumOf { it.text.length }
    }

    private fun ASTNode.containsMultilineParameter(): Boolean =
        findChildByType(VALUE_PARAMETER_LIST)
            ?.children()
            .orEmpty()
            .filter { it.elementType == VALUE_PARAMETER }
            .any { it.textContains('\n') }

    private fun ASTNode.containsAnnotatedParameter(): Boolean =
        findChildByType(VALUE_PARAMETER_LIST)
            ?.children()
            .orEmpty()
            .filter { it.elementType == VALUE_PARAMETER }
            .any { it.isAnnotated() }

    private fun ASTNode.isAnnotated() =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun calculateFunctionSignatureLengthAsSingleLineSignature(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ): Int {
        val actualFunctionSignatureLength = node.getFunctionSignatureLength()

        // Calculate the length of the function signature in case it would be rewritten as single line (and without a
        // maximum line length). The white space correction will be calculated via a dry run of the actual fix.
        return actualFunctionSignatureLength +
            // Calculate the white space correction in case the signature would be rewritten to a single line
            fixWhiteSpacesInValueParameterList(node, emit, multiline = false, dryRun = true) +
            if (node.findChildByType(EQ) == null) {
                fixWhitespaceBeforeFunctionBodyBlock(node, emit, dryRun = true)
            } else {
                0
            }
    }

    private fun ASTNode.getFunctionSignatureLength() = indent(false).length + getFunctionSignatureNodesLength()

    private fun ASTNode.getFunctionSignatureNodesLength() =
        functionSignatureNodes()
            .joinTextToString()
            .length

    private fun fixWhiteSpacesInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        multiline: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))
        val firstParameterInList =
            valueParameterList
                .children()
                .firstOrNull { it.elementType == VALUE_PARAMETER }

        whiteSpaceCorrection +=
            if (firstParameterInList == null) {
                // handle empty parameter list
                fixWhiteSpacesInEmptyValueParameterList(node, emit, dryRun)
            } else {
                fixWhiteSpacesBeforeFirstParameterInValueParameterList(node, emit, multiline, dryRun) +
                    fixWhiteSpacesBetweenParametersInValueParameterList(node, emit, multiline, dryRun) +
                    fixWhiteSpaceBeforeClosingParenthesis(node, emit, multiline, dryRun)
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesInEmptyValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        dryRun: Boolean,
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
            }.firstOrNull()
            ?.let { whiteSpace ->
                if (!dryRun) {
                    emit(
                        whiteSpace.startOffset,
                        "No whitespace expected in empty parameter list",
                        true,
                    ).ifAutocorrectAllowed {
                        whiteSpace.treeParent.removeChild(whiteSpace)
                    }
                } else {
                    whiteSpaceCorrection -= whiteSpace.textLength
                }
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesBeforeFirstParameterInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        multiline: Boolean,
        dryRun: Boolean,
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
                    val expectedParameterIndent = indentConfig.childIndentOf(node)
                    if (whiteSpaceBeforeIdentifier == null ||
                        whiteSpaceBeforeIdentifier.text != expectedParameterIndent
                    ) {
                        if (!dryRun) {
                            emit(
                                firstParameterInList.startOffset,
                                "Newline expected after opening parenthesis",
                                true,
                            ).ifAutocorrectAllowed {
                                valueParameterList.firstChildNode.upsertWhitespaceAfterMe(expectedParameterIndent)
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
                                true,
                            ).ifAutocorrectAllowed {
                                whiteSpaceBeforeIdentifier.treeParent.removeChild(whiteSpaceBeforeIdentifier)
                            }
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        multiline: Boolean,
        dryRun: Boolean,
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
                            val expectedParameterIndent = indentConfig.childIndentOf(node)
                            if (whiteSpaceBeforeIdentifier == null ||
                                whiteSpaceBeforeIdentifier.text != expectedParameterIndent
                            ) {
                                if (!dryRun) {
                                    emit(
                                        valueParameter.startOffset,
                                        "Parameter should start on a newline",
                                        true,
                                    ).ifAutocorrectAllowed {
                                        firstChildNodeInValueParameter.upsertWhitespaceBeforeMe(expectedParameterIndent)
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
                                        true,
                                    ).ifAutocorrectAllowed {
                                        firstChildNodeInValueParameter.upsertWhitespaceBeforeMe(" ")
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        multiline: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val newlineAndIndent = node.indent()
        val valueParameterList = requireNotNull(node.findChildByType(VALUE_PARAMETER_LIST))

        val closingParenthesis = valueParameterList.findChildByType(RPAR)
        closingParenthesis
            ?.prevSibling()
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
                                true,
                            ).ifAutocorrectAllowed {
                                closingParenthesis.upsertWhitespaceBeforeMe(newlineAndIndent)
                            }
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
                                true,
                            ).ifAutocorrectAllowed {
                                whiteSpaceBeforeClosingParenthesis.treeParent.removeChild(whiteSpaceBeforeClosingParenthesis)
                            }
                        } else {
                            whiteSpaceCorrection -= whiteSpaceBeforeClosingParenthesis.textLength
                        }
                    }
                }
            }
        return whiteSpaceCorrection
    }

    private fun fixFunctionBodyExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        maxLengthRemainingForFirstLineOfBodyExpression: Int,
    ) {
        val lastNodeOfFunctionSignatureWithBodyExpression =
            node
                .findChildByType(EQ)
                ?.nextLeaf(includeEmpty = true)
                ?: return
        val bodyNodes = node.getFunctionBody(lastNodeOfFunctionSignatureWithBodyExpression)
        val whiteSpaceBeforeFunctionBodyExpression = bodyNodes.getStartingWhitespaceOrNull()
        val functionBodyExpressionNodes = bodyNodes.dropWhile { it.isWhiteSpace() }

        val functionBodyExpressionLines =
            functionBodyExpressionNodes
                .joinTextToString()
                .split("\n")
        functionBodyExpressionLines
            .firstOrNull()
            ?.also { firstLineOfBodyExpression ->
                if (whiteSpaceBeforeFunctionBodyExpression.isWhiteSpaceWithNewline()) {
                    lastNodeOfFunctionSignatureWithBodyExpression
                        .nextCodeSibling()
                        .takeIf { it?.elementType == ANNOTATED_EXPRESSION }
                        ?.let {
                            // Never merge an annotated expression body with function signature as this conflicts with the Annotation rule
                            return
                        }
                    val mergeWithFunctionSignature =
                        when {
                            firstLineOfBodyExpression.length < maxLengthRemainingForFirstLineOfBodyExpression -> {
                                (functionBodyExpressionWrapping == default && !functionBodyExpressionNodes.isMultilineStringTemplate()) ||
                                    (functionBodyExpressionWrapping == multiline && functionBodyExpressionLines.size == 1) ||
                                    node.isMultilineFunctionSignatureWithoutExplicitReturnType(
                                        lastNodeOfFunctionSignatureWithBodyExpression,
                                    )
                            }

                            else -> false
                        }
                    if (mergeWithFunctionSignature) {
                        emit(
                            whiteSpaceBeforeFunctionBodyExpression!!.startOffset,
                            "First line of body expression fits on same line as function signature",
                            true,
                        ).ifAutocorrectAllowed {
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
                                true,
                            ).ifAutocorrectAllowed {
                                functionBodyExpressionNodes
                                    .first()
                                    .upsertWhitespaceBeforeMe(" ")
                            }
                        }
                    } else if (firstLineOfBodyExpression.length + 1 > maxLengthRemainingForFirstLineOfBodyExpression ||
                        (functionBodyExpressionWrapping == multiline && functionBodyExpressionLines.size > 1) ||
                        functionBodyExpressionWrapping == always
                    ) {
                        emit(
                            functionBodyExpressionNodes.first().startOffset,
                            "Newline expected before expression body",
                            true,
                        ).ifAutocorrectAllowed {
                            functionBodyExpressionNodes
                                .first()
                                .upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                        }
                    }
                }
            }
    }

    private fun List<ASTNode>.isMultilineStringTemplate() =
        first { it.isCodeLeaf() }
            .let {
                it.elementType == ElementType.OPEN_QUOTE &&
                    it
                        .nextLeaf()
                        ?.text
                        .orEmpty()
                        .startsWith("\n")
            }

    private fun ASTNode.isMultilineFunctionSignatureWithoutExplicitReturnType(lastNodeOfFunctionSignatureWithBodyExpression: ASTNode?) =
        functionSignatureNodes()
            .childrenBetween(
                startASTNodePredicate = { true },
                endASTNodePredicate = { it == lastNodeOfFunctionSignatureWithBodyExpression },
            ).joinToString(separator = "") { it.text }
            .split("\n")
            .lastOrNull()
            ?.matches(INDENT_WITH_CLOSING_PARENTHESIS)
            ?: false

    private fun fixWhitespaceBeforeFunctionBodyBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        node
            .findChildByType(BLOCK)
            ?.takeIf { it.findChildByType(LBRACE) != null }
            ?.let { block ->
                block
                    .prevLeaf()
                    .takeIf { it.isWhiteSpace() }
                    .let { whiteSpaceBeforeBlock ->
                        if (whiteSpaceBeforeBlock == null || whiteSpaceBeforeBlock.text != " ") {
                            if (!dryRun) {
                                emit(block.startOffset, "Expected a single space before body block", true)
                                    .ifAutocorrectAllowed {
                                        block.upsertWhitespaceBeforeMe(" ")
                                    }
                            } else {
                                whiteSpaceCorrection += 1 - (whiteSpaceBeforeBlock?.textLength ?: 0)
                            }
                        }
                    }
            }

        return whiteSpaceCorrection
    }

    private fun ASTNode.getFunctionBody(splitNode: ASTNode?): List<ASTNode> =
        this
            .collectLeavesRecursively()
            .childrenBetween(
                startASTNodePredicate = { it == splitNode },
                endASTNodePredicate = {
                    // collect all remaining nodes
                    false
                },
            )

    private fun List<ASTNode>.getStartingWhitespaceOrNull() =
        this
            .firstOrNull()
            ?.takeIf { first -> first.isWhiteSpace() }

    private fun isMaxLineLengthSet() = maxLineLength != MAX_LINE_LENGTH_PROPERTY_OFF

    private fun List<ASTNode>.collectLeavesRecursively(): List<ASTNode> = flatMap { it.collectLeavesRecursively() }

    private fun ASTNode.collectLeavesRecursively(): List<ASTNode> =
        if (psi is LeafElement) {
            listOf(this)
        } else {
            children()
                .flatMap { it.collectLeavesRecursively() }
                .toList()
        }

    private fun List<ASTNode>.childrenBetween(
        startASTNodePredicate: (ASTNode) -> Boolean = { _ -> true },
        endASTNodePredicate: (ASTNode) -> Boolean = { _ -> false },
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

    private fun ASTNode.hasMinimumNumberOfParameters(): Boolean = countParameters() >= functionSignatureWrappingMinimumParameters

    private fun ASTNode.countParameters(): Int {
        val valueParameterList = requireNotNull(findChildByType(VALUE_PARAMETER_LIST))
        return valueParameterList
            .children()
            .count { it.elementType == VALUE_PARAMETER }
    }

    public companion object {
        private const val FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET = Int.MAX_VALUE
        public val FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY: EditorConfigProperty<Int> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than",
                        "Force wrapping the parameters of the function signature in case it contains at least the specified " +
                            "number of parameters even in case the entire function signature would fit on a single line. " +
                            "By default this parameter is not enabled.",
                        PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                        setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "unset"),
                    ),
                defaultValue = FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET,
                ktlintOfficialCodeStyleDefaultValue = 2,
                propertyMapper = { property, _ ->
                    if (property?.isUnset == true) {
                        FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET
                    } else {
                        property?.getValueAs<Int>()
                    }
                },
                propertyWriter = { property ->
                    if (property == FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET) {
                        "unset"
                    } else {
                        property.toString()
                    }
                },
            )

        public val FUNCTION_BODY_EXPRESSION_WRAPPING_PROPERTY: EditorConfigProperty<FunctionBodyExpressionWrapping> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_function_signature_body_expression_wrapping",
                        "Determines how to wrap the body of function in case it is an expression. Use 'default' " +
                            "to wrap the body expression only when the first line of the expression does not fit on the same " +
                            "line as the function signature. Use 'multiline' to force wrapping of body expressions that " +
                            "consists of multiple line. Use 'always' to force wrapping of body expression always.",
                        EnumValueParser(FunctionBodyExpressionWrapping::class.java),
                        FunctionBodyExpressionWrapping.entries.map { it.name }.toSet(),
                    ),
                defaultValue = default,
                ktlintOfficialCodeStyleDefaultValue = multiline,
            )

        private val INDENT_WITH_CLOSING_PARENTHESIS = Regex("\\s*\\) =")
    }

    /**
     * Code style to be used while linting and formatting. Note that the [EnumValueParser] requires values to be lowercase.
     */
    @Suppress("EnumEntryName")
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
        always,
    }
}

public val FUNCTION_SIGNATURE_RULE_ID: RuleId = FunctionSignatureRule().ruleId
