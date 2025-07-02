package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTEXT_RECEIVER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTEXT_RECEIVER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.indentWithoutNewlinePrefix
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Wrapping of context receiver to a separate line. Arguments of the context receiver are wrapped to separate line
 * whenever the max line length is exceeded.
 *
 * In Kotlin 2.1.21 the context parameters have been introduced as a replacement for context receiver. Like the context receiver, the
 * context parameters are wrapped inside a context receiver list. This rule will be removed once context receivers are no longer supported
 * by the Kotlin compiler. A new rule (ContextReceiverListWrapping) will take care of wrapping the context parameters.
 */
@SinceKtlint("0.48", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class ContextReceiverWrappingRule :
    StandardRule(
        id = "context-receiver-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
            ),
    ) {
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig.maxLineLength()
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when {
            node.elementType == CONTEXT_RECEIVER_LIST && node.findChildByType(CONTEXT_RECEIVER) != null -> {
                visitContextReceiverList(node, emit)
            }

            node.elementType == TYPE_ARGUMENT_LIST && node.isPartOf(CONTEXT_RECEIVER) -> {
                visitContextReceiverTypeArgumentList(node, emit)
            }
        }
    }

    private fun visitContextReceiverList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Context receiver must be followed by new line or comment unless it is a type reference of a parameter
        node
            .takeUnless { it.isTypeReferenceParameterInFunction() }
            ?.lastChildLeafOrSelf20
            ?.nextLeaf { !it.isWhiteSpaceWithoutNewline20 && !it.isPartOfComment20 }
            ?.takeIf { !it.isWhiteSpaceWithNewline20 }
            ?.let { nodeAfterContextReceiver ->
                emit(nodeAfterContextReceiver.startOffset, "Expected a newline after the context receiver", true)
                    .ifAutocorrectAllowed {
                        nodeAfterContextReceiver
                            .firstChildLeafOrSelf20
                            .upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(node))
                    }
            }

        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run before indenting.
        if (!node.textContains('\n') &&
            node.indentWithoutNewlinePrefix.length + node.textLength > maxLineLength
        ) {
            node
                .children20
                .filter { it.elementType == CONTEXT_RECEIVER }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context receiver as max line length is violated",
                        true,
                    ).ifAutocorrectAllowed {
                        it
                            .prevLeaf
                            ?.upsertWhitespaceAfterMe(indentConfig.childIndentOf(node))
                    }
                }
            node
                .findChildByType(RPAR)
                ?.let { rpar ->
                    emit(
                        rpar.startOffset,
                        "Newline expected before closing parenthesis as max line length is violated",
                        true,
                    ).ifAutocorrectAllowed {
                        rpar.upsertWhitespaceBeforeMe(node.indent20)
                    }
                }
        }
    }

    private fun ASTNode.isTypeReferenceParameterInFunction() =
        takeIf { it.elementType == CONTEXT_RECEIVER_LIST }
            ?.parent
            ?.takeIf { it.elementType == FUNCTION_TYPE }
            ?.parent
            ?.takeIf { it.elementType == TYPE_REFERENCE }
            ?.parent
            ?.takeIf { it.elementType == VALUE_PARAMETER }
            ?.parent
            ?.takeIf { it.elementType == VALUE_PARAMETER_LIST }
            ?.parent
            ?.let { it.elementType == FUN }
            ?: false

    private fun visitContextReceiverTypeArgumentList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val contextReceiverText = node.parent?.text.orEmpty()
        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run
        // before indenting.
        if (!contextReceiverText.contains('\n') &&
            node.indentWithoutNewlinePrefix.length + contextReceiverText.length > maxLineLength
        ) {
            node
                .children20
                .filter { it.elementType == TYPE_PROJECTION }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context receiver type projection as max line length is violated",
                        true,
                    ).ifAutocorrectAllowed {
                        it.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                    }
                }
            node
                .findChildByType(GT)
                ?.let { gt ->
                    emit(
                        gt.startOffset,
                        "Newline expected before closing angle bracket as max line length is violated",
                        true,
                    ).ifAutocorrectAllowed {
                        // Ideally, the closing angle bracket should be de-indented to make it consistent with
                        // de-indentation of closing ")", "}" and "]". This however would be inconsistent with how the
                        // type argument lists are formatted by IntelliJ IDEA default formatter.
                        gt.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                    }
                }
        }
    }
}

public val CONTEXT_RECEIVER_WRAPPING_RULE_ID: RuleId = ContextReceiverWrappingRule().ruleId
