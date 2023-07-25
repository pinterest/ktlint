package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Ensures consistent spacing inside the parameter list. This rule partly overlaps with other rules like spacing around
 * commas and colons. However, it does have a more complete view on the higher concept of the parameter-list without
 * interfering of the parameter-list-wrapping rule.
 */
public class ParameterListSpacingRule :
    StandardRule("parameter-list-spacing"),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == VALUE_PARAMETER_LIST) {
            visitValueParameterList(node, emit, autoCorrect)
        }
    }

    private fun visitValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == VALUE_PARAMETER_LIST)
        val countValueParameters =
            node
                .children()
                .count { it.elementType == VALUE_PARAMETER }
        var valueParameterCount = 0
        val iterator =
            node
                .children()
                // Store elements in list before changing them as otherwise only one element is being changed
                .toList()
                .iterator()
        while (iterator.hasNext()) {
            val el = iterator.next()
            when (el.elementType) {
                WHITE_SPACE -> {
                    if (countValueParameters == 0 && node.containsNoComments()) {
                        removeUnexpectedWhiteSpace(el, emit, autoCorrect)
                    } else if (valueParameterCount == 0 && el.isNotIndent()) {
                        if (node.containsNoComments()) {
                            // whitespace before first parameter
                            removeUnexpectedWhiteSpace(el, emit, autoCorrect)
                        } else {
                            // Avoid conflict with comment spacing rule which requires a whitespace before the
                            // EOL-comment
                        }
                    } else if (valueParameterCount == countValueParameters && el.isNotIndent()) {
                        if (node.containsNoComments()) {
                            // whitespace after the last parameter
                            removeUnexpectedWhiteSpace(el, emit, autoCorrect)
                        } else {
                            // Avoid conflict with comment spacing rule which requires a whitespace before the
                            // EOL-comment
                        }
                    } else if (el.nextCodeSibling()?.elementType == COMMA) {
                        // No whitespace between parameter name and comma allowed
                        removeUnexpectedWhiteSpace(el, emit, autoCorrect)
                    } else if (el.elementType == WHITE_SPACE && el.isNotIndent() && el.isNotSingleSpace()) {
                        require(el.prevCodeSibling()?.elementType == COMMA)
                        replaceWithSingleSpace(el, emit, autoCorrect)
                    }
                }
                COMMA -> {
                    // Comma must be followed by whitespace
                    el
                        .nextLeaf()
                        ?.takeIf { it.elementType != WHITE_SPACE }
                        ?.let { addMissingWhiteSpaceAfterMe(el, emit, autoCorrect) }
                }
                VALUE_PARAMETER -> {
                    valueParameterCount += 1
                    visitValueParameter(el, emit, autoCorrect)
                }
            }
        }
    }

    private fun ASTNode.containsNoComments() = children().none { it.isPartOfComment() }

    private fun visitValueParameter(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        visitModifierList(node, emit, autoCorrect)
        removeWhiteSpaceBetweenParameterIdentifierAndColon(node, emit, autoCorrect)
        fixWhiteSpaceAfterColonInParameter(node, emit, autoCorrect)
    }

    private fun visitModifierList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val modifierList = node.findChildByType(MODIFIER_LIST) ?: return
        removeWhiteSpaceBetweenModifiersInList(modifierList, emit, autoCorrect)
        removeWhiteSpaceBetweenModifierListAndParameterIdentifier(modifierList, emit, autoCorrect)
    }

    private fun removeWhiteSpaceBetweenModifiersInList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == MODIFIER_LIST)
        node
            .children()
            .filter { it.elementType == WHITE_SPACE }
            // Store elements in list before changing them as otherwise only the first whitespace is being changed
            .toList()
            .forEach { visitWhiteSpaceAfterModifier(it, emit, autoCorrect) }
    }

    private fun visitWhiteSpaceAfterModifier(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .takeUnless {
                // Ignore when the modifier is an annotation which is placed on a separate line
                it.isIndent() && it.getPrecedingModifier()?.elementType == ANNOTATION_ENTRY
            }
            ?.takeIf { it.isNotSingleSpace() }
            ?.let { replaceWithSingleSpace(it, emit, autoCorrect) }
    }

    private fun removeWhiteSpaceBetweenModifierListAndParameterIdentifier(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == MODIFIER_LIST)
        node
            .nextSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { visitWhiteSpaceAfterModifier(it, emit, autoCorrect) }
    }

    private fun removeWhiteSpaceBetweenParameterIdentifierAndColon(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .findChildByType(COLON)
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whiteSpaceBeforeColon ->
                removeUnexpectedWhiteSpace(whiteSpaceBeforeColon, emit, autoCorrect)
            }
    }

    private fun fixWhiteSpaceAfterColonInParameter(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        val colonNode = node.findChildByType(COLON) ?: return
        colonNode
            .nextLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceAfterColon ->
                if (whiteSpaceAfterColon == null) {
                    addMissingWhiteSpaceAfterMe(colonNode, emit, autoCorrect)
                } else {
                    if (node.isTypeReferenceWithModifierList() && whiteSpaceAfterColon.isIndent()) {
                        // Allow the type to be wrapped to the next line when it has a modifier:
                        //   data class Foo(
                        //       val bar:
                        //           @FooBar("foobar")
                        //           Bar,
                        //   )
                        Unit
                    } else if (whiteSpaceAfterColon.isNotSingleSpace()) {
                        replaceWithSingleSpace(whiteSpaceAfterColon, emit, autoCorrect)
                    }
                }
            }
    }

    private fun addMissingWhiteSpaceAfterMe(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == COLON || node.elementType == COMMA)
        emit(node.startOffset, "Whitespace after '${node.text}' is missing", true)
        if (autoCorrect) {
            node.upsertWhitespaceAfterMe(" ")
        }
    }

    private fun ASTNode.isNotIndent(): Boolean = !isIndent()

    private fun ASTNode.isIndent(): Boolean {
        require(elementType == WHITE_SPACE)
        return text.startsWith("\n")
    }

    private fun ASTNode.isNotSingleSpace(): Boolean {
        require(elementType == WHITE_SPACE)
        return text != " "
    }

    private fun removeUnexpectedWhiteSpace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        emit(node.startOffset, "Unexpected whitespace", true)
        if (autoCorrect) {
            (node as LeafElement).rawRemove()
        }
    }

    private fun replaceWithSingleSpace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        emit(node.startOffset, "Expected a single space", true)
        if (autoCorrect) {
            (node as LeafPsiElement).rawReplaceWithText(" ")
        }
    }

    private fun ASTNode.getPrecedingModifier(): ASTNode? =
        prevCodeSibling()
            ?.let { prevCodeSibling ->
                if (prevCodeSibling.elementType == MODIFIER_LIST) {
                    prevCodeSibling.lastChildNode
                } else {
                    require(prevCodeSibling.treeParent.elementType == MODIFIER_LIST)
                    prevCodeSibling
                }
            }

    private fun ASTNode?.isTypeReferenceWithModifierList() =
        null !=
            this
                ?.findChildByType(TYPE_REFERENCE)
                ?.findChildByType(MODIFIER_LIST)
}

public val PARAMETER_LIST_SPACING_RULE_ID: RuleId = ParameterListSpacingRule().ruleId
