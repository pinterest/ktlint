package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lineLength
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
@SinceKtlint("0.46", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class ParameterListSpacingRule :
    StandardRule(
        id = "parameter-list-spacing",
        usesEditorConfigProperties =
            setOf(MAX_LINE_LENGTH_PROPERTY),
    ) {
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        maxLineLength = editorConfig.maxLineLength()
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == VALUE_PARAMETER_LIST) {
            visitValueParameterList(node, emit)
        }
    }

    private fun visitValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
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
                        removeUnexpectedWhiteSpace(el, emit)
                    } else if (valueParameterCount == 0 && el.isNotIndent()) {
                        if (node.containsNoComments()) {
                            // whitespace before first parameter
                            removeUnexpectedWhiteSpace(el, emit)
                        } else {
                            // Avoid conflict with comment spacing rule which requires a whitespace before the
                            // EOL-comment
                        }
                    } else if (valueParameterCount == countValueParameters && el.isNotIndent()) {
                        if (node.containsNoComments()) {
                            // whitespace after the last parameter
                            removeUnexpectedWhiteSpace(el, emit)
                        } else {
                            // Avoid conflict with comment spacing rule which requires a whitespace before the
                            // EOL-comment
                        }
                    } else if (el.nextCodeSibling()?.elementType == COMMA) {
                        // No whitespace between parameter name and comma allowed
                        removeUnexpectedWhiteSpace(el, emit)
                    } else if (el.elementType == WHITE_SPACE && el.isNotIndent() && el.isNotSingleSpace()) {
                        require(el.prevCodeSibling()?.elementType == COMMA)
                        replaceWithSingleSpace(el, emit)
                    }
                }

                COMMA -> {
                    // Comma must be followed by whitespace
                    el
                        .nextLeaf()
                        ?.takeIf { it.elementType != WHITE_SPACE }
                        ?.let { addMissingWhiteSpaceAfterMe(el, emit) }
                }

                VALUE_PARAMETER -> {
                    valueParameterCount += 1
                    visitValueParameter(el, emit)
                }
            }
        }
    }

    private fun ASTNode.containsNoComments() = children().none { it.isPartOfComment() }

    private fun visitValueParameter(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        visitModifierList(node, emit)
        removeWhiteSpaceBetweenParameterIdentifierAndColon(node, emit)
        fixWhiteSpaceAfterColonInParameter(node, emit)
    }

    private fun visitModifierList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val modifierList = node.findChildByType(MODIFIER_LIST) ?: return
        removeWhiteSpaceBetweenModifiersInList(modifierList, emit)
        removeWhiteSpaceBetweenModifierListAndParameterIdentifier(modifierList, emit)
    }

    private fun removeWhiteSpaceBetweenModifiersInList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == MODIFIER_LIST)
        node
            .children()
            .filter { it.elementType == WHITE_SPACE }
            // Store elements in list before changing them as otherwise only the first whitespace is being changed
            .toList()
            .forEach { visitWhiteSpaceAfterModifier(it, emit) }
    }

    private fun visitWhiteSpaceAfterModifier(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeUnless {
                // Ignore when the modifier is an annotation which is placed on a separate line
                it.isIndent() && it.getPrecedingModifier()?.elementType == ANNOTATION_ENTRY
            }?.takeIf { it.isNotSingleSpace() }
            ?.let { replaceWithSingleSpace(it, emit) }
    }

    private fun removeWhiteSpaceBetweenModifierListAndParameterIdentifier(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == MODIFIER_LIST)
        node
            .nextSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { visitWhiteSpaceAfterModifier(it, emit) }
    }

    private fun removeWhiteSpaceBetweenParameterIdentifierAndColon(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByType(COLON)
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            ?.let { whiteSpaceBeforeColon ->
                removeUnexpectedWhiteSpace(whiteSpaceBeforeColon, emit)
            }
    }

    private fun fixWhiteSpaceAfterColonInParameter(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val colonNode = node.findChildByType(COLON) ?: return
        colonNode
            .nextLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceAfterColon ->
                if (whiteSpaceAfterColon == null) {
                    addMissingWhiteSpaceAfterMe(colonNode, emit)
                } else {
                    if (node.isTypeReferenceWithModifierList() && whiteSpaceAfterColon.isIndent()) {
                        // Allow the type to be wrapped to the next line when it has a modifier:
                        //   data class Foo(
                        //       val bar:
                        //           @FooBar("foobar")
                        //           Bar,
                        //   )
                        Unit
                    } else if (whiteSpaceAfterColon.hasTypeReferenceWhichDoesNotFitOnSameLineAsColon()) {
                        // Allow the type to be wrapped to the next line when the type does not fit on same line as colon:
                        //   class Foo(
                        //       val someReallyLongFieldNameUsedInMyClass:
                        //           SomeReallyLongDependencyClass
                        //   )
                        Unit
                    } else if (whiteSpaceAfterColon.isNotSingleSpace()) {
                        replaceWithSingleSpace(whiteSpaceAfterColon, emit)
                    }
                }
            }
    }

    private fun addMissingWhiteSpaceAfterMe(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == COLON || node.elementType == COMMA)
        emit(node.startOffset, "Whitespace after '${node.text}' is missing", true)
            .ifAutocorrectAllowed {
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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(node.startOffset, "Unexpected whitespace", true)
            .ifAutocorrectAllowed {
                (node as LeafElement).rawRemove()
            }
    }

    private fun replaceWithSingleSpace(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(node.startOffset, "Expected a single space", true)
            .ifAutocorrectAllowed {
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

    private fun ASTNode.hasTypeReferenceWhichDoesNotFitOnSameLineAsColon() =
        takeIf { it.isWhiteSpaceWithNewline() }
            ?.nextCodeSibling()
            ?.takeIf { it.elementType == TYPE_REFERENCE }
            ?.let { typeReference ->
                val length =
                    // length of previous line
                    lineLength(excludeEolComment = true) +
                        // single space before type reference
                        1 -
                        // length of current indent before typeReference
                        this.text.substringAfterLast("\n").length +
                        // length of line containing typeReference
                        typeReference.lineLength(excludeEolComment = true)
                length > maxLineLength
            }
            ?: false
}

public val PARAMETER_LIST_SPACING_RULE_ID: RuleId = ParameterListSpacingRule().ruleId
