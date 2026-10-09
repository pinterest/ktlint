package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import io.github.ktlint.core.rule.engine.core.api.ElementType.COLON
import io.github.ktlint.core.rule.engine.core.api.ElementType.COMMA
import io.github.ktlint.core.rule.engine.core.api.ElementType.MODIFIER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.RPAR
import io.github.ktlint.core.rule.engine.core.api.ElementType.TYPE_REFERENCE
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER
import io.github.ktlint.core.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.WHITE_SPACE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.children
import io.github.ktlint.core.rule.engine.core.api.dropTrailingEolComment
import io.github.ktlint.core.rule.engine.core.api.editorconfig.EditorConfig
import io.github.ktlint.core.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isPartOfComment
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.leavesOnLine
import io.github.ktlint.core.rule.engine.core.api.lineLength
import io.github.ktlint.core.rule.engine.core.api.nextCodeSibling
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.nextSibling
import io.github.ktlint.core.rule.engine.core.api.parent
import io.github.ktlint.core.rule.engine.core.api.prevCodeSibling
import io.github.ktlint.core.rule.engine.core.api.prevLeaf
import io.github.ktlint.core.rule.engine.core.api.remove
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.rule.engine.core.api.upsertWhitespaceAfterMe
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

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
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == VALUE_PARAMETER_LIST) {
            visitValueParameterList(node, emit)
        }
    }

    private fun visitValueParameterList(
        node: ASTNode,
        emit: (Int, String, Boolean) -> AutocorrectDecision,
    ) {
        require(node.elementType == VALUE_PARAMETER_LIST)
        val iterator =
            node
                .children
                // Store elements in list before changing them as otherwise only one element is being changed
                .toList()
                .iterator()
        while (iterator.hasNext()) {
            val el = iterator.next()
            when (el.elementType) {
                WHITE_SPACE -> el.visitWhiteSpaceInValueParameterList(emit)
                COMMA -> el.visitCommaInValueParameterList(emit)
                VALUE_PARAMETER -> el.visitValueParameter(emit)
            }
        }
    }

    private fun ASTNode.visitWhiteSpaceInValueParameterList(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        if (parent!!.findChildByType(VALUE_PARAMETER) == null && parent!!.containsNoComments()) {
            // Remove whitespace from parameter list not containing a value parameter or comment
            //     fun foo( )
            this.removeUnexpectedWhiteSpace(emit)
        } else if ((isBeforeFirstOrAfterLastValueParameter()) && isNotIndent()) {
            if (parent!!.containsNoComments()) {
                this.removeUnexpectedWhiteSpace(emit)
            } else {
                // Avoid conflict with comment spacing rule
            }
        } else if (nextCodeSibling?.elementType == COMMA) {
            // No whitespace between parameter name and comma allowed
            this.removeUnexpectedWhiteSpace(emit)
        } else if (isWhiteSpace && isNotIndent() && isNotSingleSpace()) {
            require(prevCodeSibling?.elementType == COMMA)
            this.replaceWithSingleSpace(emit)
        }
    }

    private fun ASTNode.containsNoComments() = children.none { it.isPartOfComment }

    private fun ASTNode.isBeforeFirstOrAfterLastValueParameter(): Boolean =
        this.isBeforeFirstValueParameter() || this.isAfterLastValueParameter()

    private fun ASTNode.isBeforeFirstValueParameter(): Boolean = siblings(forward = false).none { it.elementType == VALUE_PARAMETER }

    private fun ASTNode.isAfterLastValueParameter(): Boolean = siblings().none { it.elementType == VALUE_PARAMETER }

    private fun ASTNode.visitCommaInValueParameterList(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        // Comma, except when it is the trailing comma, must be followed by whitespace
        nextLeaf
            ?.takeUnless { it.isWhiteSpace || it.elementType == RPAR }
            ?.let { addMissingWhiteSpaceAfterMe(emit) }
    }

    private fun ASTNode.visitValueParameter(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        this.visitModifierList(emit)
        removeWhiteSpaceBetweenParameterIdentifierAndColon(emit)
        fixWhiteSpaceAfterColonInParameter(emit)
    }

    private fun ASTNode.visitModifierList(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        val modifierList = findChildByType(MODIFIER_LIST) ?: return
        modifierList.removeWhiteSpaceBetweenModifiersInList(emit)
        modifierList.removeWhiteSpaceBetweenModifierListAndParameterIdentifier(emit)
    }

    private fun ASTNode.removeWhiteSpaceBetweenModifiersInList(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        require(this.elementType == MODIFIER_LIST)
        children
            .filter { it.isWhiteSpace }
            // Store elements in the list before changing them as otherwise only the first whitespace is being changed
            .toList()
            .forEach { it.visitWhiteSpaceAfterModifier(emit) }
    }

    private fun ASTNode.visitWhiteSpaceAfterModifier(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        if (isIndent() && getPrecedingModifier()?.elementType == ANNOTATION_ENTRY) {
            // Ignore indent when the modifier is an annotation which is placed on a separate line
            return
        }
        if (isNotSingleSpace()) {
            replaceWithSingleSpace(emit)
        }
    }

    private fun ASTNode.removeWhiteSpaceBetweenModifierListAndParameterIdentifier(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        require(this.elementType == MODIFIER_LIST)
        nextSibling
            ?.takeIf { it.isWhiteSpace }
            ?.visitWhiteSpaceAfterModifier(emit)
    }

    private fun ASTNode.removeWhiteSpaceBetweenParameterIdentifierAndColon(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        findChildByType(COLON)
            ?.prevLeaf
            ?.takeIf { it.isWhiteSpace }
            ?.removeUnexpectedWhiteSpace(emit)
    }

    private fun ASTNode.fixWhiteSpaceAfterColonInParameter(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        val colonNode = findChildByType(COLON) ?: return
        colonNode
            .nextLeaf
            ?.takeIf { it.isWhiteSpace }
            .let { whiteSpaceAfterColon ->
                if (whiteSpaceAfterColon == null) {
                    colonNode.addMissingWhiteSpaceAfterMe(emit)
                } else {
                    if (isTypeReferenceWithModifierList() && whiteSpaceAfterColon.isIndent()) {
                        // Allow the type to be wrapped to the next line when it has a modifier:
                        //   data class Foo(
                        //       val bar:
                        //           @FooBar("foobar")
                        //           Bar,
                        //   )
                    } else if (whiteSpaceAfterColon.hasTypeReferenceWhichDoesNotFitOnSameLineAsColon()) {
                        // Allow the type to be wrapped to the next line when the type does not fit on same line as colon:
                        //   class Foo(
                        //       val someReallyLongFieldNameUsedInMyClass:
                        //           SomeReallyLongDependencyClass
                        //   )
                    } else if (whiteSpaceAfterColon.isNotSingleSpace()) {
                        whiteSpaceAfterColon.replaceWithSingleSpace(emit)
                    }
                }
            }
    }

    private fun ASTNode.addMissingWhiteSpaceAfterMe(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        require(this.elementType == COLON || this.elementType == COMMA)
        emit(this.startOffset, "Whitespace after '${this.text}' is missing", true)
            .ifAutocorrectAllowed { upsertWhitespaceAfterMe(" ") }
    }

    private fun ASTNode.isNotIndent(): Boolean = !isIndent()

    private fun ASTNode.isIndent(): Boolean = isWhiteSpaceWithNewline

    private fun ASTNode.isNotSingleSpace(): Boolean {
        require(isWhiteSpace)
        return text != " "
    }

    private fun ASTNode.removeUnexpectedWhiteSpace(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        emit(startOffset, "Unexpected whitespace", true)
            .ifAutocorrectAllowed { remove() }
    }

    private fun ASTNode.replaceWithSingleSpace(emit: (Int, String, Boolean) -> AutocorrectDecision) {
        emit(startOffset, "Expected a single space", true)
            .ifAutocorrectAllowed { replaceTextWith(" ") }
    }

    private fun ASTNode.getPrecedingModifier(): ASTNode? =
        prevCodeSibling
            ?.let { prevCodeSibling ->
                if (prevCodeSibling.elementType == MODIFIER_LIST) {
                    prevCodeSibling.lastChildNode
                } else {
                    require(prevCodeSibling.parent?.elementType == MODIFIER_LIST)
                    prevCodeSibling
                }
            }

    private fun ASTNode?.isTypeReferenceWithModifierList() =
        null !=
            this
                ?.findChildByType(TYPE_REFERENCE)
                ?.findChildByType(MODIFIER_LIST)

    private fun ASTNode.hasTypeReferenceWhichDoesNotFitOnSameLineAsColon() =
        takeIf { it.isWhiteSpaceWithNewline }
            ?.nextCodeSibling
            ?.takeIf { it.elementType == TYPE_REFERENCE }
            ?.let { typeReference ->
                val length =
                    // length of the previous line
                    leavesOnLine
                        .dropTrailingEolComment()
                        .lineLength +
                        // single space before type reference
                        1 -
                        // length of current indent before typeReference
                        this.text.substringAfterLast("\n").length +
                        // length of line containing typeReference
                        typeReference
                            .leavesOnLine
                            .dropTrailingEolComment()
                            .lineLength
                length > maxLineLength
            }
            ?: false
}

public val PARAMETER_LIST_SPACING_RULE_ID: RuleId = ParameterListSpacingRule().ruleId
