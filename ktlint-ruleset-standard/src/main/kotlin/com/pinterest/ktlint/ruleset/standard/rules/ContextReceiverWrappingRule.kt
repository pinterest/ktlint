package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTEXT_RECEIVER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTEXT_RECEIVER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Wrapping of context receiver to a separate line. Arguments of the context receiver are wrapped to separate line
 * whenever the max line length is exceeded.
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
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when {
            node.elementType == CONTEXT_RECEIVER_LIST ->
                visitContextReceiverList(node, emit)

            node.elementType == TYPE_ARGUMENT_LIST && node.isPartOf(CONTEXT_RECEIVER) ->
                visitContextReceiverTypeArgumentList(node, emit)
        }
    }

    private fun visitContextReceiverList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Context receiver must be followed by new line or comment
        node
            .lastChildLeafOrSelf()
            .nextLeaf { !it.isWhiteSpaceWithoutNewline() && !it.isPartOfComment() }
            ?.takeIf { !it.isWhiteSpaceWithNewline() }
            ?.let { nodeAfterContextReceiver ->
                emit(nodeAfterContextReceiver.startOffset, "Expected a newline after the context receiver", true)
                    .ifAutocorrectAllowed {
                        nodeAfterContextReceiver
                            .firstChildLeafOrSelf()
                            .upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(node))
                    }
            }

        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run before indenting.
        if (!node.textContains('\n') &&
            node.indent(false).length + node.textLength > maxLineLength
        ) {
            node
                .children()
                .filter { it.elementType == CONTEXT_RECEIVER }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context receiver as max line length is violated",
                        true,
                    ).ifAutocorrectAllowed {
                        it
                            .prevLeaf(includeEmpty = true)
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
                        rpar.upsertWhitespaceBeforeMe(node.indent())
                    }
                }
        }
    }

    private fun visitContextReceiverTypeArgumentList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val contextReceiver = node.treeParent.text
        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run
        // before indenting.
        if (!contextReceiver.contains('\n') &&
            node.indent(false).length + contextReceiver.length > maxLineLength
        ) {
            node
                .children()
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
