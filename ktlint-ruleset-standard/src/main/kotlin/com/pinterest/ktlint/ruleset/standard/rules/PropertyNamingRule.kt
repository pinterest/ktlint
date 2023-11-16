package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONST_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GET_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INTERNAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OVERRIDE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIVATE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROTECTED_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * https://kotlinlang.org/docs/coding-conventions.html#function-names
 * https://kotlinlang.org/docs/coding-conventions.html#property-names
 */
@SinceKtlint("0.48", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class PropertyNamingRule : StandardRule("property-naming") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { node.elementType == PROPERTY }
            ?.let { property -> visitProperty(property, emit) }
    }

    private fun visitProperty(
        property: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        property
            .findChildByType(IDENTIFIER)
            ?.let { identifier ->
                when {
                    property.hasConstModifier() -> {
                        visitConstProperty(identifier, emit)
                    }
                    property.hasCustomGetter() || property.isTopLevelValue() || property.isObjectValue() -> {
                        // Can not reliably determine whether the value is immutable or not
                    }
                    identifier.text.startsWith("_") -> {
                        visitBackingProperty(identifier, emit)
                    }
                    else -> {
                        visitNonConstProperty(identifier, emit)
                    }
                }
            }
    }

    private fun visitBackingProperty(
        identifier: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        identifier
            .text
            .takeUnless { it.matches(BACKING_PROPERTY_LOWER_CAMEL_CASE_REGEXP) }
            ?.let {
                emit(identifier.startOffset, "Backing property name should start with underscore followed by lower camel case", false)
            }

        if (!identifier.treeParent.hasModifier(PRIVATE_KEYWORD)) {
            emit(identifier.startOffset, "Backing property name not allowed when 'private' modifier is missing", false)
        }

        // A backing property can only exist when a correlated public property or function exists
        identifier
            .treeParent
            ?.treeParent
            ?.children()
            ?.filter { it.elementType == PROPERTY }
            ?.mapNotNull { it.findChildByType(IDENTIFIER) }
            ?.firstOrNull { it.text == identifier.text.removePrefix("_") }
            ?.treeParent
            ?.let { correlatedProperty ->
                if (correlatedProperty.isNotPublic()) {
                    return
                }
            }

        val correlatedFunctionName = "get${identifier.capitalizeFirstChar()}"
        identifier
            .treeParent
            ?.treeParent
            ?.children()
            ?.filter { it.elementType == FUN }
            ?.filter { it.hasNonEmptyParameterList() }
            ?.mapNotNull { it.findChildByType(IDENTIFIER) }
            ?.firstOrNull { it.text == correlatedFunctionName }
            ?.treeParent
            ?.let { correlatedFunction ->
                if (correlatedFunction.isNotPublic()) {
                    return
                }
            }

        emit(identifier.startOffset, "Backing property name is only allowed when a matching public property or function exists", false)
    }

    private fun ASTNode.hasNonEmptyParameterList() =
        findChildByType(VALUE_PARAMETER_LIST)
            ?.children()
            ?.none { it.elementType == VALUE_PARAMETER }
            ?: false

    private fun ASTNode.capitalizeFirstChar() =
        text
            .removePrefix("_")
            .replaceFirstChar { it.uppercase() }

    private fun visitConstProperty(
        identifier: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        identifier
            .text
            .takeUnless {
                // Allow
                // object Foo {
                //     private const val serialVersionUID: Long = 123
                // }
                it == SERIAL_VERSION_UID_PROPERTY_NAME
            }?.takeUnless { it.matches(SCREAMING_SNAKE_CASE_REGEXP) }
            ?.let {
                emit(
                    identifier.startOffset,
                    "Property name should use the screaming snake case notation when the value can not be changed",
                    false,
                )
            }
    }

    private fun visitNonConstProperty(
        identifier: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        identifier
            .text
            .takeUnless { it.matches(LOWER_CAMEL_CASE_REGEXP) }
            ?.let {
                emit(identifier.startOffset, "Property name should start with a lowercase letter and use camel case", false)
            }
    }

    private fun ASTNode.hasCustomGetter() = findChildByType(PROPERTY_ACCESSOR)?.findChildByType(GET_KEYWORD) != null

    private fun ASTNode.hasConstModifier() = hasModifier(CONST_KEYWORD)

    private fun ASTNode.hasModifier(iElementType: IElementType) =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == iElementType }

    private fun ASTNode.isTopLevelValue() = treeParent.elementType == FILE && containsValKeyword()

    private fun ASTNode.containsValKeyword() = children().any { it.elementType == VAL_KEYWORD }

    private fun ASTNode.isObjectValue() =
        treeParent.elementType == CLASS_BODY &&
            treeParent?.treeParent?.elementType == OBJECT_DECLARATION &&
            containsValKeyword() &&
            !hasModifier(OVERRIDE_KEYWORD)

    private fun ASTNode.isNotPublic() =
        !hasModifier(PRIVATE_KEYWORD) &&
            !hasModifier(PROTECTED_KEYWORD) &&
            !hasModifier(INTERNAL_KEYWORD)

    private companion object {
        val LOWER_CAMEL_CASE_REGEXP = "[a-z][a-zA-Z0-9]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        val SCREAMING_SNAKE_CASE_REGEXP = "[A-Z][_A-Z0-9]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        val BACKING_PROPERTY_LOWER_CAMEL_CASE_REGEXP = "_[a-z][a-zA-Z0-9]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        const val SERIAL_VERSION_UID_PROPERTY_NAME = "serialVersionUID"
    }
}

public val PROPERTY_NAMING_RULE_ID: RuleId = PropertyNamingRule().ruleId
