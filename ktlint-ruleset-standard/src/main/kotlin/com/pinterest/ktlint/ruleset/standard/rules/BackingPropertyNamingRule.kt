package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMPANION_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INTERNAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OVERRIDE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIVATE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROTECTED_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.android_studio
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.hasModifier
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * https://kotlinlang.org/docs/coding-conventions.html#property-names
 * https://developer.android.com/kotlin/style-guide#backing_properties
 */
@SinceKtlint("1.2.0", STABLE)
public class BackingPropertyNamingRule :
    StandardRule(
        id = "backing-property-naming",
        usesEditorConfigProperties = setOf(CODE_STYLE_PROPERTY),
    ) {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { node.elementType == PROPERTY }
            ?.let { property -> visitProperty(property, emit) }
    }

    private fun visitProperty(
        property: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        property
            .findChildByType(IDENTIFIER)
            ?.takeIf { it.text.startsWith("_") }
            ?.takeUnless {
                // Do not report overridden properties as they can only be changed by changing the base property
                it.treeParent.hasModifier(OVERRIDE_KEYWORD)
            }?.let { identifier ->
                visitBackingProperty(identifier, emit)
            }
    }

    private fun visitBackingProperty(
        identifier: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        identifier
            .text
            .takeUnless { it.matches(BACKING_PROPERTY_LOWER_CAMEL_CASE_REGEXP) }
            ?.let {
                emit(identifier.startOffset, "Backing property should start with underscore followed by lower camel case", false)
            }

        if (!identifier.hasPrivateModifierInPropertyDeclaration() && !identifier.isDeclaredInPrivateCompanionObject()) {
            emit(identifier.startOffset, "Backing property not allowed when 'private' modifier is missing", false)
        }

        // A backing property can only exist when a correlated public property or function exists
        val correlatedPropertyOrFunction = identifier.findCorrelatedPropertyOrFunction()
        if (correlatedPropertyOrFunction == null) {
            emit(identifier.startOffset, "Backing property is only allowed when a matching property or function exists", false)
        } else {
            if (codeStyle == android_studio || correlatedPropertyOrFunction.isPublic()) {
                return
            } else {
                emit(identifier.startOffset, "Backing property is only allowed when the matching property or function is public", false)
            }
        }
    }

    private fun ASTNode.hasPrivateModifierInPropertyDeclaration() = treeParent.hasModifier(PRIVATE_KEYWORD)

    private fun ASTNode.isDeclaredInPrivateCompanionObject() =
        true ==
            treeParent
                .treeParent
                .findCompanionObject()
                ?.treeParent
                ?.treeParent
                ?.hasModifier(PRIVATE_KEYWORD)

    private fun ASTNode.findCorrelatedPropertyOrFunction() = findCorrelatedProperty() ?: findCorrelatedFunction()

    private fun ASTNode.findCorrelatedProperty(): ASTNode? {
        val propertyName = text.removePrefix("_")
        return parent(CLASS_BODY)
            ?.let { classBody ->
                // Check if related property exists in the same class body. If not, and the class body is the body of a companion object,
                // than also search in the class body where the companion object is defined.
                classBody.findPropertyWithName(propertyName)
                    ?: classBody
                        .findCompanionObject()
                        ?.parent(CLASS_BODY)
                        ?.findPropertyWithName(propertyName)
            }
    }

    private fun ASTNode.findPropertyWithName(name: String) =
        this
            .children()
            .filter { it.elementType == PROPERTY }
            .mapNotNull { it.findChildByType(IDENTIFIER) }
            .firstOrNull { it.text == name }
            ?.treeParent

    private fun ASTNode.findCompanionObject() =
        treeParent
            ?.takeIf { it.elementType == OBJECT_DECLARATION }
            ?.findChildByType(MODIFIER_LIST)
            ?.children()
            ?.firstOrNull { it.elementType == COMPANION_KEYWORD }

    private fun ASTNode.findCorrelatedFunction(): ASTNode? {
        val correlatedFunctionName = "get${capitalizeFirstChar()}"
        return parent(CLASS_BODY)
            ?.let { classBody ->
                // Check if related property exists in the same class body. If not, and the class body is the body of a companion object,
                // than also search in the class body where the companion object is defined.
                classBody.findFunctionWithName(correlatedFunctionName)
                    ?: classBody
                        .findCompanionObject()
                        ?.parent(CLASS_BODY)
                        ?.findFunctionWithName(correlatedFunctionName)
            }
    }

    private fun ASTNode.findFunctionWithName(name: String) =
        children()
            .filter { it.elementType == FUN }
            .filter { it.hasNonEmptyParameterList() }
            .mapNotNull { it.findChildByType(IDENTIFIER) }
            .firstOrNull { it.text == name }
            ?.treeParent

    private fun ASTNode.hasNonEmptyParameterList() =
        findChildByType(VALUE_PARAMETER_LIST)
            ?.children()
            ?.none { it.elementType == VALUE_PARAMETER }
            ?: false

    private fun ASTNode.capitalizeFirstChar() =
        text
            .removePrefix("_")
            .replaceFirstChar { it.uppercase() }

    private fun ASTNode.isPublic() =
        !hasModifier(PRIVATE_KEYWORD) &&
            !hasModifier(PROTECTED_KEYWORD) &&
            !hasModifier(INTERNAL_KEYWORD)

    private companion object {
        val BACKING_PROPERTY_LOWER_CAMEL_CASE_REGEXP = "_[a-z][a-zA-Z0-9]*".regExIgnoringDiacriticsAndStrokesOnLetters()
    }
}

public val BACKING_PROPERTY_NAMING_RULE_ID: RuleId = BackingPropertyNamingRule().ruleId
