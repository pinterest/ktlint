package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.CONTEXT_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.CONTEXT_RECEIVER
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUN
import io.github.ktlint.core.rule.engine.core.api.ElementType.FUNCTION_TYPE
import io.github.ktlint.core.rule.engine.core.api.ElementType.GT
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_PROJECTION
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_REFERENCE
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.firstChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.hasNoMaxLineLengthSuppression
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.indent
import io.github.ktlint.core.rule.engine.core.api.indentWithoutNewlinePrefix
import io.github.ktlint.core.rule.engine.core.api.isPartOf
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithoutNewline
import io.github.ktlint.core.rule.engine.core.api.lastChildLeafOrSelf
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceBeforeMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Wrapping of context receiver list to a separate line.
 *
 * IMPORTANT: This rule only affects a context receiver list that does not contain a context receiver. Context receivers are deprecated
 * since Kotlin 2.2.0, and are wrapped by the 'context-receiver-wrapping' rule.
 *
 * Note: In Ktlint 2.x this rule has been renamed from ContextReceiverListWrappingRule to ContextParameterListWrappingRule
 */
@SinceKtlint("1.7", STABLE)
public class ContextParameterListWrappingRule :
    StandardRule(
        id = "context-parameter-list-wrapping",
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
            node.elementType == CONTEXT_PARAMETER_LIST && node.isContextParameter() -> {
                visitContextReceiverList(node, emit)
            }

            node.elementType == TYPE_ARGUMENT_LIST && node.isContextParameter() -> {
                visitContextReceiverTypeArgumentList(node, emit)
            }
        }
    }

    private fun ASTNode.isContextParameter(): Boolean = isPartOf(CONTEXT_PARAMETER_LIST) && findChildByType(CONTEXT_RECEIVER) == null

    private fun visitContextReceiverList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Context receiver must be followed by new line or comment unless it is a type reference of a parameter
        node
            .takeUnless { it.isTypeReferenceParameterInFunction() }
            ?.lastChildLeafOrSelf
            ?.nextLeaf { !it.isWhiteSpaceWithoutNewline && !it.isPartOfComment }
            ?.takeIf { !it.isWhiteSpaceWithNewline }
            ?.let { nodeAfterContextReceiver ->
                emit(nodeAfterContextReceiver.startOffset, "Expected a newline after the context parameter", true)
                    .ifAutocorrectAllowed {
                        nodeAfterContextReceiver
                            .firstChildLeafOrSelf
                            .upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(node))
                    }
            }

        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run before indenting.
        if (!node.textContains('\n') &&
            node.hasNoMaxLineLengthSuppression() &&
            node.indentWithoutNewlinePrefix.length + node.textLength > maxLineLength
        ) {
            node
                .children
                .filter { it.elementType == VALUE_PARAMETER }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context parameter as max line length is violated",
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
                        rpar.upsertWhitespaceBeforeMe(node.indent)
                    }
                }
        }
    }

    private fun ASTNode.isTypeReferenceParameterInFunction() =
        takeIf { it.elementType == CONTEXT_PARAMETER_LIST }
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
            node.hasNoMaxLineLengthSuppression() &&
            node.indentWithoutNewlinePrefix.length + contextReceiverText.length > maxLineLength
        ) {
            node
                .children
                .filter { it.elementType == TYPE_PROJECTION }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context parameter type projection as max line length is violated",
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

public val CONTEXT_PARAMETER_LIST_WRAPPING_RULE_ID: RuleId = ContextParameterListWrappingRule().ruleId
