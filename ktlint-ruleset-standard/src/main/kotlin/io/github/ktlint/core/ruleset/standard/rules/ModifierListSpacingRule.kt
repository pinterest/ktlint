package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.CONTEXT_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.MODIFIER_LIST
import io.github.ktlint.core.rule.engine.core.api.IndentConfig
import io.github.ktlint.core.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lint and format the spacing between the modifiers in and after the last modifier in a modifier list.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class ModifierListSpacingRule :
    StandardRule(
        id = "modifier-list-spacing",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private var indentConfig = DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == MODIFIER_LIST) {
            node
                .children
                .forEach { visitModifierChild(it, emit) }
            // The whitespace of the last entry of the modifier list is actually placed outside the modifier list
            visitModifierChild(node, emit)
        }
    }

    private fun visitModifierChild(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpace) {
            return
        }
        node
            .nextSibling { it.isWhiteSpace && it.nextLeaf?.isPartOfComment != true }
            ?.takeUnless {
                // A single newline after a comment is always ok and does not need further checking.
                it.text.trim(' ', '\t').contains('\n') && it.prevLeaf?.isPartOfComment == true
            }?.let { whitespace ->
                when {
                    node.isAnnotation() -> {
                        if (whitespace.text.contains("\n\n")) {
                            emit(whitespace.startOffset, "Single newline expected after annotation", true)
                                .ifAutocorrectAllowed {
                                    whitespace.replaceTextWith("\n".plus(whitespace.text.substringAfterLast("\n")))
                                }
                        } else if (!whitespace.text.contains('\n') && whitespace.text != " ") {
                            emit(whitespace.startOffset, "Single whitespace or newline expected after annotation", true)
                                .ifAutocorrectAllowed { whitespace.replaceTextWith(" ") }
                        }
                    }

                    node.isContextReceiverList() -> {
                        if (!whitespace.text.contains("\n")) {
                            emit(whitespace.startOffset, "Single newline expected after context receiver list", true)
                                .ifAutocorrectAllowed {
                                    whitespace.replaceTextWith(indentConfig.parentIndentOf(node))
                                }
                        }
                    }

                    whitespace.text != " " -> {
                        emit(whitespace.startOffset, "Single whitespace expected after modifier", true)
                            .ifAutocorrectAllowed { whitespace.replaceTextWith(" ") }
                    }
                }
            }
    }

    private fun ASTNode.isAnnotation(): Boolean =
        isAnnotationElement() || (elementType == MODIFIER_LIST && lastChildNode.isAnnotationElement())

    private fun ASTNode?.isAnnotationElement() = this != null && (elementType == ANNOTATION || elementType == ANNOTATION_ENTRY)

    private fun ASTNode.isContextReceiverList(): Boolean =
        elementType == CONTEXT_PARAMETER_LIST || (elementType == MODIFIER_LIST && lastChildNode.isContextReceiverList())
}

public val MODIFIER_LIST_SPACING_RULE_ID: RuleId = ModifierListSpacingRule().ruleId
