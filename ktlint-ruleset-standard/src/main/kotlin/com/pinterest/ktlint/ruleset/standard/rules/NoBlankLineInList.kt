package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_CONSTRAINT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

public class NoBlankLineInListRule :
    StandardRule("no-blank-line-in-list"),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != WHITE_SPACE) {
            return
        }

        node
            .treeParent
            .elementType
            .takeIf { it in LIST_TYPES }
            ?.let { treeParentElementType ->
                visitWhiteSpace(node, emit, autoCorrect, treeParentElementType)
            }

        // Note: depending on the implementation of the list type in the Kotlin language, the whitespace before the first and after the last
        // element in the list might be or not be part of the list.
        // In a VALUE_ARGUMENT_LIST the whitespaces before the first and after the last element are included in the VALUE_ARGUMENT_LIST. In
        // the SUPER_TYPE_LIST of a CLASS the whitespaces before the first super type is a child of the CLASS. The whitespace after the last
        // SUPER_TYPE is part of the class only when the class has a body.
        node
            .nextSibling()
            ?.elementType
            ?.takeIf { it in LIST_TYPES }
            ?.let { treeParentElementType ->
                visitWhiteSpace(
                    node = node,
                    emit = emit,
                    autoCorrect = autoCorrect,
                    partOfElementType = treeParentElementType,
                    replaceWithSingeSpace = treeParentElementType == TYPE_CONSTRAINT_LIST,
                )
            }

        // Note: depending on the implementation of the list type in the Kotlin language, the whitespace before the first and after the last
        // element in the list might be or not be part of the list.
        // In a VALUE_ARGUMENT_LIST the whitespaces before the first and after the last element are included in the VALUE_ARGUMENT_LIST. In
        // the SUPER_TYPE_LIST of a CLASS the whitespaces before the first super type is a child of the CLASS. The whitespace after the last
        // SUPER_TYPE is part of the class only when the class has a body.
        node
            .prevSibling()
            ?.elementType
            ?.takeIf { it in LIST_TYPES }
            ?.let { treeParentElementType ->
                visitWhiteSpace(
                    node = node,
                    emit = emit,
                    autoCorrect = autoCorrect,
                    partOfElementType = treeParentElementType,
                    replaceWithSingeSpace = node.nextSibling()?.elementType == CLASS_BODY,
                )
            }
    }

    private fun visitWhiteSpace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        partOfElementType: IElementType,
        replaceWithSingeSpace: Boolean = false,
    ) {
        node
            .text
            .split("\n")
            .let { lines ->
                if (lines.size > 2) {
                    emit(
                        node.startOffset + 1,
                        "Unexpected blank line(s) in ${partOfElementType.elementTypeDescription()}",
                        true,
                    )
                    if (autoCorrect) {
                        if (replaceWithSingeSpace) {
                            (node as LeafPsiElement).rawReplaceWithText(" ")
                        } else {
                            (node as LeafPsiElement).rawReplaceWithText("${lines.first()}\n${lines.last()}")
                        }
                    }
                }
            }
    }

    private fun IElementType.elementTypeDescription() =
        toString()
            .lowercase()
            .replace('_', ' ')

    private companion object {
        // The MODIFIER_LIST is handled by separate rule ModifierListSpacingRule
        val LIST_TYPES =
            listOf(
                SUPER_TYPE_LIST,
                TYPE_ARGUMENT_LIST,
                TYPE_CONSTRAINT_LIST,
                TYPE_PARAMETER_LIST,
                VALUE_ARGUMENT_LIST,
                VALUE_PARAMETER_LIST,
            )
    }
}

public val NO_BLANK_LINE_IN_LIST_RULE_ID: RuleId = NoBlankLineInListRule().ruleId
