package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.CONTEXT_RECEIVER
import com.pinterest.ktlint.core.ast.ElementType.CONTEXT_RECEIVER_LIST
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.firstChildLeafOrSelf
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Wrapping of context receiver to a separate line. Arguments of the context receiver are wrapped to separate line
 * whenever the max line length is exceeded.
 */
public class ContextReceiverWrappingRule :
    Rule("$EXPERIMENTAL_RULE_SET_ID:context-receiver-wrapping"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> =
        listOf(
            INDENT_SIZE_PROPERTY,
            INDENT_STYLE_PROPERTY,
            MAX_LINE_LENGTH_PROPERTY,
        )

    private lateinit var indent: String
    private var maxLineLength = -1

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        with(editorConfigProperties) {
            val indentConfig = IndentConfig(
                indentStyle = getEditorConfigValue(INDENT_STYLE_PROPERTY),
                tabWidth = getEditorConfigValue(INDENT_SIZE_PROPERTY),
            )
            indent = indentConfig.indent
            maxLineLength = getEditorConfigValue(MAX_LINE_LENGTH_PROPERTY)
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when {
            node.elementType == CONTEXT_RECEIVER_LIST ->
                visitContextReceiverList(node, autoCorrect, emit)
            node.elementType == TYPE_ARGUMENT_LIST && node.isPartOf(CONTEXT_RECEIVER) ->
                visitContextReceiverTypeArgumentList(node, autoCorrect, emit)
        }
    }

    private fun visitContextReceiverList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        // Context receiver must be followed by new line or comment
        node
            .lastChildLeafOrSelf()
            .nextLeaf { !it.isWhiteSpaceWithoutNewline() && !it.isPartOfComment() }
            ?.takeIf { !it.isWhiteSpaceWithNewline() }
            ?.let { nodeAfterContextReceiver ->
                emit(nodeAfterContextReceiver.startOffset, "Expected a newline after the context receiver", true)
                if (autoCorrect) {
                    nodeAfterContextReceiver
                        .firstChildLeafOrSelf()
                        .upsertWhitespaceBeforeMe("\n" + node.treeParent.lineIndent())
                }
            }

        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run
        // before indenting.
        if (isMaxLineLengthSet() &&
            !node.textContains('\n') &&
            node.lineIndent().length + node.textLength > maxLineLength
        ) {
            node
                .children()
                .filter { it.elementType == CONTEXT_RECEIVER }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context receiver as max line length is violated",
                        true,
                    )
                    if (autoCorrect) {
                        it
                            .prevLeaf(includeEmpty = true)
                            ?.upsertWhitespaceAfterMe("\n" + node.lineIndent() + indent)
                    }
                }
            node
                .findChildByType(RPAR)
                ?.let { rpar ->
                    emit(
                        rpar.startOffset,
                        "Newline expected before closing parenthesis as max line length is violated",
                        true,
                    )
                    if (autoCorrect) {
                        rpar.upsertWhitespaceBeforeMe("\n" + node.lineIndent())
                    }
                }
        }
    }

    private fun visitContextReceiverTypeArgumentList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val contextReceiver = node.treeParent.text
        // Check line length assuming that the context receiver is indented correctly. Wrapping rule must however run
        // before indenting.
        if (isMaxLineLengthSet() &&
            !contextReceiver.contains('\n') &&
            node.lineIndent().length + contextReceiver.length > maxLineLength
        ) {
            node
                .children()
                .filter { it.elementType == TYPE_PROJECTION }
                .forEach {
                    emit(
                        it.startOffset,
                        "Newline expected before context receiver type projection as max line length is violated",
                        true,
                    )
                    if (autoCorrect) {
                        it
                            .upsertWhitespaceBeforeMe("\n" + node.lineIndent() + indent)
                    }
                }
            node
                .findChildByType(GT)
                ?.let { gt ->
                    emit(
                        gt.startOffset,
                        "Newline expected before closing angle bracket as max line length is violated",
                        true,
                    )
                    if (autoCorrect) {
                        // Ideally, the closing angle bracket should be de-indented to make it consistent with
                        // de-intentation of closing ")", "}" and "]". This however would be inconsistent with how the
                        // type argument lists are formatted by IntelliJ IDEA default formatter.
                        gt.upsertWhitespaceBeforeMe("\n" + node.lineIndent() + indent)
                    }
                }
        }
    }

    private fun isMaxLineLengthSet() = maxLineLength > -1
}
