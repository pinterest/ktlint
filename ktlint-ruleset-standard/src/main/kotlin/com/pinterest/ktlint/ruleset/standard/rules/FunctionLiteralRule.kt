package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.dropTrailingEolComment
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.findParentByType
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewlineOrNull
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.leavesForwardsIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine20
import com.pinterest.ktlint.rule.engine.core.api.lineLength
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * [Kotlin lang documentation](https://kotlinlang.org/docs/coding-conventions.html#lambdas):
 *
 * When declaring parameter names in a multiline lambda, put the names on the first line, followed by the arrow and the newline:
 *
 * ```
 * appendCommaSeparated(properties) { prop ->
 *     val propertyValue = prop.get(obj)  // ...
 * }
 * ```
 *
 * If the parameter list is too long to fit on a line, put the arrow on a separate line:
 *
 * ```
 * foo {
 *    context: Context,
 *    environment: Env
 *    ->
 *    context.configureEnv(environment)
 * }
 * ```
 */
@SinceKtlint("1.0", EXPERIMENTAL)
@SinceKtlint("1.3", STABLE)
public class FunctionLiteralRule :
    StandardRule(
        id = "function-literal",
        visitorModifiers =
            setOf(
                VisitorModifier.RunAfterRule(CHAIN_METHOD_CONTINUATION_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
            ),
        usesEditorConfigProperties =
            setOf(
                CODE_STYLE_PROPERTY,
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        maxLineLength = editorConfig.maxLineLength()
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == FUNCTION_LITERAL) {
            node
                .findChildByType(VALUE_PARAMETER_LIST)
                ?.let { visitValueParameterList(it, emit) }
            node
                .findChildByType(ARROW)
                ?.let { visitArrow(it, emit) }
            node
                .findChildByType(BLOCK)
                ?.let { visitBlock(it, emit) }
        }
    }

    private fun visitValueParameterList(
        parameterList: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val valueParameters =
            parameterList
                .children20
                .filter { it.elementType == VALUE_PARAMETER }
        if (valueParameters.count() > 1 || parameterList.wrapFirstParameterToNewline()) {
            if (parameterList.textContains('\n') || parameterList.doesNotFitOnSameLineAsStartOfFunctionLiteral()) {
                rewriteToMultilineParameterList(parameterList, emit)
            } else {
                rewriteToSingleLineFunctionLiteral(parameterList, emit)
            }
        } else {
            if (parameterList.textContains('\n')) {
                // Allow:
                //    val foo = {
                //            bar:
                //                @Baz("baz")
                //                Bar
                //        ->
                //        bar()
                //    }
                Unit
            } else {
                // Disallow:
                //    val foo = {
                //            bar ->
                //        bar()
                //    }
                //    val foo = { bar
                //        ->
                //        bar()
                //    }
                rewriteToSingleLineFunctionLiteral(parameterList, emit)
            }
        }
    }

    private fun ASTNode.doesNotFitOnSameLineAsStartOfFunctionLiteral(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST && parent?.elementType == FUNCTION_LITERAL)
        val lineLength =
            lineLengthIncludingLbrace()
                .plus(1) // space before parameter list
                .plus(lengthOfParameterListWhenOnSingleLine())
                .plus(3) // space after parameter list followed by ->
        return lineLength > maxLineLength
    }

    private fun ASTNode.lineLengthIncludingLbrace(): Int {
        require(elementType == VALUE_PARAMETER_LIST && parent?.elementType == FUNCTION_LITERAL)
        val lbrace = parent?.findChildByType(LBRACE)!!
        return lbrace
            .leavesOnLine20
            .dropTrailingEolComment()
            .takeWhile { it.prevLeaf != lbrace }
            .lineLength
    }

    private fun ASTNode.lengthOfParameterListWhenOnSingleLine(): Int {
        require(elementType == VALUE_PARAMETER_LIST)
        val stopAtLeaf = lastChildLeafOrSelf20.nextLeaf
        return firstChildLeafOrSelf20
            .leavesForwardsIncludingSelf
            .takeWhile { it != stopAtLeaf }
            .joinToString(separator = "") {
                if (it.isWhiteSpace20) {
                    // Eliminate newlines and redundant spaces
                    " "
                } else {
                    it.text
                }
            }.length
    }

    private fun ASTNode.exceedsMaxLineLength() = maxLineLength < leavesOnLine20.dropTrailingEolComment().lineLength

    private fun ASTNode.wrapFirstParameterToNewline() =
        if (isFunctionLiteralLambdaWithNonEmptyValueParameterList()) {
            // Disallow when max line is exceeded:
            //    val foo = someCallExpression { someLongParameterName ->
            //        bar()
            //    }
            val stopAtLeaf =
                children20
                    .first { it.elementType == VALUE_PARAMETER }
                    .lastChildLeafOrSelf20
                    .nextLeaf { !it.isWhiteSpaceWithoutNewline20 && !it.isPartOfComment20 }
            leavesOnLine20
                .dropTrailingEolComment()
                .takeWhile { it.prevLeaf != stopAtLeaf }
                .lineLength
                .let { it > maxLineLength }
        } else {
            false
        }

    private fun ASTNode.isFunctionLiteralLambdaWithNonEmptyValueParameterList() =
        takeIf { it.elementType == VALUE_PARAMETER_LIST }
            ?.takeIf { it.findChildByType(VALUE_PARAMETER) != null }
            ?.takeIf { it.parent?.elementType == FUNCTION_LITERAL }
            ?.parent
            ?.takeIf { it.parent?.elementType == LAMBDA_EXPRESSION }
            ?.parent
            ?.takeIf { it.parent?.elementType == LAMBDA_ARGUMENT }
            .let { it != null }

    private fun rewriteToMultilineParameterList(
        parameterList: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(parameterList.elementType == VALUE_PARAMETER_LIST)
        parameterList
            .children20
            .filter { it.elementType == VALUE_PARAMETER }
            .forEach { wrapValueParameter(it, emit) }
        parameterList
            .parent
            ?.findChildByType(ARROW)
            ?.let { arrow -> wrapArrow(arrow, emit) }
        parameterList
            .parent
            ?.findChildByType(RBRACE)
            ?.let { rbrace -> wrapBeforeRbrace(rbrace, emit) }
    }

    private fun wrapValueParameter(
        valueParameter: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(valueParameter.elementType == VALUE_PARAMETER)
        valueParameter
            .prevLeaf
            .takeIf { it.isWhiteSpaceWithoutNewline20 }
            ?.let { whitespaceBeforeValueParameter ->
                emit(valueParameter.startOffset, "Newline expected before parameter", true)
                    .ifAutocorrectAllowed {
                        valueParameter.upsertWhitespaceBeforeMe(
                            indentConfig.childIndentOf(valueParameter.findParentByType(FUNCTION_LITERAL)!!),
                        )
                    }
            }
    }

    private fun wrapArrow(
        arrow: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        wrapBeforeArrow(arrow, emit)
        wrapAfterArrow(arrow, emit)
    }

    private fun wrapBeforeArrow(
        arrow: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(arrow.elementType == ARROW)
        arrow
            .prevLeaf
            .takeIf { it.isWhiteSpaceWithoutNewline20 }
            ?.let {
                emit(arrow.startOffset, "Newline expected before arrow", true)
                    .ifAutocorrectAllowed {
                        arrow.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(arrow.parent!!))
                    }
            }
    }

    private fun wrapAfterArrow(
        arrow: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(arrow.elementType == ARROW)
        arrow
            .nextLeaf
            .takeIf { it.isWhiteSpaceWithoutNewlineOrNull }
            ?.let {
                emit(arrow.startOffset + arrow.textLength - 1, "Newline expected after arrow", true)
                    .ifAutocorrectAllowed {
                        arrow.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(arrow))
                    }
            }
    }

    private fun wrapBeforeRbrace(
        rbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(rbrace.elementType == RBRACE)
        rbrace
            .prevLeaf
            .takeIf { it.isWhiteSpaceWithoutNewlineOrNull }
            ?.let {
                emit(rbrace.startOffset, "Newline expected before closing brace", true)
                    .ifAutocorrectAllowed {
                        rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(rbrace))
                    }
            }
    }

    private fun rewriteToSingleLineFunctionLiteral(
        parameterList: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(parameterList.elementType == VALUE_PARAMETER_LIST)
        parameterList
            .takeUnless { it.isPrecededByComment() }
            ?.prevSibling { it.isWhiteSpaceWithNewline20 }
            ?.let { whitespaceBeforeParameterList ->
                emit(parameterList.startOffset, "No newline expected before parameter", true)
                    .ifAutocorrectAllowed {
                        whitespaceBeforeParameterList.upsertWhitespaceBeforeMe(" ")
                    }
            }
        parameterList
            .nextSibling { it.isWhiteSpace20 }
            ?.takeIf { it.isWhiteSpaceWithNewline20 }
            ?.let { whitespaceAfterParameterList ->
                emit(parameterList.startOffset + parameterList.textLength, "No newline expected after parameter", true)
                    .ifAutocorrectAllowed {
                        whitespaceAfterParameterList.upsertWhitespaceAfterMe(" ")
                    }
            }
    }

    private fun ASTNode.isPrecededByComment() = siblings(forward = false).any { it.isPartOfComment20 }

    private fun visitArrow(
        arrow: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(arrow.elementType == ARROW)
        arrow
            .prevSibling { it.elementType == VALUE_PARAMETER_LIST }
            ?.takeIf { it.hasEmptyParameterList() }
            ?.takeUnless { arrow.isLambdaExpressionNotWrappedInBlock() }
            ?.takeIf { arrow.isFollowedByNonEmptyBlock() }
            ?.let {
                emit(arrow.startOffset, "Arrow is redundant when parameter list is empty", true)
                    .ifAutocorrectAllowed {
                        arrow
                            .nextSibling20
                            .takeIf { it.isWhiteSpace20 }
                            ?.remove()
                        arrow.remove()
                    }
            }
    }

    private fun ASTNode.hasEmptyParameterList(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST)
        return findChildByType(VALUE_PARAMETER) == null
    }

    private fun ASTNode.isLambdaExpressionNotWrappedInBlock(): Boolean {
        require(elementType == ARROW)
        return findParentByType(LAMBDA_EXPRESSION)
            ?.parent
            ?.elementType
            ?.let { parentElementType ->
                // Allow:
                //     val foo = when {
                //         1 == 2 -> { -> "hi" }
                //         else -> { -> "ho" }
                //     }
                // or
                //     val foo = if (cond) { -> "hi" } else { -> "ho" } parent ->
                parentElementType == WHEN_ENTRY || parentElementType == THEN || parentElementType == ELSE
            }
            ?: false
    }

    private fun ASTNode.isFollowedByNonEmptyBlock(): Boolean {
        require(elementType == ARROW)
        return nextSibling { it.elementType == BLOCK }?.firstChildNode != null
    }

    private fun visitBlock(
        block: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(block.elementType == BLOCK)
        if (block.textContains('\n') || block.exceedsMaxLineLength()) {
            block
                .prevCodeSibling20
                ?.let { prevCodeSibling ->
                    when (prevCodeSibling.elementType) {
                        ARROW -> wrapAfterArrow(prevCodeSibling, emit)
                        LBRACE -> wrapAfterLbrace(prevCodeSibling, emit)
                        else -> LOGGER.debug { "Unexpected type of element ${prevCodeSibling.elementType}" }
                    }
                }

            block
                .nextCodeSibling20
                ?.takeIf { it.elementType == RBRACE }
                ?.let { rbrace -> wrapBeforeRbrace(rbrace, emit) }
        }
    }

    private fun wrapAfterLbrace(
        lbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(lbrace.elementType == LBRACE)
        lbrace
            .nextLeaf
            .takeIf { it.isWhiteSpace20 }
            .let { whitespaceAfterLbrace ->
                if (whitespaceAfterLbrace.isWhiteSpaceWithoutNewlineOrNull) {
                    emit(lbrace.startOffset, "Newline expected after opening brace", true)
                        .ifAutocorrectAllowed {
                            lbrace.upsertWhitespaceAfterMe(indentConfig.childIndentOf(lbrace))
                        }
                }
            }
    }
}

public val FUNCTION_LITERAL_RULE_ID: RuleId = FunctionLiteralRule().ruleId
