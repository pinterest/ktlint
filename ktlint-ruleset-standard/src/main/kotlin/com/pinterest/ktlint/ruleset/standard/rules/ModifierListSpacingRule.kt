package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTEXT_RECEIVER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Lint and format the spacing between the modifiers in and after the last modifier in a modifier list.
 */
@SinceKtlint("0.45", EXPERIMENTAL)
@SinceKtlint("0.49", STABLE)
public class ModifierListSpacingRule :
    StandardRule(
        id = "modifier-list-spacing",
        visitorModifiers =
            setOf(
                VisitorModifier.RunAfterRule(ANNOTATION_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                VisitorModifier.RunAfterRule(MODIFIER_ORDER_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
            ),
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
                .children20
                .forEach { visitModifierChild(it, emit) }
            // The whitespace of the last entry of the modifier list is actually placed outside the modifier list
            visitModifierChild(node, emit)
        }
    }

    private fun visitModifierChild(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isWhiteSpace20) {
            return
        }
        node
            .nextSibling { it.isWhiteSpace20 && it.nextLeaf?.isPartOfComment20 != true }
            ?.takeUnless {
                // A single newline after a comment is always ok and does not need further checking.
                it.text.trim(' ', '\t').contains('\n') && it.prevLeaf?.isPartOfComment20 == true
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
                        Unit
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
        elementType == CONTEXT_RECEIVER_LIST || (elementType == MODIFIER_LIST && lastChildNode.isContextReceiverList())
}

public val MODIFIER_LIST_SPACING_RULE_ID: RuleId = ModifierListSpacingRule().ruleId
