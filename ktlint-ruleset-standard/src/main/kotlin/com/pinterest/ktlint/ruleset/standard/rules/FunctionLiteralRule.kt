package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesIncludingSelf
import com.pinterest.ktlint.rule.engine.core.api.leavesOnLine
import com.pinterest.ktlint.rule.engine.core.api.lineLengthWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

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
    ),
    Rule.Experimental {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
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
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == FUNCTION_LITERAL) {
            node
                .findChildByType(VALUE_PARAMETER_LIST)
                ?.let { visitValueParameterList(it, autoCorrect, emit) }
            node
                .findChildByType(ARROW)
                ?.let { visitArrow(it, autoCorrect, emit) }
            node
                .findChildByType(BLOCK)
                ?.let { visitBlock(it, autoCorrect, emit) }
        }
    }

    private fun visitValueParameterList(
        parameterList: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val valueParameters =
            parameterList
                .children()
                .filter { it.elementType == VALUE_PARAMETER }
        if (valueParameters.count() > 1 || parameterList.wrapFirstParameterToNewline()) {
            if (parameterList.textContains('\n') || parameterList.doesNotFitOnSameLineAsStartOfFunctionLiteral()) {
                rewriteToMultilineParameterList(parameterList, autoCorrect, emit)
            } else {
                rewriteToSingleLineFunctionLiteral(parameterList, emit, autoCorrect)
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
                rewriteToSingleLineFunctionLiteral(parameterList, emit, autoCorrect)
            }
        }
    }

    private fun ASTNode.doesNotFitOnSameLineAsStartOfFunctionLiteral(): Boolean {
        require(elementType == VALUE_PARAMETER_LIST && treeParent.elementType == FUNCTION_LITERAL)
        val lineLength =
            lineLengthIncludingLbrace()
                .plus(1) // space before parameter list
                .plus(lengthOfParameterListWhenOnSingleLine())
                .plus(3) // space after parameter list followed by ->
        return lineLength > maxLineLength
    }

    private fun ASTNode.lineLengthIncludingLbrace(): Int {
        require(elementType == VALUE_PARAMETER_LIST && treeParent.elementType == FUNCTION_LITERAL)
        val lbrace = treeParent.findChildByType(LBRACE)!!
        return lbrace
            .leavesOnLine()
            .takeWhile { it.prevLeaf() != lbrace }
            .lineLengthWithoutNewlinePrefix()
    }

    private fun ASTNode.lengthOfParameterListWhenOnSingleLine(): Int {
        require(elementType == VALUE_PARAMETER_LIST)
        val stopAtLeaf = lastChildLeafOrSelf().nextLeaf()
        return firstChildLeafOrSelf()
            .leavesIncludingSelf()
            .takeWhile { it != stopAtLeaf }
            .joinToString(separator = "") {
                if (it.isWhiteSpace()) {
                    // Eliminate newlines and redundant spaces
                    " "
                } else {
                    it.text
                }
            }.length
    }

    private fun ASTNode.exceedsMaxLineLength() = lineLengthWithoutNewlinePrefix() > maxLineLength

    private fun ASTNode.wrapFirstParameterToNewline() =
        if (isFunctionLiteralLambdaWithNonEmptyValueParameterList()) {
            // Disallow when max line is exceeded:
            //    val foo = someCallExpression { someLongParameterName ->
            //        bar()
            //    }
            val stopAtLeaf =
                children()
                    .first { it.elementType == VALUE_PARAMETER }
                    .lastChildLeafOrSelf()
                    .nextLeaf { !it.isWhiteSpaceWithoutNewline() && !it.isPartOfComment() }
            leavesOnLine()
                .takeWhile { it.prevLeaf() != stopAtLeaf }
                .lineLengthWithoutNewlinePrefix()
                .let { it > maxLineLength }
        } else {
            false
        }

    private fun ASTNode.isFunctionLiteralLambdaWithNonEmptyValueParameterList() =
        takeIf { it.elementType == VALUE_PARAMETER_LIST }
            ?.takeIf { it.findChildByType(VALUE_PARAMETER) != null }
            ?.takeIf { it.treeParent.elementType == FUNCTION_LITERAL }
            ?.treeParent
            ?.takeIf { it.treeParent.elementType == LAMBDA_EXPRESSION }
            ?.treeParent
            ?.takeIf { it.treeParent.elementType == LAMBDA_ARGUMENT }
            .let { it != null }

    private fun rewriteToMultilineParameterList(
        parameterList: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(parameterList.elementType == VALUE_PARAMETER_LIST)
        parameterList
            .children()
            .filter { it.elementType == VALUE_PARAMETER }
            .forEach { wrapValueParameter(it, autoCorrect, emit) }
        parameterList
            .treeParent
            .findChildByType(ARROW)
            ?.let { arrow -> wrapArrow(arrow, autoCorrect, emit) }
        parameterList
            .treeParent
            .findChildByType(RBRACE)
            ?.let { rbrace -> wrapBeforeRbrace(rbrace, autoCorrect, emit) }
    }

    private fun wrapValueParameter(
        valueParameter: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(valueParameter.elementType == VALUE_PARAMETER)
        valueParameter
            .prevLeaf()
            .takeIf { it.isWhiteSpace() }
            .let { whitespaceBeforeValueParameter ->
                if (whitespaceBeforeValueParameter == null ||
                    !whitespaceBeforeValueParameter.textContains('\n')
                ) {
                    emit(valueParameter.startOffset, "Newline expected before parameter", true)
                    if (autoCorrect) {
                        valueParameter.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(valueParameter.parent(FUNCTION_LITERAL)!!))
                    }
                }
            }
    }

    private fun wrapArrow(
        arrow: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        wrapBeforeArrow(arrow, emit, autoCorrect)
        wrapAfterArrow(arrow, emit, autoCorrect)
    }

    private fun wrapBeforeArrow(
        arrow: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(arrow.elementType == ARROW)
        arrow
            .prevLeaf()
            .takeIf { it.isWhiteSpace() }
            .let { whitespaceBeforeArrow ->
                if (whitespaceBeforeArrow == null ||
                    !whitespaceBeforeArrow.textContains('\n')
                ) {
                    emit(arrow.startOffset, "Newline expected before arrow", true)
                    if (autoCorrect) {
                        arrow.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(arrow.treeParent))
                    }
                }
            }
    }

    private fun wrapAfterArrow(
        arrow: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(arrow.elementType == ARROW)
        arrow
            .nextLeaf()
            .takeIf { it.isWhiteSpace() }
            .let { whitespaceAfterArrow ->
                if (whitespaceAfterArrow == null ||
                    !whitespaceAfterArrow.textContains('\n')
                ) {
                    emit(arrow.startOffset + arrow.textLength - 1, "Newline expected after arrow", true)
                    if (autoCorrect) {
                        arrow.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(arrow))
                    }
                }
            }
    }

    private fun wrapBeforeRbrace(
        rbrace: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(rbrace.elementType == RBRACE)
        rbrace
            .prevLeaf()
            .takeIf { it.isWhiteSpace() }
            .let { whitespaceBeforeRbrace ->
                if (whitespaceBeforeRbrace == null ||
                    !whitespaceBeforeRbrace.textContains('\n')
                ) {
                    emit(rbrace.startOffset, "Newline expected before closing brace", true)
                    if (autoCorrect) {
                        rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(rbrace))
                    }
                }
            }
    }

    private fun rewriteToSingleLineFunctionLiteral(
        parameterList: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(parameterList.elementType == VALUE_PARAMETER_LIST)
        parameterList
            .prevSibling { it.isWhiteSpace() }
            ?.takeIf { it.isWhiteSpaceWithNewline() }
            ?.let { whitespaceBeforeParameterList ->
                emit(parameterList.startOffset, "No newline expected before parameter", true)
                if (autoCorrect) {
                    whitespaceBeforeParameterList.upsertWhitespaceBeforeMe(" ")
                }
            }
        parameterList
            .nextSibling { it.isWhiteSpace() }
            ?.takeIf { it.isWhiteSpaceWithNewline() }
            ?.let { whitespaceAfterParameterList ->
                emit(parameterList.startOffset + parameterList.textLength, "No newline expected after parameter", true)
                if (autoCorrect) {
                    whitespaceAfterParameterList.upsertWhitespaceAfterMe(" ")
                }
            }
    }

    private fun visitArrow(
        arrow: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(arrow.elementType == ARROW)
        arrow
            .prevSibling { it.elementType == VALUE_PARAMETER_LIST }
            ?.takeIf { it.findChildByType(VALUE_PARAMETER) == null }
            ?.let {
                emit(arrow.startOffset, "Arrow is redundant when parameter list is empty", true)
                if (autoCorrect) {
                    arrow
                        .nextSibling()
                        .takeIf { it.isWhiteSpace() }
                        ?.let { it.treeParent.removeChild(it) }
                    arrow.treeParent.removeChild(arrow)
                }
            }
    }

    private fun visitBlock(
        block: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        require(block.elementType == BLOCK)
        if (block.textContains('\n') || block.exceedsMaxLineLength()) {
            block
                .prevCodeSibling()
                ?.let { prevCodeSibling ->
                    when (prevCodeSibling.elementType) {
                        ARROW -> wrapAfterArrow(prevCodeSibling, emit, autoCorrect)
                        LBRACE -> wrapAfterLbrace(prevCodeSibling, emit, autoCorrect)
                        else -> LOGGER.debug { "Unexpected type of element ${prevCodeSibling.elementType}" }
                    }
                }

            block
                .nextCodeSibling()
                ?.takeIf { it.elementType == RBRACE }
                ?.let { rbrace -> wrapBeforeRbrace(rbrace, autoCorrect, emit) }
        }
    }

    private fun wrapAfterLbrace(
        lbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(lbrace.elementType == LBRACE)
        lbrace
            .nextLeaf()
            .takeIf { it.isWhiteSpace() }
            .let { whitespaceAfterLbrace ->
                if (whitespaceAfterLbrace == null ||
                    !whitespaceAfterLbrace.textContains('\n')
                ) {
                    emit(lbrace.startOffset, "Newline expected after opening brace", true)
                    if (autoCorrect) {
                        lbrace.upsertWhitespaceAfterMe(indentConfig.childIndentOf(lbrace))
                    }
                }
            }
    }
}

public val FUNCTION_LITERAL_RULE_ID: RuleId = FunctionLiteralRule().ruleId
