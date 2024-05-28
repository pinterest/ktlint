package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * The Kotlin Coding Conventions suggest to consider using a blank line after a multiline when-condition
 * (https://kotlinlang.org/docs/coding-conventions.html#control-flow-statements) which behavior is managed via '.editorconfig' property
 * `ij_kotlin_line_break_after_multiline_when_entry`.
 *
 * Ktlint uses the property `ij_kotlin_line_break_after_multiline_when_entry` to consistently add/remove blank line between all
 * when-conditions in the when-statement depending on whether the statement contains at least one multiline when-condition.
 */
@SinceKtlint("1.2.0", EXPERIMENTAL)
public class BlankLineBetweenWhenConditions :
    StandardRule(
        id = "blank-line-between-when-conditions",
        usesEditorConfigProperties = setOf(LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY),
    ),
    RuleAutocorrectApproveHandler,
    Rule.Experimental {
    private var lineBreakAfterWhenCondition = LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        lineBreakAfterWhenCondition = editorConfig[LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == ElementType.WHEN) {
            visitWhenStatement(node, emit)
        }
    }

    private fun visitWhenStatement(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val hasMultilineWhenCondition = node.hasAnyMultilineWhenCondition()
        if (hasMultilineWhenCondition && lineBreakAfterWhenCondition) {
            addBlankLinesBetweenWhenConditions(node, emitAndApprove)
        } else {
            removeBlankLinesBetweenWhenConditions(node, emitAndApprove)
        }
    }

    private fun addBlankLinesBetweenWhenConditions(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .children()
            .filter { it.elementType == WHEN_ENTRY }
            // Blank lines should only be added *between* when-conditions, so first when-condition is to be skipped
            .drop(1)
            .forEach { whenEntry ->
                whenEntry
                    .findWhitespaceAfterPreviousCodeSibling()
                    ?.takeUnless { it.containsBlankLine() }
                    ?.let { whitespaceBeforeWhenEntry ->
                        emitAndApprove(
                            whitespaceBeforeWhenEntry.startOffset + 1,
                            "Add a blank line between all when-condition in case at least one multiline when-condition is found in the " +
                                "statement",
                            true,
                        ).ifAutocorrectAllowed {
                            whitespaceBeforeWhenEntry.upsertWhitespaceBeforeMe("\n${whenEntry.indent()}")
                        }
                    }
            }
    }

    private fun ASTNode.containsBlankLine(): Boolean = elementType == WHITE_SPACE && text.count { it == '\n' } > 1

    private fun ASTNode.hasAnyMultilineWhenCondition() =
        children()
            .any { it.elementType == WHEN_ENTRY && (it.textContains('\n') || it.isPrecededByComment()) }

    private fun ASTNode.isPrecededByComment() = prevSibling { !it.isWhiteSpace() }?.isPartOfComment() ?: false

    private fun ASTNode.findWhitespaceAfterPreviousCodeSibling() =
        prevCodeSibling()
            ?.lastChildLeafOrSelf()
            ?.nextLeaf { it.isWhiteSpace() }

    private fun removeBlankLinesBetweenWhenConditions(
        node: ASTNode,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .children()
            .filter { it.elementType == WHEN_ENTRY }
            // Blank lines should only be removed *between* when-conditions, so first when-condition is to be skipped
            .drop(1)
            .forEach { whenEntry ->
                whenEntry
                    .findWhitespaceAfterPreviousCodeSibling()
                    ?.takeIf { it.containsBlankLine() }
                    ?.let { whitespaceBeforeWhenEntry ->
                        emitAndApprove(
                            whitespaceBeforeWhenEntry.startOffset + 1,
                            "Unexpected blank lines between when-condition if all when-conditions are single lines",
                            true,
                        ).ifAutocorrectAllowed {
                            whitespaceBeforeWhenEntry.upsertWhitespaceBeforeMe("\n${whenEntry.indent(includeNewline = false)}")
                        }
                    }
            }
    }

    public companion object {
        private val BOOLEAN_VALUES_SET = setOf("true", "false")

        public val LINE_BREAK_AFTER_WHEN_CONDITION_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ij_kotlin_line_break_after_multiline_when_entry",
                        "Defines whether a blank line is to be added after a when entry. Contrary to default IDEA formatting, " +
                            "ktlint adds the blank line between all when-conditions if the when-statement contains at least one " +
                            "multiline when-condition. Or, it removes all blank lines between the when-conditions if the when-statement " +
                            "does not contain any multiline when-condition.",
                        PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                        BOOLEAN_VALUES_SET,
                    ),
                defaultValue = true,
            )
    }
}

public val BLANK_LINE_BETWEEN_WHEN_CONDITIONS_RULE_ID: RuleId = BlankLineBetweenWhenConditions().ruleId
