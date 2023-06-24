package com.pinterest.ktlint.rule.engine.internal.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isRoot
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.KtlintDirectiveType.KTLINT_DISABLE
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.KtlintDirectiveType.KTLINT_ENABLE
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.SuppressionIdChange.InvalidSuppressionId
import com.pinterest.ktlint.rule.engine.internal.rules.KtLintDirective.SuppressionIdChange.ValidSuppressionId
import com.pinterest.ktlint.rule.engine.internal.rules.KtlintSuppressionRule.SuppressAnnotationType.SUPPRESS
import com.pinterest.ktlint.rule.engine.internal.rules.KtlintSuppressionRule.SuppressAnnotationType.SUPPRESS_WARNINGS
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.util.prefixIfNot
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

private const val KTLINT_SUPPRESSION_ID_PREFIX = "ktlint:"
private const val KTLINT_SUPPRESSION_ID_ALL_RULES = "\"ktlint\""
private const val DOUBLE_QUOTE = "\""

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
public class KtlintSuppressionRule(private val allowedRuleIds: List<RuleId>) : InternalRule("ktlint-suppression") {
    private val allowedRuleIdAsStrings = allowedRuleIds.map { it.value }

    private val ruleIdValidator: (String) -> Boolean = { ruleId -> allowedRuleIdAsStrings.contains(ruleId) }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { isKtlintRuleSuppressionInAnnotation(it) }
            ?.let { visitKtlintSuppressionInAnnotation(node, autoCorrect, emit) }

        node
            .ktlintDirectiveOrNull(ruleIdValidator)
            ?.visitKtlintDirective(autoCorrect, emit)
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

    private fun String.isKtlintSuppressionId() = removePrefix(DOUBLE_QUOTE).startsWith(KTLINT_SUPPRESSION_ID_PREFIX)

    private fun ASTNode.isPartOfAnnotation() = parent { it.elementType == ANNOTATION || it.elementType == ANNOTATION_ENTRY } != null

    private fun visitKtlintSuppressionInAnnotation(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .psi
            .findDescendantOfType<KtLiteralStringTemplateEntry>()
            ?.node
            ?.let { literalStringTemplateEntry ->
                val prefixedSuppression =
                    literalStringTemplateEntry
                        .text
                        .prefixKtlintSuppressionWithRuleSetIdOrNull()
                val offset = literalStringTemplateEntry.startOffset + KTLINT_SUPPRESSION_ID_PREFIX.length
                if (prefixedSuppression.isUnknownKtlintSuppression()) {
                    emit(offset, "Ktlint rule with id '$prefixedSuppression' is unknown or not loaded", false)
                } else if (prefixedSuppression != literalStringTemplateEntry.text) {
                    emit(offset, "Identifier to suppress ktlint rule must be fully qualified with the rule set id", true)
                    if (autoCorrect) {
                        node
                            .createLiteralStringTemplateEntry(prefixedSuppression)
                            ?.let { literalStringTemplateEntry.replaceWith(it) }
                    }
                }
            }
    }

    private fun ASTNode.createLiteralStringTemplateEntry(prefixedSuppression: String) =
        PsiFileFactory
            .getInstance(psi.project)
            .createFileFromText(KotlinLanguage.INSTANCE, "listOf(\"$prefixedSuppression\")")
            .getChildOfType<KtScript>()
            ?.getChildOfType<KtBlockExpression>()
            ?.getChildOfType<KtScriptInitializer>()
            ?.getChildOfType<KtCallExpression>()
            ?.getChildOfType<KtValueArgumentList>()
            ?.getChildOfType<KtValueArgument>()
            ?.getChildOfType<KtStringTemplateExpression>()
            ?.getChildOfType<KtLiteralStringTemplateEntry>()
            ?.node

    private fun String.prefixKtlintSuppressionWithRuleSetIdOrNull(): String {
        val isPrefixedWithDoubleQuote = startsWith(DOUBLE_QUOTE)
        return removePrefix(DOUBLE_QUOTE)
            .takeIf { startsWith(KTLINT_SUPPRESSION_ID_PREFIX) }
            ?.substringAfter(KTLINT_SUPPRESSION_ID_PREFIX)
            ?.let { prefixWithRuleSetIdWhenMissing(it) }
            ?.prefixIfNot(KTLINT_SUPPRESSION_ID_PREFIX)
            ?.applyIf(isPrefixedWithDoubleQuote) { prefixIfNot(DOUBLE_QUOTE) }
            ?: this
    }

    private fun String.isUnknownKtlintSuppression(): Boolean =
        removePrefix(DOUBLE_QUOTE)
            .takeIf { startsWith(KTLINT_SUPPRESSION_ID_PREFIX) }
            ?.substringAfter(KTLINT_SUPPRESSION_ID_PREFIX)
            ?.let { prefixWithRuleSetIdWhenMissing(it) }
            ?.let { ruleId ->
                allowedRuleIds.none { it.value == ruleId }
            }
            ?: false

    private fun KtLintDirective.visitKtlintDirective(
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (ktlintDirectiveType) {
            KTLINT_DISABLE -> {
                if (node.elementType == EOL_COMMENT && node.prevLeaf().isWhiteSpaceWithNewline()) {
                    removeDanglingEolCommentWithKtlintDisableDirective(autoCorrect, emit)
                } else {
                    visitKtlintDisableDirective(autoCorrect, emit)
                }
            }

            KTLINT_ENABLE -> {
                removeKtlintEnableDirective(autoCorrect, emit)
            }
        }
    }

    private fun KtLintDirective.removeDanglingEolCommentWithKtlintDisableDirective(
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(offset, "Directive 'ktlint-disable' in EOL comment is ignored as it is not preceded by a code element", true)
        if (autoCorrect) {
            node.removePrecedingWhitespace()
            node.remove()
        }
    }

    private fun KtLintDirective.visitKtlintDisableDirective(
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
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
        emit(offset, "Directive 'ktlint-disable' is deprecated. Replace with @Suppress annotation", true)
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
        if (autoCorrect) {
            findParentDeclarationOrExpression()
                .addKtlintRuleSuppression(suppressionIdChanges)
            if (node.elementType == EOL_COMMENT) {
                node.removePrecedingWhitespace()
            } else {
                if (node.nextLeaf().isWhiteSpaceWithNewline()) {
                    node
                        .nextLeaf()
                        ?.remove()
                } else {
                    node.removePrecedingWhitespace()
                }
            }
            node.remove()
        }
    }

    private fun KtLintDirective.findParentDeclarationOrExpression(): ASTNode {
        val shouldBeConvertedToFileAnnotation = shouldBeConvertedToFileAnnotation()
        var targetNode =
            if (node.elementType == BLOCK_COMMENT &&
                shouldBePromotedToParentDeclaration(ruleIdValidator)
            ) {
                node.treeParent.treeParent.psi
            } else {
                node.psi
            }
        while (
            shouldBeConvertedToFileAnnotation ||
            targetNode is KtClassInitializer ||
            targetNode is KtBlockExpression ||
            targetNode is KtPrimaryConstructor ||
            (targetNode !is KtDeclaration && targetNode !is KtExpression)
        ) {
            if (targetNode.parent == null) {
                return targetNode.node
            }
            targetNode = targetNode.parent
        }
        return targetNode.node
    }

    private fun ASTNode.remove() {
        treeParent.removeChild(this)
    }

    private fun ASTNode.removePrecedingWhitespace() {
        prevLeaf()
            .takeIf { it.isWhiteSpace() }
            ?.remove()
    }

    private fun ASTNode.addKtlintRuleSuppression(suppressionIdChanges: Set<KtLintDirective.SuppressionIdChange>) {
        val ktlintRuleSuppressions =
            suppressionIdChanges
                .filterIsInstance<ValidSuppressionId>()
                .map { it.suppressionId }
                .toSet()
        if (ktlintRuleSuppressions.isEmpty()) {
            // Do not add or alter the @Suppress / @SuppressWarnings
            return
        }
        val suppressionAnnotations = findSuppressionAnnotations()
        // Add ktlint rule suppressions:
        //   - To the @Suppress annotation if found
        //   - otherwise to the @SuppressWarnings annotation if found
        //   - otherwise create a new @Suppress annotation
        when {
            suppressionAnnotations.containsKey(SUPPRESS) ->
                ktlintRuleSuppressions.mergeInto(suppressionAnnotations.getValue(SUPPRESS), SUPPRESS)

            suppressionAnnotations.containsKey(SUPPRESS_WARNINGS) ->
                ktlintRuleSuppressions.mergeInto(suppressionAnnotations.getValue(SUPPRESS_WARNINGS), SUPPRESS_WARNINGS)

            else -> createSuppressAnnotation(SUPPRESS, ktlintRuleSuppressions)
        }
    }

    private fun Set<String>.mergeInto(
        annotationNode: ASTNode,
        suppressType: SuppressAnnotationType,
    ) {
        annotationNode
            .getValueArguments()
            .plus(this)
            .let { suppressions ->
                if (suppressions.contains(KTLINT_SUPPRESSION_ID_ALL_RULES)) {
                    // When all ktlint rules are to be suppressed, then ignore all suppressions for specific ktlint rules
                    suppressions
                        .filterNot { it.isKtlintSuppressionId() }
                        .toSet()
                } else {
                    suppressions
                }
            }.map { it.prefixKtlintSuppressionWithRuleSetIdOrNull() }
            .toSet()
            .let { suppressions -> annotationNode.createSuppressAnnotation(suppressType, suppressions) }
    }

    private fun ASTNode.findSuppressionAnnotations(): Map<SuppressAnnotationType, ASTNode> =
        if (this.isRoot()) {
            findChildByType(FILE_ANNOTATION_LIST)
                ?.findSuppressionAnnotationsInModifierList()
                .orEmpty()
        } else {
            findChildByType(MODIFIER_LIST)
                ?.findSuppressionAnnotationsInModifierList()
                .orEmpty()
        }

    private fun ASTNode.findSuppressionAnnotationsInModifierList(): Map<SuppressAnnotationType, ASTNode> =
        children()
            .mapNotNull { modifier ->
                when (modifier.suppressionAnnotationTypeOrNull()) {
                    SUPPRESS -> Pair(SUPPRESS, modifier)
                    SUPPRESS_WARNINGS -> Pair(SUPPRESS_WARNINGS, modifier)
                    else -> null
                }
            }.toMap()

    private fun ASTNode.suppressionAnnotationTypeOrNull() =
        takeIf { elementType == ANNOTATION || elementType == ANNOTATION_ENTRY }
            ?.findChildByType(ElementType.CONSTRUCTOR_CALLEE)
            ?.findChildByType(ElementType.TYPE_REFERENCE)
            ?.findChildByType(ElementType.USER_TYPE)
            ?.findChildByType(ElementType.REFERENCE_EXPRESSION)
            ?.findChildByType(ElementType.IDENTIFIER)
            ?.text
            ?.let { SuppressAnnotationType.findByIdOrNull(it) }

    private fun ASTNode.getValueArguments() =
        findChildByType(VALUE_ARGUMENT_LIST)
            ?.children()
            ?.filter { it.elementType == VALUE_ARGUMENT }
            ?.map { it.text }
            ?.toSet()
            .orEmpty()

    private fun ASTNode.createSuppressAnnotation(
        suppressType: SuppressAnnotationType,
        suppressions: Set<String>,
    ) {
        val targetNode =
            if (elementType == ANNOTATION_ENTRY) {
                treeParent
            } else {
                this
            }

        if (targetNode.psi is KtFile || targetNode.psi is KtFileAnnotationList) {
            val fileAnnotation =
                targetNode
                    .psi
                    .createFileAnnotation(
                        suppressType,
                        suppressions
                            .sorted()
                            .joinToString(),
                    ).node
            if (targetNode.elementType == FILE_ANNOTATION_LIST) {
                this.replaceWith(fileAnnotation.firstChildNode)
            } else {
                this.createFileAnnotationList(fileAnnotation)
            }
        } else {
            val modifierListWithAnnotation =
                targetNode
                    .psi
                    .createModifierListWithAnnotationEntry(
                        suppressType,
                        suppressions
                            .sorted()
                            .joinToString(),
                    )
            when (elementType) {
                ANNOTATION_ENTRY ->
                    this.replaceWith(
                        modifierListWithAnnotation
                            .getChildOfType<KtAnnotationEntry>()!!
                            .node,
                    )
                CLASS, FUN, PROPERTY, PROPERTY_ACCESSOR -> {
                    this.addChild(PsiWhiteSpaceImpl(indent()), this.firstChildNode)
                    this.addChild(modifierListWithAnnotation.node, this.firstChildNode)
                }
                else -> {
                    treeParent.addChild(
                        modifierListWithAnnotation
                            .getChildOfType<KtAnnotationEntry>()!!
                            .node,
                        this,
                    )
                    treeParent.addChild(PsiWhiteSpaceImpl(indent()), this)
                }
            }
        }
    }

    private fun PsiElement.createFileAnnotation(
        suppressType: SuppressAnnotationType,
        sortedSuppressionsString: String,
    ): PsiElement =
        "@file:${suppressType.annotationName}($sortedSuppressionsString)"
            .let { annotation ->
                PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(KotlinLanguage.INSTANCE, annotation)
                    ?.firstChild
                    ?: throw IllegalStateException("Can not create annotation '$annotation'")
            }

    private fun ASTNode.createFileAnnotationList(annotation: ASTNode) {
        require(isRoot()) { "File annotation list can only be created for root node" }
        // Should always be inserted into the first (root) code child regardless in which root node the ktlint directive
        // was actually found
        findChildByType(PACKAGE_DIRECTIVE)
            ?.let { packageDirective ->
                packageDirective
                    .treeParent
                    .addChild(annotation, packageDirective)
                packageDirective
                    .treeParent
                    .addChild(PsiWhiteSpaceImpl("\n" + indent()), packageDirective)
            }
    }

    private fun PsiElement.createModifierListWithAnnotationEntry(
        suppressType: SuppressAnnotationType,
        sortedSuppressionsString: String,
    ): PsiElement =
        "@${suppressType.annotationName}($sortedSuppressionsString)"
            .let { annotation ->
                PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(
                        KotlinLanguage.INSTANCE,
                        // Create the annotation for a dummy declaration as the entire code block should be valid Kotlin code
                        """
                        $annotation
                        fun foo() {}
                        """.trimIndent(),
                    ).getChildOfType<KtScript>()
                    ?.getChildOfType<KtBlockExpression>()
                    ?.getChildOfType<KtNamedFunction>()
                    ?.getChildOfType<KtDeclarationModifierList>()
                    ?: throw IllegalStateException("Can not create annotation '$annotation'")
            }

    private fun KtLintDirective.removeKtlintEnableDirective(
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(offset, "Directive 'ktlint-enable' is obsolete after migrating to suppress annotations", true)
        if (autoCorrect) {
            node.removePrecedingWhitespace()
            node.remove()
        }
    }

    private fun ASTNode.replaceWith(node: ASTNode) {
        treeParent.addChild(node, this)
        this.remove()
    }

    private enum class SuppressAnnotationType(val annotationName: String) {
        SUPPRESS("Suppress"),
        SUPPRESS_WARNINGS("SuppressWarnings"),
        ;

        companion object {
            fun findByIdOrNull(id: String): SuppressAnnotationType? =
                SuppressAnnotationType
                    .values()
                    .firstOrNull { it.annotationName == id }
        }
    }
}

private data class KtLintDirective(
    val node: ASTNode,
    val ktlintDirectiveType: KtlintDirectiveType,
    val ktlintDirectives: String,
    val suppressionIdChanges: Set<SuppressionIdChange>,
) {
    val offset = node.startOffset + node.text.indexOf(ktlintDirectiveType.id)

    fun hasNoMatchingKtlintEnableDirective(ruleIdValidator: (String) -> Boolean): Boolean {
        require(ktlintDirectiveType == KTLINT_DISABLE && node.elementType == BLOCK_COMMENT)

        return if (shouldBeConvertedToFileAnnotation()) {
            false
        } else {
            findMatchingKtlintEnableDirective(ruleIdValidator) == null
        }
    }

    private fun findMatchingKtlintEnableDirective(ruleIdValidator: (String) -> Boolean) =
        node
            .applyIf(node.isSuppressibleDeclaration()) { node.treeParent }
            .siblings()
            .firstOrNull {
                it
                    .ktlintDirectiveOrNull(ruleIdValidator)
                    ?.takeIf { it.ktlintDirectiveType == KTLINT_ENABLE }
                    ?.ktlintDirectives == ktlintDirectives
            }

    fun shouldBeConvertedToFileAnnotation() =
        node.isTopLevel() ||
            (node.elementType == BLOCK_COMMENT && node.isSuppressibleDeclaration() && node.treeParent.isTopLevel())

    private fun ASTNode.isSuppressibleDeclaration() =
        when (treeParent.elementType) {
            CLASS, FUN, PROPERTY -> true
            else -> false
        }

    private fun ASTNode.isTopLevel() =
        FILE ==
            this
                .treeParent
                .elementType

    fun shouldBePromotedToParentDeclaration(ruleIdValidator: (String) -> Boolean): Boolean {
        require(ktlintDirectiveType == KTLINT_DISABLE && node.elementType == BLOCK_COMMENT)

        return if (shouldBeConvertedToFileAnnotation()) {
            false
        } else {
            node
                .takeIf { it.isSuppressibleDeclaration() }
                ?.let { findMatchingKtlintEnableDirective(ruleIdValidator) }
                ?.let { matchingKtlintEnabledDirective ->
                    // In case the node is part of a suppressible declaration and the next sibling matches the enable directive then the
                    // block directive should be match with this declaration only and not be moved to the parent.
                    matchingKtlintEnabledDirective !=
                        node
                            .treeParent
                            .nextSibling { !it.isWhiteSpace() }
                }
                ?: false
        }
    }


    enum class KtlintDirectiveType(val id: String) {
        KTLINT_DISABLE("ktlint-disable"),
        KTLINT_ENABLE("ktlint-enable"),
    }

    sealed class SuppressionIdChange {
        class ValidSuppressionId(val suppressionId: String) : SuppressionIdChange()

        class InvalidSuppressionId(
            val originalRuleId: String,
            val offsetOriginalRuleId: Int,
        ) : SuppressionIdChange()
    }
}

private fun ASTNode.ktlintDirectiveOrNull(ruleIdValidator: (String) -> Boolean): KtLintDirective? {
    val ktlintDirectiveString =
        when (elementType) {
            EOL_COMMENT ->
                text
                    .removePrefix("//")
                    .trim()

            BLOCK_COMMENT ->
                text
                    .removePrefix("/*")
                    .removeSuffix("*/")
                    .trim()

            else ->
                return null
        }
    val ktlintDirectiveType =
        ktlintDirectiveString.toKtlintDirectiveTypeOrNull()
            ?: return null
    val ruleIds = ktlintDirectiveString.removePrefix(ktlintDirectiveType.id)
    val suppressionIdChanges = ruleIds.toSuppressionIdChanges(ruleIdValidator)

    return KtLintDirective(this, ktlintDirectiveType, ruleIds, suppressionIdChanges)
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
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { originalRuleId ->
            val prefixedRuleId = prefixWithRuleSetIdWhenMissing(originalRuleId)
            if (ruleIdValidator(prefixedRuleId)) {
                ValidSuppressionId(
                    prefixedRuleId
                        .prefixIfNot(KTLINT_SUPPRESSION_ID_PREFIX)
                        .surroundWith(DOUBLE_QUOTE),
                )
            } else {
                InvalidSuppressionId(
                    originalRuleId,
                    this.indexOf(originalRuleId),
                )
            }
        }.toSet()
        .ifEmpty { setOf(ValidSuppressionId("\"ktlint\"")) }

private fun prefixWithRuleSetIdWhenMissing(ruleIdString: String) =
    RuleId
        .prefixWithStandardRuleSetIdWhenMissing(
            // The experimental ruleset was removed in Ktlint 0.49. References to that ruleset however may still exist. Also, not all
            // user seem to understand that it is no longer a separate ruleset.
            ruleIdString.removePrefix("experimental:"),
        )

private fun String.surroundWith(string: String) =
    removeSurrounding(string)
        .prefixIfNot(string)
        .plus(string)

public val KTLINT_SUPPRESSION_RULE_ID: RuleId = KtlintSuppressionRule(emptyList()).ruleId
