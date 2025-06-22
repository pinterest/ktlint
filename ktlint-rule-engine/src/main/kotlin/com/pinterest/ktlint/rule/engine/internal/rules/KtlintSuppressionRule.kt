package com.pinterest.ktlint.rule.engine.internal.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.IgnoreKtlintSuppressions
import com.pinterest.ktlint.rule.engine.core.api.KtlintKotlinCompiler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.findChildByTypeRecursively
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.rule.engine.core.api.replaceWith
import com.pinterest.ktlint.rule.engine.internal.KTLINT_SUPPRESSION_ID_ALL_RULES
import com.pinterest.ktlint.rule.engine.internal.insertKtlintRuleSuppression
import com.pinterest.ktlint.rule.engine.internal.isKtlintSuppressionId
import com.pinterest.ktlint.rule.engine.internal.isTopLevel
import com.pinterest.ktlint.rule.engine.internal.qualifiedRuleIdString
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.KtlintDirectiveType.KTLINT_DISABLE
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.KtlintDirectiveType.KTLINT_ENABLE
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.SuppressionIdChange.InvalidSuppressionId
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.SuppressionIdChange.ValidSuppressionId
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.SuppressionIdChange.ValidSuppressionId.Companion.KTLINT_SUPPRESSION_ALL
import com.pinterest.ktlint.rule.engine.internal.toFullyQualifiedKtlintSuppressionId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

/**
 * Disallow usage of the old "ktlint-disable" and "ktlint-enable" directives.
 *
 * A ktlint-disable directive is replaced with an annotation on the closest parent declaration or expression, or as annotation on the file
 * level in case the directive is associated with a top level element. Ktlint-disable directives placed in block comments are only
 * autocorrected when placed as top level element or in case a matching Ktlint-enable directive is found in the same parent as the disable
 * directive.
 *
 * If the target element is annotated with a [Suppress] (or, if missing, is annotated with a [SuppressWarnings] annotation) then the
 * ktlint-directive will be matched against this annotation. If this annotation already contains a suppression for *all* ktlint rules, or
 * for the specific rule id, then it is not added to annotation as it would be redundant. In case a suppression identifier is added to an
 * existing annotation then all identifiers in the annotation are alphabetically sorted.
 *
 * If the target element is not annotated with [Suppress] or [SuppressWarnings] then a [Suppress] annotation is added.
 *
 * Ktlint-enable directives are removed as annotations have a scope in which the suppression will be active.
 */
public class KtlintSuppressionRule(
    private val allowedRuleIds: List<RuleId>,
) : InternalRule("ktlint-suppression"),
    // The SuppressionLocatorBuilder no longer support the old ktlint suppression directives using comments. This rule may not be disabled
    // in any way as it would fail to process suppressions.
    IgnoreKtlintSuppressions {
    private val allowedRuleIdAsStrings = allowedRuleIds.map { it.value }

    private val ruleIdValidator: (String) -> Boolean = { ruleId -> allowedRuleIdAsStrings.contains(ruleId) }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { isKtlintRuleSuppressionInAnnotation(it) }
            ?.let { visitKtlintSuppressionInAnnotation(node, emit) }

        node
            .ktlintDirectiveOrNull(ruleIdValidator)
            ?.visitKtlintDirective(emit)
    }

    private fun isKtlintRuleSuppressionInAnnotation(node: ASTNode) =
        node
            .takeIf { it.elementType == STRING_TEMPLATE }
            ?.takeIf { it.text.isKtlintSuppressionId() }
            ?.let { literalStringTemplate ->
                literalStringTemplate
                    .parent(VALUE_ARGUMENT)
                    ?.isPartOfAnnotation()
            }
            ?: false

    private fun ASTNode.isPartOfAnnotation() = parent { it.elementType == ANNOTATION || it.elementType == ANNOTATION_ENTRY } != null

    private fun visitKtlintSuppressionInAnnotation(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .findChildByTypeRecursively(ElementType.LITERAL_STRING_TEMPLATE_ENTRY, includeSelf = true)
            ?.let { literalStringTemplateEntry ->
                val prefixedSuppression =
                    literalStringTemplateEntry
                        .text
                        .toFullyQualifiedKtlintSuppressionId()
                        .removeSurrounding("\"")
                val offset = literalStringTemplateEntry.startOffset
                if (prefixedSuppression.isUnknownKtlintSuppression()) {
                    emit(offset, "Ktlint rule with id '$prefixedSuppression' is unknown or not loaded", false)
                    Unit
                } else if (prefixedSuppression != literalStringTemplateEntry.text) {
                    emit(offset, "Identifier to suppress ktlint rule must be fully qualified with the rule set id", true)
                        .ifAutocorrectAllowed {
                            createLiteralStringTemplateEntry(prefixedSuppression)
                                ?.let { literalStringTemplateEntry.replaceWith(it) }
                        }
                }
            }
    }

    private fun ASTNode.removePrecedingWhitespace() {
        prevLeaf()
            .takeIf { it.isWhiteSpace20 }
            ?.remove()
    }

    private fun createLiteralStringTemplateEntry(prefixedSuppression: String) =
        KtlintKotlinCompiler
            .createASTNodeFromText("listOf(\"$prefixedSuppression\")")
            ?.findChildByType(CALL_EXPRESSION)
            ?.findChildByType(VALUE_ARGUMENT_LIST)
            ?.findChildByType(VALUE_ARGUMENT)
            ?.findChildByType(STRING_TEMPLATE)
            ?.findChildByType(LITERAL_STRING_TEMPLATE_ENTRY)

    private fun KtLintDirective.visitKtlintDirective(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (ktlintDirectiveType) {
            KTLINT_DISABLE -> {
                if (node.elementType == EOL_COMMENT && node.prevLeaf().isWhiteSpaceWithNewline20) {
                    removeDanglingEolCommentWithKtlintDisableDirective(emit)
                } else {
                    visitKtlintDisableDirective(emit)
                }
            }

            KTLINT_ENABLE -> {
                removeKtlintEnableDirective(emit)
            }
        }
    }

    private fun KtLintDirective.removeDanglingEolCommentWithKtlintDisableDirective(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(offset, "Directive 'ktlint-disable' in EOL comment is ignored as it is not preceded by a code element", true)
            .ifAutocorrectAllowed {
                node.removePrecedingWhitespace()
                node.remove()
            }
    }

    private fun KtLintDirective.visitKtlintDisableDirective(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == BLOCK_COMMENT && hasNoMatchingKtlintEnableDirective(ruleIdValidator)) {
            emit(
                offset,
                "Directive 'ktlint-disable' is deprecated. The matching 'ktlint-enable' directive is not found in same scope. Replace " +
                    "with @Suppress annotation",
                false,
            )
            return
        }
        val autocorrectDecision = emit(offset, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation", true)
        suppressionIdChanges
            .filterIsInstance<InvalidSuppressionId>()
            .forEach { ktlintDirectiveChange ->
                emit(
                    offset +
                        ktlintDirectiveType.id.length +
                        ktlintDirectiveChange.offsetOriginalRuleId,
                    "Ktlint rule with id '${ktlintDirectiveChange.originalRuleId}' is unknown or not loaded",
                    false,
                )
            }
        autocorrectDecision.ifAutocorrectAllowed {
            val suppressionIds =
                suppressionIdChanges
                    .filterIsInstance<ValidSuppressionId>()
                    .map { it.suppressionId }
                    .toSet()
            node
                .applyIf(node.elementType == BLOCK_COMMENT && shouldBePromotedToParentDeclaration(ruleIdValidator)) {
                    treeParent.treeParent
                }.insertKtlintRuleSuppression(suppressionIds, forceFileAnnotation = node.shouldBeConvertedToFileAnnotation())
            if (node.elementType == EOL_COMMENT) {
                node.removePrecedingWhitespace()
            } else {
                if (node.nextLeaf.isWhiteSpaceWithNewline20) {
                    node
                        .nextLeaf
                        ?.remove()
                } else {
                    node.removePrecedingWhitespace()
                }
            }
            node.remove()
        }
    }

    private fun KtLintDirective.removeKtlintEnableDirective(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(offset, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations", true)
            .ifAutocorrectAllowed {
                node.removePrecedingWhitespace()
                node.remove()
            }
    }

    private fun String.isUnknownKtlintSuppression(): Boolean =
        qualifiedRuleIdString()
            .let { ruleId ->
                allowedRuleIds.none { it.value == ruleId }
            }
}

private class KtLintDirective(
    val node: ASTNode,
    val ruleIdValidator: (String) -> Boolean,
    val ktlintDirectiveType: KtlintDirectiveType,
    val ktlintDirectives: String,
    val suppressionIdChanges: Set<SuppressionIdChange>,
) {
    val offset = node.startOffset + node.text.indexOf(ktlintDirectiveType.id)

    fun hasNoMatchingKtlintEnableDirective(ruleIdValidator: (String) -> Boolean): Boolean {
        require(ktlintDirectiveType == KTLINT_DISABLE && node.elementType == BLOCK_COMMENT)

        return if (node.shouldBeConvertedToFileAnnotation()) {
            false
        } else {
            findMatchingKtlintEnableDirective(ruleIdValidator) == null
        }
    }

    private fun findMatchingKtlintEnableDirective(ruleIdValidator: (String) -> Boolean) =
        node
            .applyIf(node.isSuppressibleDeclaration()) { node.treeParent }
            .siblings()
            .firstOrNull { node ->
                node
                    .ktlintDirectiveOrNull(ruleIdValidator)
                    ?.takeIf { it.ktlintDirectiveType == KTLINT_ENABLE }
                    ?.ktlintDirectives == ktlintDirectives
            }

    fun shouldBePromotedToParentDeclaration(ruleIdValidator: (String) -> Boolean): Boolean {
        require(ktlintDirectiveType == KTLINT_DISABLE && node.elementType == BLOCK_COMMENT)

        if (node.shouldBeConvertedToFileAnnotation()) {
            return false
        }

        if (node.isSuppressibleDeclaration()) {
            return findMatchingKtlintEnableDirective(ruleIdValidator)
                ?.let { matchingKtlintEnabledDirective ->
                    // In case the node is part of a suppressible declaration and the next sibling matches the enable directive then the
                    // block directive should be match with this declaration only and not be moved to the parent.
                    matchingKtlintEnabledDirective !=
                        node
                            .treeParent
                            .nextSibling { !it.isWhiteSpace20 }
                }
                ?: false
        }

        return node.surroundsMultipleListElements()
    }

    private fun ASTNode.surroundsMultipleListElements(): Boolean {
        require(ktlintDirectiveType == KTLINT_DISABLE && elementType == BLOCK_COMMENT)
        return if (treeParent.elementType in listTypeTokenSet) {
            findMatchingKtlintEnableDirective(ruleIdValidator)
                ?.siblings(false)
                ?.takeWhile { it != this }
                ?.count { it.elementType in listElementTypeTokenSet }
                ?.let { it > 1 }
                ?: false
        } else {
            false
        }
    }

    private val listTypeTokenSet =
        TokenSet.create(
            ElementType.TYPE_ARGUMENT_LIST,
            ElementType.TYPE_PARAMETER_LIST,
            ElementType.VALUE_ARGUMENT_LIST,
            ElementType.VALUE_PARAMETER_LIST,
        )

    private val listElementTypeTokenSet =
        TokenSet.create(
            ElementType.TYPE_PROJECTION,
            ElementType.TYPE_PARAMETER,
            VALUE_ARGUMENT,
            ElementType.VALUE_PARAMETER,
        )

    enum class KtlintDirectiveType(
        val id: String,
    ) {
        KTLINT_DISABLE("ktlint-disable"),
        KTLINT_ENABLE("ktlint-enable"),
    }

    sealed class SuppressionIdChange {
        class ValidSuppressionId(
            val suppressionId: String,
        ) : SuppressionIdChange() {
            companion object {
                val KTLINT_SUPPRESSION_ALL = ValidSuppressionId(KTLINT_SUPPRESSION_ID_ALL_RULES)
            }
        }

        class InvalidSuppressionId(
            val originalRuleId: String,
            val offsetOriginalRuleId: Int,
        ) : SuppressionIdChange()
    }
}

private fun ASTNode.ktlintDirectiveOrNull(ruleIdValidator: (String) -> Boolean): KtLintDirective? {
    val ktlintDirectiveString =
        when (elementType) {
            EOL_COMMENT -> {
                text
                    .removePrefix("//")
                    .trim()
            }

            BLOCK_COMMENT -> {
                text
                    .removePrefix("/*")
                    .removeSuffix("*/")
                    .trim()
            }

            else -> {
                return null
            }
        }
    val ktlintDirectiveType =
        ktlintDirectiveString.toKtlintDirectiveTypeOrNull()
            ?: return null
    val ruleIds = ktlintDirectiveString.removePrefix(ktlintDirectiveType.id)
    val suppressionIdChanges = ruleIds.toSuppressionIdChanges(ruleIdValidator)

    return KtLintDirective(this, ruleIdValidator, ktlintDirectiveType, ruleIds, suppressionIdChanges)
}

private fun String.toKtlintDirectiveTypeOrNull() =
    when {
        startsWith(KTLINT_DISABLE.id) -> KTLINT_DISABLE
        startsWith(KTLINT_ENABLE.id) -> KTLINT_ENABLE
        else -> null
    }

// Transform the string: "ktlint-disable foo standard:bar"
// to a (sorted) list containing elements:
//    ktlint:standard:bar
//    ktlint:standard:foo
private fun String.toSuppressionIdChanges(ruleIdValidator: (String) -> Boolean) =
    trim()
        .split(" ")
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.qualifiedRuleIdString() }
        .map { originalRuleId ->
            if (ruleIdValidator(originalRuleId)) {
                ValidSuppressionId(originalRuleId)
            } else {
                InvalidSuppressionId(
                    originalRuleId,
                    this.indexOf(originalRuleId),
                )
            }
        }.toSet()
        .ifEmpty { setOf(KTLINT_SUPPRESSION_ALL) }

private fun ASTNode.shouldBeConvertedToFileAnnotation() =
    isTopLevel() ||
        (elementType == BLOCK_COMMENT && isSuppressibleDeclaration() && treeParent.isTopLevel())

private fun ASTNode.isSuppressibleDeclaration() =
    when (treeParent.elementType) {
        ElementType.CLASS, ElementType.FUN, ElementType.PROPERTY -> true
        else -> false
    }

public val KTLINT_SUPPRESSION_RULE_ID: RuleId = KtlintSuppressionRule(emptyList()).ruleId
