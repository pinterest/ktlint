package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONST_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GET_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OVERRIDE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.hasModifier
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens

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
            ?.takeUnless { it.isTokenKeywordBetweenBackticks() }
            ?.let { identifier ->
                when {
                    property.hasConstModifier() -> {
                        visitConstProperty(identifier, emit)
                    }
                    property.hasCustomGetter() || property.isTopLevelValue() || property.isObjectValue() -> {
                        // Can not reliably determine whether the value is immutable or not
                    }
                    else -> {
                        visitNonConstProperty(identifier, emit)
                    }
                }
            }
    }

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
            ?.takeUnless {
                // Ignore backing properties
                it.startsWith("_")
            }?.let {
                emit(identifier.startOffset, "Property name should start with a lowercase letter and use camel case", false)
            }
    }

    private fun ASTNode.hasCustomGetter() = findChildByType(PROPERTY_ACCESSOR)?.findChildByType(GET_KEYWORD) != null

    private fun ASTNode.hasConstModifier() = hasModifier(CONST_KEYWORD)

    private fun ASTNode.isTopLevelValue() = treeParent.elementType == FILE && containsValKeyword()

    private fun ASTNode.containsValKeyword() = children().any { it.elementType == VAL_KEYWORD }

    private fun ASTNode.isObjectValue() =
        treeParent.elementType == CLASS_BODY &&
            treeParent?.treeParent?.elementType == OBJECT_DECLARATION &&
            containsValKeyword() &&
            !hasModifier(OVERRIDE_KEYWORD)

    private fun ASTNode.isTokenKeywordBetweenBackticks() =
        this
            .takeIf { it.elementType == IDENTIFIER }
            ?.text
            .orEmpty()
            .removeSurrounding("`")
            .let { KEYWORDS.contains(it) }

    private companion object {
        val LOWER_CAMEL_CASE_REGEXP = "[a-z][a-zA-Z0-9]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        val SCREAMING_SNAKE_CASE_REGEXP = "[A-Z][_A-Z0-9]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        const val SERIAL_VERSION_UID_PROPERTY_NAME = "serialVersionUID"
        val KEYWORDS =
            setOf(KtTokens.KEYWORDS, KtTokens.SOFT_KEYWORDS)
                .flatMap { tokenSet -> tokenSet.types.mapNotNull { it.debugName } }
                .filterNot { keyword ->
                    // The keyword sets contain a few 'keywords' which should be ignored. All valid keywords only contain lowercase
                    // characters
                    keyword.any { it.isUpperCase() }
                }.toSet()
    }
}

public val PROPERTY_NAMING_RULE_ID: RuleId = PropertyNamingRule().ruleId
